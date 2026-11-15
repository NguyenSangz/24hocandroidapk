package com.example.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ContentVersionEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * F17a - Seeder nạp dữ liệu chương trình học từ các "gói nội dung" (content package).
 *
 * Kiến trúc mới (thay cho logic cũ "nạp 1 file nếu chưa có 15 môn"):
 * 1. Gói gốc `sample_curriculum.json` (nằm ngay gốc assets) - định nghĩa 15 môn
 *    và nội dung Toán/Tiếng Anh hiện có.
 * 2. Mọi file JSON trong `assets/curriculum/` là một gói nội dung bổ sung
 *    (ví dụ GDCD, KHTN, Lịch sử & Địa lí...) - có thể chỉ chứa chương/bài/câu hỏi
 *    cho các môn đã định nghĩa, KHÔNG bắt buộc khai báo lại môn.
 *
 * Mỗi gói có `packageId` + `contentVersion` (số nguyên tăng dần). Seeder so sánh
 * với bảng `content_versions`:
 * - Lần đầu hoặc version trong JSON lớn hơn -> nhập (upsert, REPLACE theo PK).
 * - Version bằng/nhỏ hơn -> bỏ qua, không nhập lại mỗi lần mở app.
 *
 * BẢO TOÀN DỮ LIỆU NGƯỜI DÙNG khi nhập lại gói đã có (REPLACE sẽ ghi đè):
 * - `SubjectEntity.isPinned`: ghim môn là cài đặt của người dùng -> giữ trạng thái cũ.
 * - `LessonEntity.isCompleted`: tiến độ "đã học" của bài -> giữ trạng thái cũ.
 * - `FlashcardEntity` (box, intervalDays, nextReviewTimestamp, easeFactor, repetitions):
 *   lịch ôn SM-2 của thẻ -> giữ trạng thái cũ.
 *
 * Hàm `importPackage(json, fallbackPackageId, fallbackVersion)` không đọc assets
 * nên unit test được trực tiếp với chuỗi JSON.
 */
class CurriculumDataSeeder(
    private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "CurriculumDataSeeder"

        /** Thư mục trong assets chứa các gói nội dung bổ sung. */
        private const val PACKAGE_DIR = "curriculum"

        /** Gói nội dung gốc (file cũ nằm ngay gốc assets, JSON không có packageId). */
        private const val LEGACY_ASSET_FILE = "sample_curriculum.json"
        private const val LEGACY_PACKAGE_ID = "sample_curriculum"
        private const val LEGACY_PACKAGE_VERSION = 1
    }

    /**
     * Nạp tất cả gói nội dung: gói gốc trước, sau đó các gói trong `assets/curriculum/`
     * theo thứ tự tên file. Gọi từ Splash (qua ContentLoader) nên chạy nền IO.
     */
    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        try {
            // 1. Gói gốc: 15 môn + nội dung Toán/Tiếng Anh
            importAssetFile(LEGACY_ASSET_FILE, LEGACY_PACKAGE_ID, LEGACY_PACKAGE_VERSION)

            // 2. Các gói nội dung bổ sung, sắp theo tên file để thứ tự nhập ổn định
            val packageFiles = context.assets.list(PACKAGE_DIR)
                ?.filter { it.endsWith(".json") }
                ?.sorted()
                ?: emptyList()
            for (fileName in packageFiles) {
                importAssetFile(
                    assetPath = "$PACKAGE_DIR/$fileName",
                    fallbackPackageId = fileName.removeSuffix(".json"),
                    fallbackVersion = 1
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi nạp dữ liệu bài học: ${e.message}", e)
        }
    }

    /** Đọc một file asset rồi gọi [importPackage]. Lỗi 1 gói không chặn các gói khác. */
    private suspend fun importAssetFile(assetPath: String, fallbackPackageId: String, fallbackVersion: Int) {
        try {
            val jsonString = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            importPackage(jsonString, fallbackPackageId, fallbackVersion)
        } catch (e: Exception) {
            Log.e(TAG, "Không đọc được gói nội dung '$assetPath': ${e.message}", e)
        }
    }

    /**
     * Nhập một gói nội dung từ chuỗi JSON.
     * @return Result.success(số phần tử đã nhập) - 0 nếu gói đã nhập ở phiên bản bằng/nhỏ hơn
     *         hoặc JSON rỗng; Result.failure nếu JSON không đọc được.
     */
    suspend fun importPackage(
        jsonString: String,
        fallbackPackageId: String,
        fallbackVersion: Int = 1
    ): Result<Int> {
        return try {
            val root = JSONObject(jsonString)
            val packageId = root.optString("packageId", fallbackPackageId).ifBlank { fallbackPackageId }
            val contentVersion = root.optInt("contentVersion", fallbackVersion).coerceAtLeast(1)

            // Đã nhập phiên bản này (hoặc mới hơn) rồi -> bỏ qua, không chạm vào dữ liệu
            val imported = database.contentVersionDao().getVersion(packageId)
            if (imported != null && imported.version >= contentVersion) {
                Log.d(TAG, "Gói '$packageId' v$contentVersion đã nhập (đang lưu v${imported.version}) - bỏ qua.")
                return Result.success(0)
            }

            // --- 1. Phân tích JSON thành entity (có thể ném JSONException) ---
            val subjectsList = parseSubjects(root)
            val chaptersList = mutableListOf<ChapterEntity>()
            val lessonsList = mutableListOf<LessonEntity>()
            parseChaptersAndLessons(root, chaptersList, lessonsList)
            val questionsList = parseQuestions(root)
            val flashcardsList = parseFlashcards(root)

            val total = subjectsList.size + chaptersList.size + lessonsList.size +
                questionsList.size + flashcardsList.size
            if (total == 0) {
                Log.w(TAG, "Gói '$packageId' không có dữ liệu nào - không ghi nhận phiên bản.")
                return Result.success(0)
            }

            // --- 2. Nhập trong transaction, bảo toàn trạng thái người dùng ---
            database.withTransaction {
                if (subjectsList.isNotEmpty()) {
                    // Bảo toàn isPinned: ghim môn là lựa chọn của người dùng
                    val existingSubjects = database.subjectDao().getAllSubjectsSync().associateBy { it.id }
                    val mergedSubjects = subjectsList.map { incoming ->
                        val old = existingSubjects[incoming.id]
                        if (old != null) incoming.copy(isPinned = old.isPinned) else incoming
                    }
                    database.subjectDao().insertAll(mergedSubjects)
                }

                if (chaptersList.isNotEmpty()) database.chapterDao().insertAll(chaptersList)

                if (lessonsList.isNotEmpty()) {
                    // Bảo toàn isCompleted: tiến độ học bài của người dùng
                    val existingLessons = database.lessonDao().getAllLessonsSync().associateBy { it.id }
                    val mergedLessons = lessonsList.map { incoming ->
                        val old = existingLessons[incoming.id]
                        if (old != null) incoming.copy(isCompleted = old.isCompleted) else incoming
                    }
                    database.lessonDao().insertAll(mergedLessons)
                }

                if (questionsList.isNotEmpty()) database.questionDao().insertAll(questionsList)

                if (flashcardsList.isNotEmpty()) {
                    // Bảo toàn trạng thái SM-2 của thẻ (box/intervan/ease/hẹn ôn lại)
                    val existingCards = database.flashcardDao().getAllFlashcardsSync().associateBy { it.id }
                    val mergedCards = flashcardsList.map { incoming ->
                        val old = existingCards[incoming.id]
                        if (old != null) incoming.copy(
                            box = old.box,
                            intervalDays = old.intervalDays,
                            nextReviewTimestamp = old.nextReviewTimestamp,
                            easeFactor = old.easeFactor,
                            repetitions = old.repetitions
                        ) else incoming
                    }
                    database.flashcardDao().insertAll(mergedCards)
                }

                // Ghi nhận phiên bản SAU CÙNG, trong cùng transaction để không lặp nhập
                database.contentVersionDao().upsert(
                    ContentVersionEntity(
                        packageId = packageId,
                        version = contentVersion,
                        importedAtMillis = System.currentTimeMillis()
                    )
                )
            }

            Log.d(
                TAG,
                "Đã nhập gói '$packageId' v$contentVersion: ${subjectsList.size} môn, " +
                    "${chaptersList.size} chương, ${lessonsList.size} bài, " +
                    "${questionsList.size} câu hỏi, ${flashcardsList.size} flashcard."
            )
            Result.success(total)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi nhập gói nội dung '$fallbackPackageId': ${e.message}", e)
            Result.failure(e)
        }
    }

    // ===================== PHÂN TÍCH JSON =====================

    /** Parse danh sách môn (có thể trống với gói chỉ bổ sung nội dung). */
    private fun parseSubjects(root: JSONObject): List<SubjectEntity> {
        val result = mutableListOf<SubjectEntity>()
        if (!root.has("subjects")) return result
        val subjectsArray = root.getJSONArray("subjects")
        for (i in 0 until subjectsArray.length()) {
            val obj = subjectsArray.getJSONObject(i)
            result.add(
                SubjectEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    icon = obj.optString("icon", "menu_book"),
                    color = obj.optString("color", "#2563EB"),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", "Chung"),
                    isPinned = obj.optBoolean("isPinned", false)
                )
            )
        }
        return result
    }

    /** Parse chương + bài học (lồng trong mảng `grades`). */
    private fun parseChaptersAndLessons(
        root: JSONObject,
        chaptersList: MutableList<ChapterEntity>,
        lessonsList: MutableList<LessonEntity>
    ) {
        if (!root.has("grades")) return
        val gradesArray = root.getJSONArray("grades")
        for (g in 0 until gradesArray.length()) {
            val gradeObj = gradesArray.getJSONObject(g)
            val gradeNum = gradeObj.getInt("grade")
            val chaptersArray = gradeObj.getJSONArray("chapters")

            for (c in 0 until chaptersArray.length()) {
                val chapObj = chaptersArray.getJSONObject(c)
                val chapId = chapObj.getString("id")
                val subjectId = chapObj.getString("subjectId")
                val order = chapObj.getInt("order")
                val title = chapObj.getString("title")
                val description = chapObj.optString("description", "")

                chaptersList.add(
                    ChapterEntity(
                        id = chapId,
                        subjectId = subjectId,
                        grade = gradeNum,
                        order = order,
                        title = title,
                        description = description,
                        series = chapObj.optString("series", "kntt"),
                        phanMon = chapObj.optString("phanMon", "")
                    )
                )

                val lessonsArray = chapObj.getJSONArray("lessons")
                for (l in 0 until lessonsArray.length()) {
                    val lessonObj = lessonsArray.getJSONObject(l)
                    lessonsList.add(
                        LessonEntity(
                            id = lessonObj.getString("id"),
                            chapterId = chapId,
                            order = lessonObj.getInt("order"),
                            title = lessonObj.getString("title"),
                            summary = lessonObj.getString("summary"),
                            examples = lessonObj.optString("examples", ""),
                            tips = lessonObj.optString("tips", ""),
                            formulasLatex = lessonObj.optString("formulasLatex", ""),
                            tablesJson = lessonObj.optString("tablesJson", ""),
                            isCompleted = false,
                            series = lessonObj.optString("series", "kntt"),
                            topicId = lessonObj.optString("topicId", ""),
                            phanMon = lessonObj.optString("phanMon", ""),
                            tier = lessonObj.optString("tier", "tier_a"),
                            objectives = lessonObj.optString("objectives", "")
                        )
                    )
                }
            }
        }
    }

    /** Parse câu hỏi. Mọi loại câu hỏi đều cần có mảng `options` (FILL_BLANK có thể rỗng). */
    private fun parseQuestions(root: JSONObject): List<QuestionEntity> {
        val result = mutableListOf<QuestionEntity>()
        if (!root.has("questions")) return result
        val questionsArray = root.getJSONArray("questions")
        for (q in 0 until questionsArray.length()) {
            val qObj = questionsArray.getJSONObject(q)
            val optionsArray: JSONArray = qObj.getJSONArray("options")

            result.add(
                QuestionEntity(
                    id = qObj.getString("id"),
                    lessonId = qObj.getString("lessonId"),
                    type = qObj.getString("type"),
                    content = qObj.getString("content"),
                    optionsJson = optionsArray.toString(),
                    correctAnswer = qObj.getString("correctAnswer"),
                    hint = qObj.optString("hint", ""),
                    explanation = qObj.optString("explanation", ""),
                    difficulty = qObj.optInt("difficulty", 1),
                    series = qObj.optString("series", "kntt"),
                    phanMon = qObj.optString("phanMon", ""),
                    topicId = qObj.optString("topicId", "")
                )
            )
        }
        return result
    }

    /** Parse flashcard (thẻ mới khởi tạo ở Box 1, trạng thái SM-2 được bảo toàn khi nhập lại). */
    private fun parseFlashcards(root: JSONObject): List<FlashcardEntity> {
        val result = mutableListOf<FlashcardEntity>()
        if (!root.has("flashcards")) return result
        val flashcardsArray = root.getJSONArray("flashcards")
        for (f in 0 until flashcardsArray.length()) {
            val fObj = flashcardsArray.getJSONObject(f)
            result.add(
                FlashcardEntity(
                    id = fObj.getString("id"),
                    deckId = fObj.optString("deckId", "deck_default"),
                    grade = fObj.getInt("grade"),
                    subjectId = fObj.getString("subjectId"),
                    front = fObj.getString("front"),
                    back = fObj.getString("back"),
                    category = fObj.optString("category", "Chung"),
                    series = fObj.optString("series", "kntt"),
                    word = if (fObj.has("word")) fObj.getString("word") else null,
                    partOfSpeech = if (fObj.has("partOfSpeech")) fObj.getString("partOfSpeech") else null,
                    phonetic = if (fObj.has("phonetic")) fObj.getString("phonetic") else null,
                    meaning = if (fObj.has("meaning")) fObj.getString("meaning") else null,
                    exampleSentence = if (fObj.has("exampleSentence")) fObj.getString("exampleSentence") else null,
                    box = 1,
                    intervalDays = 1,
                    nextReviewTimestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    repetitions = 0
                )
            )
        }
        return result
    }
}
