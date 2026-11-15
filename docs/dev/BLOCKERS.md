# BLOCKERS – Việc cần con người

> Nguyên tắc: không bịa khóa/không giả lập "đã triển khai". Việc nào chưa làm được
> thì ghi rõ ai làm, lệnh cần chạy, và việc gì bị chặn theo.

## B1. Service account FCM + cấu hình Firebase (TÔI CẦN LÀM)

- **Cần:** file JSON service account của Firebase project `gen-lang-client-0422573454`
  (hoặc project bạn chọn), tải từ Firebase Console → Project settings → Service accounts.
- **Cách làm:**
  1. Generate new private key → tải JSON.
  2. Đưa lên máy chủ: `/etc/pocketbase/fcm-service-account.json`.
  3. `chown pocketbase:pocketbase ... && chmod 600 ...`.
  4. Đặt `FCM_SERVICE_ACCOUNT_FILE=/etc/pocketbase/fcm-service-account.json` trong `/etc/pocketbase/push.env`.
- **Bị chặn:** gửi FCM thật, kiểm thử token hết hạn, kiểm thử tải/rate limit thật.
- **Không bị chặn:** toàn bộ code app/server + unit test đã chạy bằng bản giả.

## B2. Bật cờ `ENABLE_PUSH=true` khi build (TÔI CẦN LÀM)

- **Cần:** thêm `ENABLE_PUSH=true` vào `.env` hoặc `local.properties` (đã gitignore).
- **Bị chặn:** nhìn thấy mục "Thông báo đẩy" trong Cài đặt và kiểm thử trên máy thật.
- **Mặc định hiện tại:** `false` – app chạy y như trước, không xin quyền.

## B3. Triển khai worker + secret trên máy chủ Linux (TÔI CẦN LÀM)

- **Cần:**
  - `PUSH_ENQUEUE_SECRET` (≥ 32 ký tự ngẫu nhiên) trong `/etc/pocketbase/pocketbase.env`.
  - `/etc/pocketbase/push.env` từ `server/push/push.env.example` (chmod 600).
  - Cài `push-sender.service` + `push-sender.timer` (xem `server/push/README.md`).
- **Bị chặn:** gửi định kỳ, kiểm thử backoff/queue thật.

## B4. Thông tin hạ tầng còn placeholder (TÔI CẦN ĐIỀN)

| Biến | Giá trị cần điền | Ảnh hưởng |
|---|---|---|
| Domain API | `api.tenmiencuatoi.vn` | Mọi kết nối app/server |
| Vị trí máy chủ | quốc gia/nhà cung cấp | Chính sách riêng tư, chuyển dữ liệu |
| Email hỗ trợ | `hotro@...` | Trang xóa dữ liệu, khiếu nại |
| Thời hạn lưu giữ | ví dụ 12 tháng | Chính sách riêng tư |
| Thời hạn backup | ví dụ 30 ngày | Chính sách riêng tư |

## B5. Luật sư rà soát pháp lý (TÔI CẦN LÀM)

- **Cần:** rà soát Nghị định 13/2023/NĐ-CP + Google Play Families + điều khoản Google FCM.
- **Bị chặn:** phát hành production có bật `ENABLE_PUSH`; khai báo Data safety cuối.
- Xem chi tiết `LEGAL_FLAGS.md`.

## B6. Thiết bị thật có/không có Google Play Services (TÔI CẦN THỬ)

- **Cần:** 1 máy có GPS (để nhận FCM) và 1 máy/emulator không có GPS (để kiểm tra ẩn tính năng).
- **Bị chặn:** xác nhận hành vi ẩn tính năng và nhận thông báo thật.

## B7. Triển khai và kiểm chứng xác nhận phụ huynh P2.5 (TÔI CẦN LÀM)

- **Cần:** backup database; cài migration `1700000005_parental_consent_verification.js`,
  hook `f16a_auth_and_account.pb.js`, `f16b_sync_hooks.pb.js`, `f20a_push.pb.js` và
  `web/parental-consent.html` lên cùng PocketBase; cấu hình `PB_PUBLIC_URL` HTTPS và SMTP.
- **Cần thử:** email nhận đúng; tải trang chưa xác nhận cho đến khi nhấn nút; token hết hạn,
  dùng lại hoặc token cũ sau request mới bị từ chối; sync và FCM bị chặn trước xác nhận,
  hoạt động sau xác nhận; dữ liệu outbox còn nguyên.
- **Bị chặn:** chưa có máy chủ/SMTP thật để chạy end-to-end; migration reset đồng ý hiện có
  của tài khoản trẻ nên phải thông báo và có backup/rollback plan trước khi áp dụng.

## B8. Gradle output bị khóa cho quality gate P2.5 (TÔI CẦN XỬ LÝ)

- **Phát hiện 2026-11-15:** `java` không có trong PATH nhưng JBR có tại
  `C:\Program Files\Android\Android Studio\jbr`; đã chạy Gradle dùng JBR.
- **Lỗi:** `:app:generateDebugRFile` không xóa được
  `app\build\intermediates\compile_r_class_jar\debug\generateDebugRFile\R.jar`;
  Gradle daemon PID 4316 vẫn báo `BUSY`. Không dừng daemon vì chưa xác định có IDE build đang dùng nó.
- **Cần:** khi file không còn bị khóa, chạy lại `:app:testDebugUnitTest --tests
  com.example.AccountAuthUnitTest --tests com.example.PocketBaseSyncIntegrationTest`,
  sau đó `:app:assembleDebug` và lint thích hợp.
- **Bị chặn:** chưa có bằng chứng Kotlin compile/test/assemble/lint hiện hành; không coi kết quả
  ngày 2026-11-13 là xác minh cho thay đổi P2.5.
