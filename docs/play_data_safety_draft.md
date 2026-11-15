# Bản nháp Google Play Data Safety & Families Policy (F16a – Self-Hosted PocketBase)

> **[CẦN THAM VẤN PHÁP LÝ]** & **[KIỂM TRA THỦ CÔNG TRÊN PLAY CONSOLE]**: Tài liệu này phản ánh kiến trúc Offline-first với tùy chọn tài khoản **PocketBase tự host trên máy chủ Linux của nhà phát triển**.

## 1. Data collection and security
- **Does your app collect or share any of the required user data types?** -> **Yes** (chỉ khi người dùng tự nguyện đăng ký/đăng nhập; Chế độ Khách lưu 100% cục bộ trên máy).
- **Is all of the user data collected by your app encrypted in transit?** -> **Yes** (bắt buộc HTTPS qua Caddy / Cloudflare Tunnel + `network_security_config.xml` chặn cleartext traffic + hỗ trợ Certificate Pinning).
- **Do you provide a way for users to request that their data is deleted?** -> **Yes**:
  - Xóa trực tiếp trong app (2 bước tại màn hình Hồ sơ Tài khoản -> gọi `DELETE /api/custom/account`).
  - URL trang web xóa dữ liệu: `https://[TÔI CẦN ĐIỀN: api.tenmiencuatoi.vn]/delete-account.html`.

## 2. Data types collected (chỉ khi đăng nhập tài khoản)
| Nhóm dữ liệu | Loại dữ liệu | Mục đích | Bắt buộc / Tùy chọn | Chia sẻ bên thứ 3 |
|---|---|---|---|---|
| Personal info | Name (biệt danh), Email address (email người học & email phụ huynh nếu < 16t), Other info (năm sinh kiểm tra độ tuổi, mốc `parentConsentAt`) | App functionality (xác thực & tuân thủ đồng ý phụ huynh) | Optional (Chế độ Khách không thu thập) | No |
| App activity / App info | User-generated study progress (từ F16b: điểm, flashcard, câu sai, XP) | App functionality (đồng bộ đa thiết bị) | Optional | No |
| Device or other IDs | **FCM registration token** (chuỗi định danh thiết bị do Google Firebase Cloud Messaging cấp; lưu ở `device_tokens` trên máy chủ PocketBase tự host) | App functionality (gửi thông báo đẩy tùy chọn) | Optional – chỉ khi đã đăng nhập **và** chủ động bật thông báo; Chế độ Khách không gửi token | **CẦN KIỂM CHỨNG ĐIỀU KHOẢN**: token được gửi tới Google FCM để vận chuyển. Khai báo là “processed by service provider” hay “shared” tùy định nghĩa hiện hành của Play Console. `[CẦN LUẬT SƯ]` |
| App info and performance | Không thu thập telemetry tự động nếu không bật | - | - | No |

### 2b. Thông báo đẩy F20a – tóm tắt để khai báo
- **Thu thập:** Device or other IDs → Device ID / FCM token; thời điểm đăng ký (`updatedAt` do server đặt); nền tảng (`android`).
- **Mục đích:** App functionality – gửi nhắc học, bài tập, báo cáo, nội dung mới (chỉ khi người dùng bật).
- **Không thu thập qua FCM:** tên, email, điểm số, nội dung bài làm. Payload chỉ có `type` + `content_id`.
- **Mã hóa khi truyền:** Có (HTTPS tới PocketBase; FCM HTTP v1 dùng TLS).
- **Xóa:** tắt thông báo, đăng xuất, xóa tài khoản → gọi `/api/custom/push/unregister`; token chết được worker dọn khỏi `device_tokens`.
- **Bên thứ ba:** Google Firebase Cloud Messaging (Google LLC) – dịch vụ vận chuyển. `[CẦN LUẬT SƯ]` về chuyển dữ liệu xuyên biên giới và dữ liệu trẻ dưới 16 tuổi.
- **Play Console:** khai báo mục *Data safety* → *Device or other IDs*; rà lại *Families Policy* nếu chọn nhóm tuổi bao gồm trẻ dưới 13. `[CẦN KIỂM CHỨNG ĐIỀU KHOẢN]`

## 3. Google Play Families Policy & Nghị định 13/2023/NĐ-CP `[CẦN THAM VẤN PHÁP LÝ]`
- Không có quảng cáo, không bảng xếp hạng công khai, không chat/mạng xã hội.
- Có **Parental Gate** (phép tính người lớn + email phụ huynh + checkbox + timestamp máy chủ `parentConsentAt`) cho học sinh dưới 16 tuổi.
- Cần luật sư kiểm tra nội dung Chính sách quyền riêng tư (`privacy.html`), quy trình thông báo rò rỉ dữ liệu 72 giờ và mục *Target audience and content* (9–12, 13–15 tuổi) trên Play Console.
