# RISKS – Rủi ro còn lại

| ID | Rủi ro | Mức | Giảm thiểu hiện tại | Việc còn lại |
|---|---|---|---|---|
| R20a-01 | Token FCM bị lộ qua log/crash report | Cao | Không log token; chỉ log mã loại; payload không có PII | Rà log worker/app trước phát hành |
| R20a-02 | Google FCM xử lý siêu dữ liệu thiết bị (IP, model) | Cao (trẻ em) | Payload tối giản; chính sách nêu rõ bên thứ ba | **CẦN LUẬT SƯ** + khai báo Play |
| R20a-03 | Token thuộc tài khoản cũ khi đổi tài khoản trên cùng máy | Vừa | Endpoint register chuyển quyền sở hữu token sang user hiện tại | Test tay 2 tài khoản trên 1 máy |
| R20a-04 | Đăng xuất khi mất mạng → token còn trên server | Vừa | App xóa cục bộ + gọi unregister; worker tự xóa token khi FCM báo chết | Thử máy bay khi đăng xuất |
| R20a-05 | Rate limit trong bộ nhớ (`$app.store`) mất khi restart | Thấp | Cửa sổ 5 phút, giới hạn 30; server restart hiếm | Có thể chuyển sang bảng đếm nếu bị lạm dụng |
| R20a-06 | Worker và app dùng chung quyền admin PocketBase | Cao | `push.env` chmod 600; worker chạy user `pocketbase`; không mở port | Rà quyền file, cân nhắc token admin chỉ đọc outbox |
| R20a-07 | Dịch vụ Google Play không có → FCM crash | Vừa | `FirebaseFcmGateway` bọc try/catch, kiểm tra `GoogleApiAvailability` | Test máy không GPS |
| R20a-08 | Người dùng từ chối quyền nhưng công tắc vẫn bật | Vừa | Launcher kiểm tra, tắt công tắc + snackbar mở Cài đặt | Test tay Android 13+ |
| R20a-09 | Token đăng ký lặp gây rate limit | Thấp | Bỏ qua nếu cùng token < 12h; server upsert | Theo dõi log 429 |
| R20a-10 | Outbox phình do token chết lặp lại | Thấp | Worker xóa `device_tokens`, đánh dấu `failed` | Giám sát số bản ghi pending |
| R20a-11 | Khai báo Data safety thiếu mục Device ID | Cao | Đã bổ sung bản nháp | **CẦN KIỂM CHỨNG ĐIỀU KHOẢN** trên Play Console |
| R20a-12 | `ENABLE_PUSH=false` nhưng vẫn xin quyền | Thấp | Toàn bộ UI/logic nằm sau `isSupported` | Test bản không bật cờ |
| R20a-13 | `packages` Firebase mới làm tăng thời gian build/APK | Thấp | Chỉ thêm messaging, không Analytics | Đo kích thước ở P6 |
| R20a-14 | Hook `require()` không tương thích phiên bản PocketBase cũ | Vừa | Dùng cú pháp chuẩn JSVM hiện hành | Xác nhận phiên bản PB trên server thật |
| R20a-15 | Xóa tài khoản không xóa hết token do lỗi transaction | Thấp | Hook xóa `push_outbox` + `device_tokens` trong transaction | Test xóa tài khoản thật |
| P2.5-01 | Email + năm sinh tự khai không chứng minh quan hệ giám hộ hoặc ý chí đồng ý hợp lệ | Cao | Phụ huynh phải nhấn nút trong email; ghi rõ cần tư vấn pháp lý | Luật sư đánh giá phương thức xác minh, nội dung đồng ý và quy trình xử lý dữ liệu trẻ em |
| P2.5-02 | Challenge token lưu dạng rõ trong database đến khi xác nhận/hết hạn | Cao | Collection không có API rules; token ngẫu nhiên, hạn 1 giờ, xóa sau xác nhận/cron | Hash token trước lưu; kiểm tra backup/log/monitor không giữ token |
| P2.5-03 | Rate limit xác nhận phụ huynh ở process store có thể mất khi PocketBase restart | Vừa | Tối đa 3 request/giờ/tài khoản trong runtime hiện tại | Dùng persistent limiter nếu cần chống lạm dụng qua restart |
| P2.5-04 | Migration đặt lại đồng ý cũ cho tài khoản trẻ hoặc thiếu năm sinh | Cao | Fail closed; outbox trên thiết bị không bị xóa | Backup, thông báo người dùng, đo ảnh hưởng và thử rollback trước triển khai |
