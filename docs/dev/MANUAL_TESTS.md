# MANUAL_TESTS – Việc phải thử tay trên máy thật

> Đánh dấu [ ] khi chưa thử, [x] khi đã thử và ghi kết quả/ngày.
>
> **Trạng thái F20a (2026-11-13):** phần **tự động** đã xanh – toàn bộ 206 unit test
> app (gồm 21 test push) và 14 test server pass; `assembleDebug` thành công. Các mục
> dưới đây **vẫn chưa thử tay** vì cần service account FCM + thiết bị thật.

## P2.5 – Xác nhận phụ huynh (chưa thử trên server thật)

- [ ] Tài khoản dưới 16 tuổi chưa xác nhận: push/pull sync trả 403, cục bộ vẫn giữ outbox.
- [ ] Yêu cầu email: nhận đúng link HTTPS; tải trang hoặc email link scanner không xác nhận;
  chỉ nhấn nút mới gửi POST confirm.
- [ ] Xác nhận thành công: refresh trong app thấy trạng thái đã xác nhận; push/pull sync được mở.
- [ ] Dùng lại link và dùng link hết hạn sau 1 giờ: server từ chối, không sửa trạng thái.
- [ ] Yêu cầu link mới: link trước đó mất hiệu lực; xác nhận chỉ qua link mới.
- [ ] Gửi yêu cầu lần thứ 4 trong 1 giờ: 429; chờ qua cửa sổ hoặc xác minh limiter sau server restart.
- [ ] Mô phỏng SMTP lỗi: app không đánh dấu đã đồng ý và challenge tạo dở bị xóa.
- [ ] Migration: backup trước; kiểm tra tài khoản trẻ cũ bị yêu cầu xác nhận lại, tài khoản trưởng thành
  được phân loại theo quy tắc hiện tại; không xóa local outbox/progress.
- [ ] Xóa tài khoản đang có challenge: challenge bị xóa theo transaction/cascade.

## F20a – Thông báo đẩy

- [ ] **Build có bật cờ:** thêm `ENABLE_PUSH=true` vào `.env`, build, cài lên máy có Google Play Services.
- [ ] **Ẩn khi thiếu GPS:** cài lên emulator không có Google Play / máy không có GPS → mục "Thông báo đẩy" **không xuất hiện**, app không crash.
- [ ] **Khách:** ở chế độ Khách, mở Cài đặt → thấy giải thích không gửi token; công tắc bị vô hiệu hóa; kiểm tra `device_tokens` trên server **không có** bản ghi.
- [ ] **Đăng nhập lần đầu:** đăng nhập → bật công tắc → hộp thoại giải thích hiện ra → cấp quyền → kiểm tra `device_tokens` có token đúng `userId`.
- [ ] **Từ chối quyền:** từ chối quyền POST_NOTIFICATIONS → công tắc tắt, snackbar có nút "Mở cài đặt" → bấm mở đúng màn hình thông báo của app.
- [ ] **Tắt thông báo:** tắt công tắc → bản ghi token biến mất khỏi server; nhắc học cục bộ vẫn chạy.
- [ ] **Đăng xuất:** đăng xuất → token biến mất khỏi server. Thử lại khi **bật máy bay** rồi đăng xuất → app không treo; token cục bộ bị xóa; khi có mạng lại, server vẫn có thể còn token cũ (worker sẽ dọn khi gửi).
- [ ] **Xóa tài khoản:** xóa tài khoản 2 bước → `device_tokens` + `push_outbox` của user biến mất.
- [ ] **Payload sạch:** enqueue thử với `contentId` bình thường; bắt log FCM (hoặc xem network) → chỉ có `type` + `content_id`, không có tên/điểm/email.
- [ ] **Token hết hạn:** xóa dữ liệu app/Firebase để token cũ chết, enqueue lại → worker đánh dấu `failed`, xóa dòng `device_tokens`.
- [ ] **Rate limit:** gọi register liên tục > 30 lần/5 phút → nhận 429, app xử lý không crash (chỉ log và thử lại sau).
- [ ] **Hai tài khoản một máy:** đăng nhập A → bật push → đăng xuất → đăng nhập B → bật push → kiểm tra token chuyển sang B, không còn thuộc A.
- [ ] **Chữ lớn/TalkBack:** bật font 200% và TalkBack tại mục "Thông báo đẩy" → đọc được nhãn, công tắc, giải thích; vùng chạm ≥ 48dp.
- [ ] **Chế độ tối:** mở Cài đặt ở dark mode → thẻ thông báo đẩy hiển thị đúng màu.

## Server / worker

- [ ] **Chạy worker khi outbox trống:** log "Không có thông báo chờ gửi.", exit code 0.
- [ ] **Enqueue bằng curl:** trả `{"enqueued":1}`; worker gửi thành công, `status=sent`.
- [ ] **Sai secret:** enqueue với secret sai → 401; thiếu env → 503.
- [ ] **Sai service account:** dùng JSON hỏng → worker log lỗi rõ, không lộ khóa; exit code 1.
- [ ] **Quyền file:** `ls -l /etc/pocketbase/fcm-service-account.json` → `-rw-------`; nếu rộng hơn, worker cảnh báo.
- [ ] **Systemd timer:** `systemctl list-timers push-sender.timer` → chạy mỗi phút; `journalctl` không có token/PII.

## Hồi quy (bắt buộc trước khi kết luận F20a an toàn)

- [ ] Tắt cờ `ENABLE_PUSH=false` → toàn bộ luồng học, luyện tập, kiểm tra, đồng bộ, nhắc học cục bộ hoạt động y như trước.
- [ ] Máy bay giữa lúc học → không treo, không mất dữ liệu; bật mạng lại → đồng bộ bình thường.
- [ ] Đăng nhập hai máy → dữ liệu vẫn gộp đúng, không cộng đúp XP.
