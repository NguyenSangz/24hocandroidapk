package com.example

import com.example.domain.model.Grade
import com.example.domain.model.QuestionType
import com.example.domain.model.StandardCurriculumSubjects
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Script kiểm tra cấu trúc và tính toàn vẹn của file dữ liệu JSON (F01).
 * Dùng Moshi (thư viện thực tế, không bị lỗi Method Not Mocked của android.jar).
 */
class CurriculumJsonValidatorTest {

    private lateinit var rootMap: Map<String, Any>

    @Before
    @Suppress("UNCHECKED_CAST")
    fun setUp() {
        val assetFile = listOf(
            File("src/main/assets/sample_curriculum.json"),
            File("app/src/main/assets/sample_curriculum.json"),
            File("../app/src/main/assets/sample_curriculum.json")
        ).firstOrNull { it.exists() }

        assertNotNull("Không tìm thấy file sample_curriculum.json trong thư mục assets", assetFile)
        val jsonString = assetFile!!.readText()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(Map::class.java)
        val parsed = adapter.fromJson(jsonString) as? Map<String, Any>
        assertNotNull("JSON parse thất bại", parsed)
        rootMap = parsed!!
    }

    @Test
    fun testRootContainsAllRequiredSections() {
        assertTrue("Thiếu mục 'subjects'", rootMap.containsKey("subjects"))
        assertTrue("Thiếu mục 'grades'", rootMap.containsKey("grades"))
        assertTrue("Thiếu mục 'questions'", rootMap.containsKey("questions"))
        assertTrue("Thiếu mục 'flashcards'", rootMap.containsKey("flashcards"))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testAll15SubjectsAreConfiguredInCurriculum() {
        val subjectsList = rootMap["subjects"] as? List<Map<String, Any>>
        assertNotNull("subjects phải là một danh sách", subjectsList)
        assertTrue("Số môn học phải ít nhất là 15", subjectsList!!.size >= 15)

        val subjectIds = mutableSetOf<String>()
        subjectsList.forEach { sub ->
            val id = sub["id"] as? String ?: ""
            subjectIds.add(id)

            assertTrue("Tên môn học không được rỗng", (sub["name"] as? String)?.isNotBlank() == true)
            assertTrue("Icon môn học không được rỗng", (sub["icon"] as? String)?.isNotBlank() == true)
            assertTrue("Mã màu môn học hợp lệ", (sub["color"] as? String)?.startsWith("#") == true)
            assertTrue("Danh mục môn học không được rỗng", (sub["category"] as? String)?.isNotBlank() == true)
        }

        StandardCurriculumSubjects.ALL_15_SUBJECTS.forEach { expectedSub ->
            assertTrue(
                "Thiếu môn học bắt buộc: ${expectedSub.name} (${expectedSub.id})",
                subjectIds.contains(expectedSub.id)
            )
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testGradesStructureCoversClasses6To9() {
        val gradesList = rootMap["grades"] as? List<Map<String, Any>>
        assertNotNull("grades phải là một danh sách", gradesList)

        val gradesFound = mutableSetOf<Int>()
        gradesList!!.forEach { gradeObj ->
            val g = (gradeObj["grade"] as? Number)?.toInt() ?: 0
            gradesFound.add(g)

            val chapters = gradeObj["chapters"] as? List<Map<String, Any>>
            assertNotNull("Chapters của lớp $g phải là danh sách", chapters)
            assertTrue("Lớp $g phải có ít nhất 1 chương học", chapters!!.isNotEmpty())

            chapters.forEach { chap ->
                assertFalse("ID chương không được rỗng", (chap["id"] as? String).isNullOrBlank())
                assertFalse("Tiêu đề chương không được rỗng", (chap["title"] as? String).isNullOrBlank())

                val lessons = chap["lessons"] as? List<Map<String, Any>>
                assertNotNull("Lessons của chương ${chap["id"]} phải là danh sách", lessons)
                assertTrue("Chương ${chap["id"]} phải có bài học", lessons!!.isNotEmpty())

                lessons.forEach { lesson ->
                    assertFalse("ID bài học không được rỗng", (lesson["id"] as? String).isNullOrBlank())
                    assertFalse("Tiêu đề bài học không được rỗng", (lesson["title"] as? String).isNullOrBlank())
                    assertFalse("Tóm tắt lý thuyết không được rỗng", (lesson["summary"] as? String).isNullOrBlank())
                }
            }
        }

        Grade.entries.forEach { expectedGrade ->
            assertTrue(
                "Thiếu nội dung khối lớp: ${expectedGrade.displayName}",
                gradesFound.contains(expectedGrade.value)
            )
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testQuestionsAdhereToSchemaAndHaveExplanations() {
        val questionsList = rootMap["questions"] as? List<Map<String, Any>>
        assertNotNull("questions phải là một danh sách", questionsList)
        assertTrue("Ngân hàng câu hỏi phải có ít nhất 20 câu", questionsList!!.size >= 20)

        questionsList.forEach { q ->
            val id = q["id"] as? String ?: ""
            val typeStr = q["type"] as? String ?: ""
            val content = q["content"] as? String ?: ""
            val correctAnswer = q["correctAnswer"] as? String ?: ""
            val explanation = q["explanation"] as? String ?: ""

            assertFalse("ID câu hỏi $id không được rỗng", id.isBlank())
            assertFalse("Nội dung câu hỏi $id không được rỗng", content.isBlank())
            assertFalse("Đáp án đúng câu hỏi $id không được rỗng", correctAnswer.isBlank())
            assertFalse("Lời giải chi tiết câu hỏi $id không được rỗng", explanation.isBlank())

            val parsedType = QuestionType.fromCode(typeStr)
            assertNotNull("Loại câu hỏi $typeStr không hợp lệ", parsedType)
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testFlashcardsHaveFrontAndBackContent() {
        val flashcardsList = rootMap["flashcards"] as? List<Map<String, Any>>
        assertNotNull("flashcards phải là một danh sách", flashcardsList)
        assertTrue("Phải có ít nhất 10 flashcard", flashcardsList!!.size >= 10)

        flashcardsList.forEach { fc ->
            assertFalse("Mặt trước flashcard không được rỗng", (fc["front"] as? String).isNullOrBlank())
            assertFalse("Mặt sau flashcard không được rỗng", (fc["back"] as? String).isNullOrBlank())
            assertFalse("Chủ đề flashcard không được rỗng", (fc["category"] as? String).isNullOrBlank())
        }
    }
}
