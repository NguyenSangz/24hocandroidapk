# DECISIONS – Nhật ký quyết định kỹ thuật

## 2026-11-15 – P2.5: xác nhận trẻ qua liên kết email một lần

- **Quyết định:** email phụ huynh phải mở trang cùng origin và nhấn nút xác nhận rõ ràng; tải trang đơn thuẩn không tiêu thụ token. Challenge có hạn dùng 1 giờ, lần yêu cầu mới vô hiệu hóa challenge cũ, giới hạn yêu cầu 3 lần/giờ/tài khoản và xác nhận xóa challenge.
- **Lý do:** checkbox/câu hỏi trong app chỉ là kiểm tra giao diện, không chứng minh người giám hộ đã đồng ý. Không tự xác nhận khi trang tải nhằm tránh email security scanners tiêu thụ liên kết.
- **Quyết định:** đặt lại trạng thái đồng ý của tài khoản cũ dưới 16 tuổi hoặc thiếu năm sinh trong migration; chặn push/pull sync và đăng ký FCM ở server trong khi chưa xác nhận; app không cấp đồng ý nếu request lỗi mạng/server.
- **Hệ quả:** người dùng trẻ hiện hữu phải xác nhận lại sau migration; dữ liệu outbox trên app được giữ. Migration chưa được chạy trên máy chủ thật.
- **Giới hạn:** năm sinh tự khai và email xác nhận không chứng minh chắc chắn tuổi hay quan hệ giám hộ; đây chưa phải kết luận tuân thủ pháp lý.

## 2026-11-15 – P2.5: tạm lưu challenge tối đa một giờ

- **Quyết định:** challenge chứa email phụ huynh và token dùng một lần, chỉ trong collection không có API rules; token bị xóa khi xác nhận hoặc theo cron dọn hết hạn mỗi 30 phút.
- **Lý do:** giữ dữ liệu xác nhận tách khỏi profile cho tới khi người giám hộ chủ động xác nhận.
- **Rủi ro còn lại:** token hiện được lưu dạng rõ trong database server và rate-limit nằm trong `$app.store()` có thể mất khi restart; cần đánh giá hardening trước production.

## 2026-11-13 – F20a: tách F20 thành 4 phần, chỉ làm F20a

- **Quyết định:** F20 được chia F20a (hạ tầng FCM), F20b (hiển thị/kênh/giờ yên lặng), F20c (gia sư AI), F20d (chi phí/ngân sách). Phiên này chỉ làm F20a, dừng chờ "tiếp".
- **Lý do:** tránh gộp thay đổi lớn, dễ rollback; F20b/c/d phụ thuộc quyết định sản phẩm/pháp lý riêng.
- **Phương án bỏ:** làm toàn bộ F20 trong một lần (rủi ro cao, không kiểm chứng được).

## 2026-11-13 – Chỉ thêm `firebase-messaging`, không Analytics/Crashlytics

- **Quyết định:** dependency duy nhất thêm là `com.google.firebase:firebase-messaging` qua BOM có sẵn.
- **Lý do:** nguyên tắc dữ liệu tối thiểu; Analytics/Crashlytics cần đồng ý riêng và khai báo chính sách.
- **Hệ quả:** kích thước APK tăng nhẹ; không có telemetry tự động.

## 2026-11-13 – Cờ `ENABLE_PUSH` mặc định false

- **Quyết định:** `BuildConfig.ENABLE_PUSH` đọc từ `.env`/`local.properties`, mặc định `false`.
- **Lý do:** khi chưa có service account/kiểm thử thật, app phải hoạt động y hệt trước đây.
- **Hệ quả:** mục "Thông báo đẩy" ẩn hoàn toàn; không xin quyền, không đăng ký token.

## 2026-11-13 – Sửa harness `WrongBankFlowComposeUiTest` (không đổi assertion)

- **Vấn đề:** `waitUntil { !viewModel.uiState.value.isLoading }` timeout vô hạn dù đã
  tăng lên 30 giây. Nguyên nhân là Robolectric giữ main looper ở `PAUSED`; `waitUntil`
  với `StateFlow` thường không đẩy looper nên coroutine DataStore/Room không được resume.
- **Quyết định:** gọi `ShadowLooper.idleMainLooper()` **bên trong** điều kiện chờ;
  giữ nguyên timeout 5 giây và mọi assertion. Không xóa/skip test.
- **Lý do:** sửa đúng nguyên nhân môi trường test, không nới lỏng tiêu chí kiểm tra.
- **Hệ quả:** lớp test pass 2/2; toàn bộ 206 test pass.

## 2026-11-13 – Token do `PushTokenManager` điều phối, hủy trước khi xóa phiên

- **Quyết định:** `PushTokenManager` quan sát `authRepository.accountState` + `settingsRepository.pushEnabled`, và `PocketBaseAuthRepository` gọi hook `onSessionEnding` **trước** `tokenStorage.clearSession()`.
- **Lý do:** cần Authorization còn hiệu lực để báo server xóa token khi đăng xuất/xóa tài khoản.
- **Phương án bỏ:** xóa token sau khi clear session (sẽ nhận 401, token sót trên server).

## 2026-11-13 – `PushLocalStore` thay vì phụ thuộc trực tiếp `UserPreferencesManager`

- **Quyết định:** tách interface `PushLocalStore` (pushEnabled, token, registeredAt) để test logic đăng ký bằng bản giả, không cần Android Context.
- **Lý do:** test nhanh, ổn định; giữ DataStore là chi tiết triển khai.
- **Hệ quả:** `UserPreferencesManager` implement interface này; không đổi hành vi.

## 2026-11-13 – Payload FCM chỉ có `type` + `content_id`

- **Quyết định:** `PushPayloadParser` whitelist đúng 2 khóa; `content_id` chỉ chữ/số/`_`/`.`/`-`, tối đa 128 ký tự. Khóa lạ bị bỏ hoàn toàn.
- **Lý do:** không để tên/điểm/email đi qua Google FCM; chữ hiển thị dựng cục bộ ở F20b.
- **Phương án bỏ:** gửi title/body qua FCM (vi phạm dữ liệu tối thiểu).

## 2026-11-13 – Chống đăng ký lặp: cùng token trong 12 giờ thì bỏ qua

- **Quyết định:** nếu token không đổi và lần đăng ký trước < 12 giờ, không gọi server; token mới từ `onNewToken` luôn đăng ký lại.
- **Lý do:** tránh đập rate limit khi mở app nhiều lần; server vẫn upsert idempotent.
- **Hệ quả:** nếu server mất dữ liệu token, tối đa 12 giờ sau mới tự đăng ký lại (ngoài luồng đăng nhập/tắt-bật).

## 2026-11-13 – `pushEnabled` là cài đặt theo thiết bị, không đồng bộ

- **Quyết định:** không đưa `pushEnabled` vào snapshot `sync_settings`.
- **Lý do:** quyền thông báo và token gắn với từng thiết bị; đồng bộ sẽ gây bật/tắt chéo máy.
- **Phương án bỏ:** đồng bộ công tắc (hai máy chia sẻ một token khái niệm sai).

## 2026-11-13 – Server: `device_tokens` + `push_outbox`, endpoint custom

- **Quyết định:**
  - `device_tokens`: unique index theo `token`, relation `userId` cascade delete, autodate `updatedAt`.
  - `push_outbox`: chỉ admin/worker truy cập; `dedupeKey` unique; lưu bản sao `token`.
  - Endpoint `POST /api/custom/push/register|unregister` cho app; `enqueue` chỉ server/ops với `PUSH_ENQUEUE_SECRET`.
- **Lý do:** app không được ghi trực tiếp collection; worker cần đọc nhanh, không join.
- **Phương án bỏ:** cho app tạo record trực tiếp (khó rate limit, dễ lạm dụng).

## 2026-11-13 – Worker Node zero-dependency, chạy bằng systemd timer

- **Quyết định:** `server/push/fcm_sender.mjs` dùng `fetch` + `node:crypto`, mỗi lượt xử lý tối đa `MAX_PER_RUN`, giãn cách `SEND_INTERVAL_MS`.
- **Lý do:** ít bề mặt tấn công, không npm install trên server, dễ kiểm toán.
- **Phương án bỏ:** dùng Firebase Admin SDK (nặng, thêm chuỗi cung ứng npm).

## 2026-11-13 – Service account FCM nằm ngoài repo

- **Quyết định:** chỉ đường dẫn qua `FCM_SERVICE_ACCOUNT_FILE`; worker cảnh báo nếu quyền file rộng hơn `600`.
- **Lý do:** nguyên tắc bất biến "không bí mật trong repo".
- **Hệ quả:** người dùng phải tự tạo/tải JSON (BLOCKERS B1).
