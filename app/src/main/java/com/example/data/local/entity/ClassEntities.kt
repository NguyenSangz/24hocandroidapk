package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "teacher_name")
    val teacherName: String,

    @ColumnInfo(name = "join_code")
    val joinCode: String
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "class_id")
    val classId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "due_date")
    val dueDate: String?,

    @ColumnInfo(name = "test_id")
    val testId: String?
)
