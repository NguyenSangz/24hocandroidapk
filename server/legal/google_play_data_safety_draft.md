# Bản Nháp Google Play Data Safety & Families Policy (F16a + F16b)

> **[CẦN THAM VẤN PHÁP LÝ & KIỂM TRA BỞI CHỦ SỞ HỮU]**: Tài liệu này tổng hợp thông số kỹ thuật hiện có của ứng dụng **Học Tốt THCS** để bạn điền biểu mẫu **Data safety** và **Target audience / Families** trên Google Play Console. Cần kiểm tra lại cùng chuyên gia pháp lý (Nghị định 13/2023/NĐ-CP Việt Nam) trước khi nộp xét duyệt.

---

## 1. Khai báo tổng quan (Google Play Data Safety)

| Câu hỏi trên Play Console | Khai báo khuyến nghị | Ghi chú kỹ thuật thực tế trong app |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (chỉ thu thập khi người dùng tự chọn đăng nhập / đồng bộ) | Chế độ Khách mặc định chỉ lưu Room cục bộ, không gửi dữ liệu ra ngoài. |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | Chỉ dùng HTTPS (TLS) tới máy chủ PocketBase tự vận hành; hỗ trợ `network_security_config.xml` + certificate pinning. |
| Do you provide a way for users to request that their data is deleted? | **Yes** | Trong app: **Tài khoản → Xóa tài khoản & Dữ liệu máy chủ** (xác nhận 2 bước). Trên web: `https://[TÔI CẦN ĐIỀN: domain]/delete-account.html` (`server/web/delete-account.html`). |
| Does your app share user data with third parties? | **No** | Không quảng cáo, không SDK phân tích/quảng cáo bên thứ ba, không bán dữ liệu. |

---

## 2. Chi tiết các loại dữ liệu thu thập (Data Types)

1. **Personal info** *(Thu thập tùy chọn — chỉ khi nâng cấp lên tài khoản)*:
   - **Email address**: Định danh đăng nhập, đặt lại mật khẩu qua SMTP, liên hệ sự cố.
   - **Name / Nickname**: Biệt danh học sinh (`displayName`).
   - **User IDs**: Mã bản ghi tài khoản PocketBase (`uid`) dùng để phân tách dữ liệu đồng bộ.
   - **Other info (Birth year / Parental consent metadata)**: Năm sinh để xác định ngưỡng dưới 16 tuổi; email phụ huynh (`parentEmail`) và thời điểm đồng ý (`parentConsentAt`) theo Nghị định 13/2023/NĐ-CP. `[TÔI CẦN KIỂM TRA: xác nhận lại cách khai báo trường phụ huynh trên Play Console]`.
2. **App activity / Education & Study Data** *(Đồng bộ F16b khi đăng nhập)*:
   - Tiến độ bài học, lịch sử luyện tập/kiểm tra, ngân hàng câu sai, tiến độ flashcard SM-2, điểm XP, huy hiệu, cài đặt app và danh sách động từ bất quy tắc đã thuộc.
   - **Mục đích:** App functionality (đồng bộ đa thiết bị và sao lưu tài khoản).
   - **Tính bắt buộc:** Optional (Người dùng vẫn dùng toàn bộ tính năng học ở Chế độ Khách).

---

## 3. Khai báo Target Audience & Families Policy `[CẦN THAM VẤN PHÁP LÝ]`

- **Nhóm tuổi mục tiêu:** Học sinh THCS Việt Nam (lớp 6–9, khoảng **11–15 tuổi**; cân nhắc chọn nhóm tuổi phù hợp trên Play Console như `13–15` hoặc `9–12 & 13–15` tùy đối tượng phát hành thực tế).
- **Quảng cáo:** Không chứa quảng cáo (`Contains ads = No`).
- **Mua hàng trong app (IAP):** Không có.
- **Bảo vệ trẻ em & Đồng ý phụ huynh:**
  - Học sinh dưới 16 tuổi được yêu cầu hoàn thành **cổng phụ huynh + email cha mẹ/người giám hộ** và lưu dấu thời gian đồng ý (`parentConsentAt`).
  - `[CẦN THAM VẤN PHÁP LÝ]`: Kiểm tra yêu cầu **xác minh email phụ huynh hai bước (double opt-in)** nếu cơ quan quản lý hoặc Google Play Families reviewer yêu cầu nâng mức xác thực phụ huynh trước khi mở đồng bộ đám mây cho trẻ dưới 13/16 tuổi.
- **Google Sign-In cho ứng dụng trẻ em:** `[TÔI CẦN KIỂM TRA]` Rà soát lại chính sách Google Play Families Program về việc hiển thị nút Google Sign-In vs Chế độ Khách mặc định cho nhóm tuổi đã chọn trên Play Console.

---

## 4. Checklist tự kiểm tra trước khi Submit lên Google Play

- [ ] Đã đăng `server/web/privacy.html` và `server/web/delete-account.html` lên HTTPS công khai và gắn đúng URL vào Play Console.
- [ ] Đã thay toàn bộ `[TÔI CẦN ĐIỀN]` trong `privacy_policy.md`, `privacy.html`, `delete-account.html` bằng tên miền và email hỗ trợ thật.
- [ ] Đã xác nhận `android:allowBackup="false"` và `data_extraction_rules.xml` tắt sao lưu tự động dữ liệu nhạy cảm qua Google Drive.
- [ ] Đã kiểm tra nút xóa tài khoản trong app và trang web xóa dữ liệu hoạt động đúng thực tế.
