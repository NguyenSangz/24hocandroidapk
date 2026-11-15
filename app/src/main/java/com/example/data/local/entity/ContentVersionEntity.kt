package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * F17a - Ghi nhận phiên bản nội dung đã nhập vào Room cho từng gói nội dung (content package).
 *
 * Mỗi file JSON trong `assets/curriculum/` là một "gói nội dung" với:
 * - `packageId`: định danh gói (không đổi trong suốt vòng đời gói).
 * - `contentVersion`: số nguyên tăng dần; tăng lên mỗi lần cập nhật nội dung của gói.
 *
 * Seeder so sánh `contentVersion` trong JSON với bản ghi ở bảng này:
 * - Nếu bản ghi chưa tồn tại hoặc nhỏ hơn -> nhập gói (upsert).
 * - Nếu bằng hoặc lớn hơn -> bỏ qua để tránh nhập lặp lại mỗi lần mở app.
 *
 * Bảng KHÔNG chứa dữ liệu cá nhân của người dùng, chỉ là dữ liệu kỹ thuật của app
 * nên không ảnh hưởng đến chính sách quyền riêng tư / Data safety.
 */
@Entity(tableName = "content_versions")
data class ContentVersionEntity(
    @PrimaryKey val packageId: String,
    /** Phiên bản nội dung đã nhập (lấy từ trường `contentVersion` của file JSON). */
    val version: Int,
    /** Thời điểm nhập gói (epoch millis) - phục vụ debug/kiểm tra. */
    val importedAtMillis: Long
)
