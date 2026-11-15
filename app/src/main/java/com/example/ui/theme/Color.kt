package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Bảng màu "thân thiện, không màu mè" cho học sinh THCS.
// Nguyên tắc: tông pastel dịu, độ bão hòa thấp, một màu chủ đạo
// (xanh dương dịu) + màu phụ (xanh ngọc sage) + điểm nhấn vàng nhạt.
// Không neon, không gradient nhiều màu.
// ============================================================

// Light Theme Colors - dịu mắt, gần gũi
val PrimaryBlue = Color(0xFF4A7FA8)            // xanh dương dịu (màu chủ đạo)
val PrimaryBlueContainer = Color(0xFFE9F1F8)   // nền xanh rất nhạt
val OnPrimaryBlueContainer = Color(0xFF2C5578)

val SecondaryEmerald = Color(0xFF55897B)       // xanh ngọc sage nhẹ nhàng
val SecondaryEmeraldContainer = Color(0xFFEDF5F1)
val OnSecondaryEmeraldContainer = Color(0xFF2F5C50)

val TertiaryAmber = Color(0xFFC08A52)          // vàng nhạt ấm (điểm nhấn tiết chế)

val ErrorRed = Color(0xFFC26161)               // đỏ dịu, không chói
val ErrorContainer = Color(0xFFFAF0EF)

val BackgroundLight = Color(0xFFF7F8FA)        // trắng xám nhẹ
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEFF2F5)
val OutlineLight = Color(0xFFC7CFD8)
val OutlineVariantLight = Color(0xFFE3E8ED)
val TextPrimaryLight = Color(0xFF2F3B47)       // xám xanh đậm, đỡ gắt hơn đen thuần
val TextSecondaryLight = Color(0xFF6B7A88)

// Dark Theme Colors - tối giản, xám trầm, không xanh đen chói
val PrimaryBlueDark = Color(0xFF8FB8D9)
val PrimaryBlueContainerDark = Color(0xFF2C4A66)
val SecondaryEmeraldDark = Color(0xFF8FC4B4)
val SecondaryEmeraldContainerDark = Color(0xFF2E4A42)
val BackgroundDark = Color(0xFF171B21)
val SurfaceDark = Color(0xFF20252C)
val SurfaceVariantDark = Color(0xFF2C333B)
val OutlineDark = Color(0xFF525C66)
val OutlineVariantDark = Color(0xFF3A424B)
val TextPrimaryDark = Color(0xFFE8ECEF)
val TextSecondaryDark = Color(0xFF9AA8B4)

// Subject Accent Colors (15 môn GDPT 2018) - tông trầm dịu, vẫn phân biệt được
val SubjectMath = Color(0xFF4A7FA8)
val SubjectLiterature = Color(0xFFA8763F)
val SubjectEnglish = Color(0xFF3E8E9C)
val SubjectPhysics = Color(0xFF5B6BAE)
val SubjectChemistry = Color(0xFF7C5CAD)
val SubjectBiology = Color(0xFF4E9E6A)
val SubjectHistory = Color(0xFFB26B4A)
val SubjectGeography = Color(0xFF4C8FB0)
val SubjectCivic = Color(0xFFB25E6B)
val SubjectInformatics = Color(0xFF4C8FB0)
val SubjectTechnology = Color(0xFF5E6B77)
val SubjectPE = Color(0xFF3F9E6E)
val SubjectMusic = Color(0xFF6C77B0)
val SubjectArt = Color(0xFF8B62A8)
val SubjectExperiential = Color(0xFFC08A52)
val SubjectScience = Color(0xFF5B6BAE)
val SubjectHistoryGeo = Color(0xFFB26B4A)

// Status feedback colors - xanh/đỏ dịu cho phản hồi đúng/sai
val CorrectGreen = Color(0xFF3F9E6E)
val IncorrectRed = Color(0xFFC26161)
val GoldenStar = Color(0xFFDFA34F)

/**
 * Tra cứu màu sắc đặc trưng của từng môn GDPT 2018 (F11 - Thống kê học tập)
 */
object SubjectColors {
    fun getColorForSubject(subjectId: String): Color {
        return when (subjectId.lowercase()) {
            "math", "toan" -> SubjectMath
            "literature", "van" -> SubjectLiterature
            "english", "tienganh" -> SubjectEnglish
            "physics", "vatly" -> SubjectPhysics
            "chemistry", "hoahoc" -> SubjectChemistry
            "biology", "sinhhoc" -> SubjectBiology
            "history", "lichsu" -> SubjectHistory
            "geography", "dialy" -> SubjectGeography
            "civic", "gdcd" -> SubjectCivic
            "informatics", "tinhoc" -> SubjectInformatics
            "technology", "congnghe" -> SubjectTechnology
            "pe", "theduc" -> SubjectPE
            "music", "amnhac" -> SubjectMusic
            "art", "mythuat" -> SubjectArt
            "experiential", "hdtn", "hoatdong" -> SubjectExperiential
            "science", "khtn" -> SubjectScience
            "history_geo", "ls_dl" -> SubjectHistoryGeo
            else -> PrimaryBlue
        }
    }
}
