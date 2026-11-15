package com.example.domain.mapper

import android.util.Log
import com.example.data.local.entity.QuestionEntity
import com.example.domain.model.Difficulty
import com.example.domain.model.MatchingPair
import com.example.domain.model.Question
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mapper chuyển đổi từ QuestionEntity (Room) sang sealed class Question (Domain)
 * Xử lý dữ liệu JSON an toàn, bắt lỗi rõ ràng, tuyệt đối không làm crash app.
 */
object QuestionMapper {

    private const val TAG = "QuestionMapper"

    fun toDomain(entity: QuestionEntity): Question {
        val diff = Difficulty.fromLevel(entity.difficulty)
        val upperType = entity.type.trim().uppercase()

        return try {
            when (upperType) {
                "MULTIPLE_CHOICE" -> parseMultipleChoice(entity, diff)
                "TRUE_FALSE" -> parseTrueFalse(entity, diff)
                "FILL_BLANK" -> parseFillBlank(entity, diff)
                "MATCHING", "MATCH" -> parseMatching(entity, diff)
                "SORTING", "ORDERING", "ORDER" -> parseOrdering(entity, diff)
                "SHORT_ESSAY", "SHORT_ANSWER" -> parseShortAnswer(entity, diff)
                else -> {
                    Log.w(TAG, "Loại câu hỏi lạ '${entity.type}' ở câu ${entity.id}, fallback về MultipleChoice")
                    parseMultipleChoice(entity, diff)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phân tích JSON ở câu ${entity.id} (loại ${entity.type}): ${e.message}", e)
            // Fallback an toàn: Trả về MultipleChoice cơ bản chứa nội dung thô để học sinh vẫn xem được
            Question.MultipleChoice(
                id = entity.id,
                lessonId = entity.lessonId,
                content = entity.content,
                options = listOf(entity.correctAnswer),
                correctAnswer = entity.correctAnswer,
                hint = entity.hint,
                explanation = "${entity.explanation} (Lưu ý: Đã tải dữ liệu dự phòng do định dạng)",
                difficulty = diff
            )
        }
    }

    private fun parseMultipleChoice(entity: QuestionEntity, diff: Difficulty): Question.MultipleChoice {
        val optionsList = parseStringList(entity.optionsJson)
        val finalOptions = if (optionsList.isNotEmpty()) {
            optionsList
        } else {
            listOf(entity.correctAnswer)
        }

        return Question.MultipleChoice(
            id = entity.id,
            lessonId = entity.lessonId,
            content = entity.content,
            options = finalOptions,
            correctAnswer = entity.correctAnswer,
            hint = entity.hint,
            explanation = entity.explanation,
            difficulty = diff
        )
    }

    private fun parseTrueFalse(entity: QuestionEntity, diff: Difficulty): Question.TrueFalse {
        val ansStr = entity.correctAnswer.trim().lowercase()
        val boolAns = (ansStr == "true" || ansStr == "đúng" || ansStr == "dung" || ansStr == "1")

        return Question.TrueFalse(
            id = entity.id,
            lessonId = entity.lessonId,
            content = entity.content,
            correctAnswer = boolAns,
            hint = entity.hint,
            explanation = entity.explanation,
            difficulty = diff
        )
    }

    private fun parseFillBlank(entity: QuestionEntity, diff: Difficulty): Question.FillBlank {
        // Đáp án có thể nằm trong optionsJson (nhiều đáp án chấp nhận) hoặc chuỗi correctAnswer (phân tách bởi | hoặc ;)
        val accepted = mutableListOf<String>()

        // 1. Kiểm tra optionsJson nếu là mảng
        if (entity.optionsJson.isNotBlank() && entity.optionsJson != "[]") {
            try {
                val array = JSONArray(entity.optionsJson)
                for (i in 0 until array.length()) {
                    val item = array.optString(i, "").trim()
                    if (item.isNotEmpty() && !accepted.contains(item)) {
                        accepted.add(item)
                    }
                }
            } catch (_: Exception) {
                // Không phải JSON array
            }
        }

        // 2. Tách từ correctAnswer nếu dùng ký tự phân tách '|' hoặc ';'
        if (entity.correctAnswer.isNotBlank()) {
            val parts = entity.correctAnswer.split('|', ';')
            for (p in parts) {
                val clean = p.trim()
                if (clean.isNotEmpty() && !accepted.contains(clean)) {
                    accepted.add(clean)
                }
            }
        }

        if (accepted.isEmpty() && entity.correctAnswer.isNotBlank()) {
            accepted.add(entity.correctAnswer.trim())
        }

        return Question.FillBlank(
            id = entity.id,
            lessonId = entity.lessonId,
            content = entity.content,
            acceptedAnswers = accepted,
            hint = entity.hint,
            explanation = entity.explanation,
            difficulty = diff
        )
    }

    private fun parseMatching(entity: QuestionEntity, diff: Difficulty): Question.Matching {
        val pairs = mutableListOf<MatchingPair>()

        // Thử parse optionsJson dạng [{"left": "...", "right": "..."}, ...] hoặc các chuỗi phân tách bằng "::"
        if (entity.optionsJson.isNotBlank() && entity.optionsJson != "[]") {
            try {
                val array = JSONArray(entity.optionsJson)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        val left = obj.optString("left", "")
                        val right = obj.optString("right", "")
                        if (left.isNotBlank() && right.isNotBlank()) {
                            pairs.add(MatchingPair(left, right))
                        }
                    } else {
                        // Trường hợp mảng chuỗi kiểu "A::B" hoặc "A - B"
                        val str = array.optString(i, "")
                        val split = str.split("::", " - ", " -> ")
                        if (split.size >= 2) {
                            pairs.add(MatchingPair(split[0].trim(), split[1].trim()))
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Nếu optionsJson chưa có hoặc rỗng, thử parse từ correctAnswer dạng "A-1; B-2" hoặc JSON object
        if (pairs.isEmpty() && entity.correctAnswer.isNotBlank()) {
            try {
                val obj = JSONObject(entity.correctAnswer)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.getString(k)
                    pairs.add(MatchingPair(k, v))
                }
            } catch (_: Exception) {
                // Tách dạng chuỗi "A::B; C::D"
                val items = entity.correctAnswer.split(';', '\n')
                for (item in items) {
                    val split = item.split("::", " - ", " -> ", ":")
                    if (split.size >= 2) {
                        pairs.add(MatchingPair(split[0].trim(), split[1].trim()))
                    }
                }
            }
        }

        return Question.Matching(
            id = entity.id,
            lessonId = entity.lessonId,
            content = entity.content,
            pairs = pairs,
            hint = entity.hint,
            explanation = entity.explanation,
            difficulty = diff
        )
    }

    private fun parseOrdering(entity: QuestionEntity, diff: Difficulty): Question.Ordering {
        val orderList = mutableListOf<String>()

        // 1. Thử parse từ optionsJson
        if (entity.optionsJson.isNotBlank() && entity.optionsJson != "[]") {
            try {
                val array = JSONArray(entity.optionsJson)
                for (i in 0 until array.length()) {
                    val item = array.optString(i, "").trim()
                    if (item.isNotEmpty()) {
                        orderList.add(item)
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Nếu rỗng, lấy từ correctAnswer tách bằng "->", ";", ","
        if (orderList.isEmpty() && entity.correctAnswer.isNotBlank()) {
            val parts = if (entity.correctAnswer.contains("->")) {
                entity.correctAnswer.split("->")
            } else if (entity.correctAnswer.contains(";")) {
                entity.correctAnswer.split(";")
            } else {
                entity.correctAnswer.split(",")
            }

            for (p in parts) {
                val clean = p.trim()
                if (clean.isNotEmpty()) {
                    orderList.add(clean)
                }
            }
        }

        return Question.Ordering(
            id = entity.id,
            lessonId = entity.lessonId,
            content = entity.content,
            correctOrder = orderList,
            hint = entity.hint,
            explanation = entity.explanation,
            difficulty = diff
        )
    }

    private fun parseShortAnswer(entity: QuestionEntity, diff: Difficulty): Question.ShortAnswer {
        val samples = mutableListOf<String>()

        if (entity.optionsJson.isNotBlank() && entity.optionsJson != "[]") {
            try {
                val array = JSONArray(entity.optionsJson)
                for (i in 0 until array.length()) {
                    val item = array.optString(i, "").trim()
                    if (item.isNotEmpty()) {
                        samples.add(item)
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (samples.isEmpty() && entity.correctAnswer.isNotBlank()) {
            val parts = entity.correctAnswer.split('|', ';')
            for (p in parts) {
                val clean = p.trim()
                if (clean.isNotEmpty()) {
                    samples.add(clean)
                }
            }
        }

        return Question.ShortAnswer(
            id = entity.id,
            lessonId = entity.lessonId,
            content = entity.content,
            sampleAnswers = samples,
            evaluationCriteria = entity.hint,
            hint = entity.hint,
            explanation = entity.explanation,
            difficulty = diff
        )
    }

    private fun parseStringList(jsonStr: String): List<String> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.optString(i, ""))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
