package com.example

import com.example.domain.model.Grade
import com.example.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumLogicTest {

    @Test
    fun testGradeEnums() {
        val grades = Grade.entries
        assertEquals(4, grades.size)
        assertEquals(6, Grade.GRADE_6.value)
        assertEquals(7, Grade.GRADE_7.value)
        assertEquals(8, Grade.GRADE_8.value)
        assertEquals(9, Grade.GRADE_9.value)

        val fromVal = Grade.fromValue(7)
        assertEquals(Grade.GRADE_7, fromVal)
    }

    @Test
    fun testQuestionTypes() {
        val mc = QuestionType.fromCode("MULTIPLE_CHOICE")
        assertEquals(QuestionType.MULTIPLE_CHOICE, mc)

        val tf = QuestionType.fromCode("TRUE_FALSE")
        assertEquals(QuestionType.TRUE_FALSE, tf)

        val fib = QuestionType.fromCode("FILL_BLANK")
        assertEquals(QuestionType.FILL_BLANK, fib)
    }

    @Test
    fun testSpacedRepetitionIntervalCalculation() {
        // SM-2 / Leitner Box interval logic
        fun calculateNextInterval(currentBox: Int, remembered: Boolean): Int {
            return if (remembered) {
                when (currentBox) {
                    1 -> 2
                    2 -> 4
                    3 -> 7
                    4 -> 14
                    else -> 30
                }
            } else {
                1
            }
        }

        // When answered correctly, intervals increase exponentially
        assertEquals(2, calculateNextInterval(1, true))
        assertEquals(4, calculateNextInterval(2, true))
        assertEquals(7, calculateNextInterval(3, true))
        assertEquals(14, calculateNextInterval(4, true))
        assertEquals(30, calculateNextInterval(5, true))

        // When forgotten, drops back to 1 day review
        assertEquals(1, calculateNextInterval(4, false))
    }

    @Test
    fun testScoreCalculation() {
        fun computeScore10Scale(correctCount: Int, totalCount: Int): Float {
            if (totalCount == 0) return 0f
            return ((correctCount.toFloat() / totalCount.toFloat()) * 10f * 10).toInt() / 10f
        }

        assertEquals(10.0f, computeScore10Scale(20, 20), 0.01f)
        assertEquals(8.0f, computeScore10Scale(16, 20), 0.01f)
        assertEquals(5.0f, computeScore10Scale(10, 20), 0.01f)
        assertEquals(0.0f, computeScore10Scale(0, 20), 0.01f)
    }
}
