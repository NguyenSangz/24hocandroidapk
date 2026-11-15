package com.example.domain.evaluator

data class ChapterAccuracyItem(
    val chapterId: String,
    val chapterTitle: String,
    val subjectId: String,
    val totalAnswered: Int,
    val totalCorrect: Int,
    val accuracyPercent: Float
)
