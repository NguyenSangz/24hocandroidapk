# Chính Sách Quyền Riêng Tư – Học Tốt THCS (GDPT 2018)

*Cập nhật cho kiến trúc Offline-First + Máy chủ PocketBase tự vận hành (F16)*

> **[CẦN THAM VẤN PHÁP LÝ]**: Tài liệu này là bản nháp kỹ thuật phục vụ phát triển ứng dụng cho học sinh THCS (11–15 tuổi) theo **Nghị định 13/2023/NĐ-CP** (Bảo vệ dữ liệu cá nhân) và **Google Play Families Policy**. Trước khi phát hành công khai trên Google Play, chủ sở hữu ứng dụng cần rà soát cùng luật sư / chuyên gia pháp lý.

## 1. Nguyên tắc Offline-First & Chế độ Khách (Không bắt buộc đăng nhập)
- Bạn có thể sử dụng toàn bộ tính năng học tập (bài học, luyện tập, kiểm tra, flashcard, công cụ môn học) ở **Chế độ Khách** hoàn toàn ngoại tuyến mà **không cần đăng nhập** và **không gửi dữ liệu ra ngoài thiết bị**.
- Ứng dụng **không quảng cáo**, không tích hợp SDK theo dõi quảng cáo, không bán hay chia sẻ dữ liệu học sinh cho bên thứ ba.

## 2. Dữ liệu thu thập khi bạn tự nguyện tạo/đăng nhập Tài khoản
Khi bạn chọn nâng cấp từ Chế độ Khách lên tài khoản (bằng Email hoặc Google) để đồng bộ và sao lưu:
- **Thông tin định danh tối thiểu:** Email tài khoản, mã định danh (`id`), biệt danh học sinh, khối lớp và bộ sách mặc định.
- **Bảo vệ học sinh dưới 16 tuổi (`[CẦN THAM VẤN PHÁP LÝ]`):** Năm sinh (để xác định ngưỡng dưới 16 tuổi), email cha mẹ/người giám hộ và mốc thời gian xác nhận đồng ý (`parentConsentAt`). Email xác nhận chứa liên kết dùng một lần, hết hạn sau một giờ; yêu cầu chưa xác nhận được máy chủ dọn định kỳ. Cách xác minh và hiệu lực pháp lý cần luật sư rà soát trước khi phát hành.
- **Dữ liệu học tập đồng bộ (từ F16b):** Tiến độ bài học, lịch sử luyện tập/kiểm tra, ngân hàng câu sai, flashcard SM-2, XP/chuỗi ngày/huy hiệu.

## 2b. Thông báo đẩy (tùy chọn, F20a) `[CẦN LUẬT SƯ]`
- **Chỉ khi bạn đăng nhập và chủ động bật** công tắc “Thông báo đẩy” trong Cài đặt, ứng dụng mới lấy **mã thiết bị FCM** (một chuỗi định danh do Google Firebase Cloud Messaging cấp) và gửi tới máy chủ PocketBase tự vận hành để gắn với tài khoản của bạn. **Chế độ Khách không bao giờ đăng ký mã thiết bị.**
- **Nội dung thông báo tối giản:** máy chủ chỉ gửi **mã loại thông báo** và **mã nội dung** (ví dụ mã bài học). Tên học sinh, điểm số, email, nội dung bài làm và câu trả lời **không bao giờ** đi qua Google FCM. Chữ hiển thị được dựng cục bộ trên máy.
- **Bên thứ ba:** việc vận chuyển thông báo do **Google Firebase Cloud Messaging (Google LLC)** thực hiện. Google có thể xử lý siêu dữ liệu kỹ thuật (địa chỉ IP, thông tin thiết bị, thời điểm gửi) theo chính sách của Google. `[CẦN LUẬT SƯ: đánh giá chuyển dữ liệu ra nước ngoài và dữ liệu trẻ em dưới 16 tuổi]`
- **Quyền kiểm soát:** bạn có thể tắt bất cứ lúc nào trong Cài đặt; khi tắt, đăng xuất hoặc xóa tài khoản, ứng dụng báo máy chủ xóa mã thiết bị. Mã không còn hợp lệ cũng được máy chủ tự động dọn khi gửi thất bại.
- **Nhắc học cục bộ** (không cần mạng, không cần tài khoản) vẫn hoạt động độc lập và không liên quan tới mã thiết bị.

## 3. Nơi lưu trữ, Bảo mật & Thời gian lưu giữ
- **Nơi lưu trữ:** Dữ liệu được lưu cục bộ trong cơ sở dữ liệu Room trên thiết bị và đồng bộ qua kết nối **HTTPS (TLS)** tới **máy chủ PocketBase trên máy Linux do nhà phát triển tự quản lý** tại: **`[TÔI CẦN ĐIỀN: tên miền / khu vực đặt máy chủ]`**.
- **Bảo mật phiên trên thiết bị:** Ứng dụng **không bao giờ lưu mật khẩu thô** trên máy. Token phiên đăng nhập (JWT) được mã hóa bằng **Android Keystore (AES-256-GCM)**.
- **Thời gian lưu giữ:** Dữ liệu được lưu giữ trong suốt thời gian tài khoản còn hoạt động cho đến khi bạn tự xóa trong ứng dụng hoặc gửi yêu cầu xóa qua email (hoặc sau **`[TÔI CẦN ĐIỀN: ví dụ 12 tháng]`** không hoạt động). Bản sao lưu mã hóa định kỳ (`restic`) tự động hết hạn và bị ghi đè sau tối đa **`[TÔI CẦN ĐIỀN: ví dụ 30 ngày]`**.

## 4. Quy trình xử lý sự cố rò rỉ dữ liệu `[CẦN THAM VẤN PHÁP LÝ]`
Trong trường hợp phát hiện sự cố truy cập trái phép hoặc rò rỉ dữ liệu cá nhân:
1. Cô lập máy chủ bị ảnh hưởng, thu hồi toàn bộ token phiên hoạt động và khắc phục lỗ hổng.
2. Đánh giá phạm vi bản ghi bị ảnh hưởng và lưu nhật ký kiểm tra.
3. Thông báo trực tiếp qua email cho học sinh/phụ huynh bị ảnh hưởng và gửi thông báo tới cơ quan chức năng có thẩm quyền theo thời hạn **72 giờ** kể từ khi xảy ra vi phạm theo quy định của Nghị định 13/2023/NĐ-CP.

## 5. Quyền của học sinh và phụ huynh
- **Quyền truy cập & xuất dữ liệu:** Xem trực tiếp trong app hoặc xuất sao lưu cục bộ / báo cáo PDF.
- **Quyền chỉnh sửa:** Cập nhật biệt danh, khối lớp, thông tin phụ huynh trong màn hình Tài khoản.
- **Quyền xóa vĩnh viễn:** Bấm **"Xóa tài khoản & Dữ liệu máy chủ"** (xác nhận 2 bước) ngay trong ứng dụng để xóa vĩnh viễn tài khoản và toàn bộ dữ liệu đồng bộ trên PocketBase; người dùng tự chọn giữ lại hoặc xóa luôn dữ liệu trên thiết bị.

## 6. Liên hệ
- Email hỗ trợ quyền riêng tư & yêu cầu xóa dữ liệu: **`[TÔI CẦN ĐIỀN: hotro@tenmiencuatoi.vn]`**
- Trang web hướng dẫn xóa dữ liệu: **`https://[TÔI CẦN ĐIỀN: api.tenmiencuatoi.vn]/delete-account.html`**
