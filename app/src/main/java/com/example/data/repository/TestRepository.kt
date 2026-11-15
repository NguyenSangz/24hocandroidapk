package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.TestAnswerEntity
import com.example.data.local.entity.TestEntity
import com.example.domain.model.GradeResult
import com.example.domain.model.Question
import com.example.domain.model.TestAnswerDetail
import com.example.domain.model.UserAnswer
import com.example.data.sync.SyncOutboxHelper
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class TestRepository(
    private val database: AppDatabase,
    private val wrongQuestionRepository: WrongQuestionRepository? = null
) {
    private val testDao = database.testDao()
    private val attemptDao = database.attemptDao()
    private val questionDao = database.questionDao()

    /**
     * Lưu trạng thái nháp của bài kiểm tra (tự lưu định kỳ hoặc khi người dùng tạm thoát)
     */
    suspend fun saveDraft(
        testEntity: TestEntity,
        answerDetails: List<TestAnswerDetail>
    ) {
        val draftEntity = testEntity.copy(isDraft = true)
        testDao.insertTest(draftEntity)

        val answerEntities = answerDetails.mapIndexed { index, detail ->
            TestAnswerEntity(
                testId = testEntity.id,
                questionId = detail.question.id,
                questionIndex = index,
                userAnswerJson = serializeUserAnswer(detail.userAnswer),
                isAnswered = detail.isAnswered,
                isFlagged = detail.isFlagged,
                score = detail.gradeResult?.score ?: 0.0f,
                isCorrect = detail.gradeResult?.isCorrect ?: false,
                gradeStatus = detail.gradeResult?.status?.name ?: "INCORRECT",
                selfEvaluated = detail.isSelfEvaluated
            )
        }
        testDao.deleteAnswersByTestId(testEntity.id)
        testDao.insertAnswers(answerEntities)
        SyncOutboxHelper.enqueueTest(database, draftEntity, answerEntities)
    }

    /**
     * Tìm bài thi đang làm dở của học sinh
     */
    suspend fun getActiveDraft(grade: Int, subjectId: String = "all"): TestEntity? {
        return testDao.getActiveDraftTest(grade, subjectId)
    }

    /**
     * Lấy danh sách câu trả lời đã lưu nháp
     */
    suspend fun getDraftAnswers(testId: String): List<TestAnswerEntity> {
        return testDao.getAnswersForTest(testId)
    }

    /**
     * Nộp bài kiểm tra:
     * 1. Cập nhật testEntity thành isDraft = false, lưu điểm, thời gian
     * 2. Lưu chi tiết câu trả lời vào test_answers
     * 3. Ghi vết toàn bộ câu hỏi vào bảng `attempts` (AttemptEntity)
     * 4. Ghi nhận các câu sai vào `wrong_questions` (F08 ngân hàng câu sai)
     */
    suspend fun submitTest(
        testEntity: TestEntity,
        answerDetails: List<TestAnswerDetail>
    ) {
        val completedEntity = testEntity.copy(isDraft = false)
        testDao.insertTest(completedEntity)

        val answerEntities = answerDetails.mapIndexed { index, detail ->
            TestAnswerEntity(
                testId = testEntity.id,
                questionId = detail.question.id,
                questionIndex = index,
                userAnswerJson = serializeUserAnswer(detail.userAnswer),
                isAnswered = detail.isAnswered,
                isFlagged = detail.isFlagged,
                score = detail.gradeResult?.score ?: 0.0f,
                isCorrect = detail.gradeResult?.isCorrect ?: false,
                gradeStatus = detail.gradeResult?.status?.name ?: "INCORRECT",
                selfEvaluated = detail.isSelfEvaluated
            )
        }
        testDao.deleteAnswersByTestId(testEntity.id)
        testDao.insertAnswers(answerEntities)

        // Ghi vết vào AttemptDao cho từng câu hỏi và WrongQuestionRepository cho câu sai
        answerDetails.forEach { detail ->
            val isCorrect = detail.gradeResult?.isCorrect ?: false
            val score = detail.gradeResult?.score ?: 0.0f
            val userAnswerStr = serializeUserAnswer(detail.userAnswer)

            val attempt = AttemptEntity(
                questionId = detail.question.id,
                isCorrect = isCorrect,
                score = score,
                timestamp = System.currentTimeMillis(),
                timeSpentSeconds = 0,
                userAnswer = userAnswerStr,
                usedHint = false // Kiểm tra không có gợi ý
            )
            attemptDao.insertAttempt(attempt)
            SyncOutboxHelper.enqueueAttempt(database, attempt)

            if (!isCorrect || score < 1.0f) {
                wrongQuestionRepository?.recordWrongAnswer(
                    questionId = detail.question.id,
                    lessonId = detail.question.lessonId,
                    wrongAnswer = userAnswerStr
                )
            } else {
                wrongQuestionRepository?.recordCorrectAnswer(detail.question.id)
            }
        }
        SyncOutboxHelper.enqueueTest(database, completedEntity, answerEntities)
    }

    /**
     * Cập nhật điểm và trạng thái tự đánh giá cho câu tự luận ngắn
     */
    suspend fun updateSelfEvaluation(
        testId: String,
        questionId: String,
        newScore: Float,
        isCorrect: Boolean,
        newOverallTestScore: Float,
        newCorrectCount: Int
    ) {
        val test = testDao.getTestById(testId) ?: return
        val answers = testDao.getAnswersForTest(testId)
        val targetAnswer = answers.find { it.questionId == questionId } ?: return

        testDao.updateAnswer(
            targetAnswer.copy(
                score = newScore,
                isCorrect = isCorrect,
                gradeStatus = if (isCorrect) "CORRECT" else "INCORRECT",
                selfEvaluated = true
            )
        )

        val updatedTest = test.copy(
            score = newOverallTestScore,
            correctCount = newCorrectCount
        )
        testDao.updateTest(updatedTest)
        val refreshedAnswers = testDao.getAnswersForTest(testId)
        SyncOutboxHelper.enqueueTest(database, updatedTest, refreshedAnswers)
    }

    /**
     * Lấy bài kiểm tra theo id
     */
    suspend fun getTestById(id: String): TestEntity? {
        return testDao.getTestById(id)
    }

    /**
     * Lấy danh sách câu trả lời của bài kiểm tra
     */
    suspend fun getAnswersForTest(testId: String): List<TestAnswerEntity> {
        return testDao.getAnswersForTest(testId)
    }

    /**
     * Lấy lịch sử tất cả bài kiểm tra đã hoàn thành
     */
    fun getAllCompletedTests(): Flow<List<TestEntity>> {
        return testDao.getAllCompletedTests()
    }

    /**
     * Lấy lịch sử bài kiểm tra theo lớp và môn
     */
    fun getCompletedTestsByGradeAndSubject(grade: Int, subjectId: String = "all"): Flow<List<TestEntity>> {
        return testDao.getCompletedTestsByGradeAndSubject(grade, subjectId)
    }

    /**
     * Xóa bài kiểm tra
     */
    suspend fun deleteTest(id: String) {
        testDao.deleteTestById(id)
        testDao.deleteAnswersByTestId(id)
        SyncOutboxHelper.enqueueTestDeletion(database, id)
    }

    // Helper serialize/deserialize chuỗi UserAnswer
    private fun serializeUserAnswer(answer: UserAnswer?): String {
        if (answer == null) return ""
        return try {
            when (answer) {
                is UserAnswer.MultipleChoice -> answer.selectedOption
                is UserAnswer.TrueFalse -> answer.selectedValue.toString()
                is UserAnswer.FillBlank -> answer.filledText
                is UserAnswer.Matching -> {
                    val array = JSONArray()
                    answer.userPairs.forEach { pair ->
                        val obj = JSONObject()
                        obj.put("l", pair.left)
                        obj.put("r", pair.right)
                        array.put(obj)
                    }
                    array.toString()
                }
                is UserAnswer.Ordering -> {
                    val array = JSONArray()
                    answer.userOrder.forEach { array.put(it) }
                    array.toString()
                }
                is UserAnswer.ShortAnswer -> answer.submittedText
            }
        } catch (_: Exception) {
            ""
        }
    }
}
