# LEGAL_FLAGS – Các điểm cần luật sư / kiểm chứng điều khoản

> Không tự khẳng định đã tuân thủ. Mỗi mục ghi rõ vị trí và việc cần làm.

## LF-01. Dùng Google FCM cho trẻ dưới 16 tuổi `[CẦN LUẬT SƯ]`

- **Vị trí:** `app/src/main/assets/legal/privacy_policy.md` mục 2b; `server/web/privacy.html` mục 2b.
- **Vấn đề:** FCM là bên thứ ba (Google LLC); cần đánh giá cơ sở pháp lý và sự đồng ý của phụ huynh
  theo Nghị định 13/2023/NĐ-CP khi gửi mã thiết bị của học sinh.
- **Việc cần:** luật sư xác nhận cách xin đồng ý (gộp vào đồng ý phụ huynh hiện có hay riêng),
  và nội dung thông báo cho phụ huynh.

## LF-02. Chuyển dữ liệu ra nước ngoài `[CẦN LUẬT SƯ]`

- **Vị trí:** chính sách riêng tư mục 2b; Data safety.
- **Vấn đề:** mã thiết bị + siêu dữ liệu gửi tới Google (ngoài Việt Nam).
- **Việc cần:** đánh giá nghĩa vụ theo NĐ 13/2023 (hồ sơ đánh giá tác động chuyển dữ liệu xuyên biên giới)
  và điều khoản Google Firebase.

## LF-03. Khai báo Google Play Data safety `[CẦN KIỂM CHỨNG ĐIỀU KHOẢN]`

- **Vị trí:** `docs/play_data_safety_draft.md` mục 2b.
- **Vấn đề:** xác định FCM token thuộc “Device or other IDs” và khai báo là *collected* / *shared*
  hay *processed by service provider* theo định nghĩa hiện hành của Play Console.
- **Việc cần:** đối chiếu Play Console lúc phát hành; không dựa vào trí nhớ.

## LF-04. Families Policy / Target audience

- **Vị trí:** hồ sơ Play Console, chính sách riêng tư.
- **Vấn đề:** nếu chọn nhóm tuổi bao gồm trẻ dưới 13, yêu cầu về SDK bên thứ ba và quảng cáo khác.
- **Việc cần:** luật sư + Play Console xác nhận; hiện app không quảng cáo.

## LF-05. Nội dung thông báo

- **Vị trí:** `server/pb_hooks/f20a_push.pb.js`, `server/push/fcm_client.mjs`.
- **Vấn đề:** đã giới hạn payload chỉ `type` + `content_id`; cần giữ nguyên khi làm F20b và
  không đưa tên/điểm vào tiêu đề thông báo hiển thị trên màn hình khóa (người khác có thể nhìn thấy).
- **Việc cần:** rà soát UX F20b, cho phép tắt hiển thị nội dung nhạy cảm trên màn hình khóa nếu cần.

## LF-06. Lưu giữ và xóa token

- **Vị trí:** `device_tokens`, `push_outbox`, hook xóa tài khoản.
- **Vấn đề:** cần nêu thời hạn lưu token và cam kết xóa khi tắt/đăng xuất/xóa tài khoản.
- **Việc cần:** điền thời hạn cụ thể `[TÔI CẦN ĐIỀN]` và luật sư xác nhận.

## LF-07. Xác nhận phụ huynh cho người dưới 16 tuổi `[CẦN LUẬT SƯ]`

- **Vị trí:** `app/src/main/java/com/example/ui/screens/auth/ParentalConsentScreen.kt`,
  `server/pb_hooks/f16a_auth_and_account.pb.js`, `server/web/parental-consent.html`,
  `server/pb_migrations/1700000005_parental_consent_verification.js`.
- **Cách làm kỹ thuật hiện tại:** người dùng nhập năm sinh và email; server gửi link dùng một lần,
  phụ huynh cần nhấn nút xác nhận; đồng ý cũ của tài khoản trẻ được đặt lại; sync được chặn tới
  khi xác nhận.
- **Vấn đề cần thẩm định:** năm sinh là tự khai (không có ngày/tháng), quyền sở hữu email chỉ chứng
  minh quyền truy cập hộp thư chứ không chứng minh tuổi/quan hệ giám hộ hoặc đồng ý có hiểu biết;
  cần xác định căn cứ, nội dung thông báo, lưu bằng chứng và thời hạn lưu dữ liệu theo luật áp dụng.
- **Việc cần:** luật sư chuyên về dữ liệu cá nhân/trẻ em xác nhận phương thức; không coi email link
  hiện tại là bằng chứng tuân thủ hoặc thay thế quy trình pháp lý.
- **Triển khai:** chưa chạy migration trên server thật; phải backup, thử SMTP/HTTPS, expiry/replay,
  và có kế hoạch với tài khoản trẻ hiện hữu trước khi bật.
