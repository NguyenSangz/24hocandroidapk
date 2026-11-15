package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Bảng chỉ mục FTS4 (Full-Text Search) trong SQLite Room (F13):
 * - FTS4 tương thích 100% trên toàn bộ các phiên bản Android (minSdk 24).
 * - Sử dụng notIndexed cho các trường siêu dữ liệu (metadata) để tối ưu kích thước bảng chỉ mục
 *   và tăng tốc độ tra cứu văn bản.
 * - Lưu trữ song song cả văn bản có dấu (NFC) và không dấu (đã loại bỏ dấu thanh, đ -> d)
 *   cho cả tiêu đề và nội dung.
 */
@Entity(tableName = "search_fts")
@Fts4(notIndexed = ["targetId", "type", "grade", "series", "subjectId", "phanMon", "topicId"])
data class SearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int = 0,
    val targetId: String,
    val type: String, // LESSON, QUESTION, FLASHCARD, CHAPTER, SUBJECT
    val grade: Int,
    val series: String, // kntt, cd, ctst, chung
    val subjectId: String,
    val phanMon: String = "",
    val topicId: String = "",
    val title: String,
    val titleNoAccent: String,
    val content: String,
    val contentNoAccent: String
)
