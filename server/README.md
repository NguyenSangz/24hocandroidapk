# Server PocketBase Self-Hosted (F16a) – Học Tốt THCS

Thư mục `/server` chứa toàn bộ cấu hình, migration và hook cho backend **PocketBase tự host trên máy chủ Linux** của bạn.

---

## 1. Thông tin hạ tầng cần điền (`[TÔI CẦN ĐIỀN]`)

| Tham số | Giá trị hiện tại / Placeholder | Ghi chú |
|---|---|---|
| **Domain API** | `[TÔI CẦN ĐIỀN: ví dụ api.tenmiencuatoi.vn]` | Trỏ DNS về IP máy chủ hoặc Cloudflare Tunnel |
| **Truy cập Internet** | `[TÔI CẦN ĐIỀN: Caddy + IP công cộng HOẶC Cloudflare Tunnel]` | Chọn 1 trong 2 file mẫu ở mục 4 |
| **Nhà cung cấp SMTP** | `[TÔI CẦN ĐIỀN: Brevo / Resend / SMTP host]` | Dùng cho chức năng *Quên mật khẩu* và xác nhận phụ huynh |
| **Hệ điều hành Linux** | `[TÔI CẦN ĐIỀN: ví dụ Ubuntu 24.04 LTS]` | Kiến trúc `amd64` hoặc `arm64` |
| **Cấu hình RAM / Ổ đĩa** | `[TÔI CẦN ĐIỀN: ví dụ 2 GB RAM / 40 GB SSD]` | PocketBase dùng ~50–150 MB RAM ở tải nhẹ |
| **Google Web Client ID** | `[TÔI CẦN ĐIỀN: xxx.apps.googleusercontent.com]` | Loại *Web application* trong Google Cloud Console (dùng chung cho backend & Android Credential Manager) |
| **Email hỗ trợ pháp lý** | `[TÔI CẦN ĐIỀN: hotro@tenmiencuatoi.vn]` | Hiển thị trên trang Chính sách & Xóa dữ liệu |

---

## 2. Cài đặt PocketBase trên Linux (Không chạy bằng root)

> Khuyến nghị phiên bản **PocketBase >= v0.25.x** (tương thích JSVM `pb_hooks` và `pb_migrations` thế hệ v0.23+).

```bash
# 1. Tạo user hệ thống riêng không có shell đăng nhập
sudo useradd --system --no-create-home --shell /usr/sbin/nologin pocketbase

# 2. Tạo thư mục ứng dụng & dữ liệu tách biệt
sudo mkdir -p /opt/pocketbase/pb_hooks /opt/pocketbase/pb_migrations /opt/pocketbase/pb_public
sudo mkdir -p /var/lib/pocketbase /etc/pocketbase

# 3. Tải PocketBase Linux (thay v0.25.8 và kiến trúc amd64/arm64 nếu cần)
cd /tmp
PB_VER="0.25.8"
wget "https://github.com/pocketbase/pocketbase/releases/download/v${PB_VER}/pocketbase_${PB_VER}_linux_amd64.zip"
wget "https://github.com/pocketbase/pocketbase/releases/download/v${PB_VER}/checksums.txt"
sha256sum -c checksums.txt --ignore-missing
unzip -o "pocketbase_${PB_VER}_linux_amd64.zip" pocketbase -d /opt/pocketbase/
sudo chmod 755 /opt/pocketbase/pocketbase

# 4. Copy migrations, hooks và trang web công khai từ thư mục /server của repo
sudo cp server/pb_migrations/*.js /opt/pocketbase/pb_migrations/
sudo cp server/pb_hooks/*.js /opt/pocketbase/pb_hooks/
sudo cp server/web/*.html /opt/pocketbase/pb_public/

# 5. Tạo file biến môi trường bảo mật
sudo cp server/pocketbase.env.example /etc/pocketbase/pocketbase.env
sudo nano /etc/pocketbase/pocketbase.env   # Điền GOOGLE_WEB_CLIENT_ID
sudo chmod 600 /etc/pocketbase/pocketbase.env

# 6. Phân quyền sở hữu cho user pocketbase
sudo chown -R pocketbase:pocketbase /opt/pocketbase /var/lib/pocketbase /etc/pocketbase
```

---

## 3. Chạy PocketBase bằng `systemd`

```bash
sudo cp server/systemd/pocketbase.service /etc/systemd/system/pocketbase.service
sudo systemctl daemon-reload
sudo systemctl enable --now pocketbase
sudo systemctl status pocketbase
```

Tạo tài khoản Superuser (Admin) đầu tiên:
```bash
sudo -u pocketbase /opt/pocketbase/pocketbase superuser upsert "[TÔI CẦN ĐIỀN: admin@tenmiencuatoi.vn]" "[MẬT_KHẨU_MẠNH]" --dir=/var/lib/pocketbase
```
*(Mở Admin UI qua SSH tunnel an toàn: `ssh -L 8090:127.0.0.1:8090 user@server`, rồi mở `http://127.0.0.1:8090/_/` trên máy cá nhân — tuyệt đối không mở cổng `8090` ra Internet).*

---

## 4. Public qua HTTPS (Chọn 1 trong 2)

### Phương án A: Caddy + IP công cộng (Let's Encrypt tự động)
1. Trỏ bản ghi DNS `A` của `[TÔI CẦN ĐIỀN: api.tenmiencuatoi.vn]` về IPv4 máy chủ.
2. Mở cổng `80/tcp` và `443/tcp` (không mở `8090`).
3. Cài Caddy, sửa domain trong `server/caddy/Caddyfile` và copy vào `/etc/caddy/Caddyfile`, sau đó `sudo systemctl reload caddy`.

### Phương án B: Cloudflare Tunnel (Không cần mở port / không cần IP tĩnh)
1. Cài `cloudflared`, chạy `cloudflared tunnel login` và `cloudflared tunnel create hoctot-pb`.
2. Cập nhật tunnel ID và domain trong `server/cloudflare/cloudflared-config.yml`.
3. Chạy dịch vụ `cloudflared service install` và bật **Always Use HTTPS** trên Cloudflare Dashboard.

---

## 5. Cấu hình SMTP (Quên mật khẩu)
Trong PocketBase Admin UI (`/_/`) → **Settings → Mail settings**:
- Bật **Use SMTP mail server**.
- Điền thông tin của `[TÔI CẦN ĐIỀN: Brevo / Resend]`:
  - **Brevo**: Host `smtp-relay.brevo.com`, Port `587`, TLS `Auto`, Username = email đăng ký Brevo, Password = SMTP Key.
  - **Resend**: Host `smtp.resend.com`, Port `465` (TLS), Username `resend`, Password = API Key.
- Nhấn **Send test email** để kiểm tra trước khi đưa vào dùng thật.

SMTP cũng gửi liên kết xác nhận phụ huynh. Cấu hình `PB_PUBLIC_URL` trong
`/etc/pocketbase/pocketbase.env` thành URL công khai HTTPS gốc (không có dấu `/`
cuối), rồi khởi động lại PocketBase sau khi cài hook và trang
`parental-consent.html`. Liên kết dùng một lần hết hạn sau 1 giờ; các yêu cầu hết
hạn được xóa định kỳ. Trước khi phát hành, kiểm tra SMTP thật, email đến đúng địa
chỉ, hết hạn/tái sử dụng token và trạng thái tài khoản sau xác nhận.

---

## 6. Cấu hình phía Android & Certificate Pinning (Kèm kế hoạch xoay khóa)

1. Trên máy build Android cá nhân, khai báo trong `.env` (hoặc `local.properties` trên máy bạn — đã nằm trong `.gitignore`, KHÔNG commit):
   ```properties
   POCKETBASE_BASE_URL=https://[TÔI CẦN ĐIỀN: api.tenmiencuatoi.vn]
   GOOGLE_WEB_CLIENT_ID=[TÔI CẦN ĐIỀN: xxx.apps.googleusercontent.com]
   ```
2. **(Tùy chọn) Bật Certificate Pinning + Kế hoạch xoay khóa**:
   Lấy mã pin SPKI SHA-256 của chứng chỉ đang dùng **và** chứng chỉ gốc/dự phòng (ví dụ Let's Encrypt E5/R10/ISRG Root X1):
   ```bash
   openssl s_client -servername api.tenmiencuatoi.vn -connect api.tenmiencuatoi.vn:443 </dev/null 2>/dev/null \
     | openssl x509 -pubkey -noout \
     | openssl pkey -pubin -outform der \
     | openssl dgst -sha256 -binary | openssl enc -base64
   ```
   Điền vào `.env`:
   ```properties
   POCKETBASE_CERT_PIN_PRIMARY=sha256/<PIN_CHINH>
   POCKETBASE_CERT_PIN_BACKUP=sha256/<PIN_DU_PHONG_ROOT_HOAC_CERT_MOI>
   ```
   **Kế hoạch xoay khóa (Key Rotation Plan):**
   - Luôn ghim **ít nhất 2 pin** (`PRIMARY` = Intermediate/Leaf hiện tại, `BACKUP` = Root CA hoặc khóa dự phòng thế hệ tiếp theo).
   - Trước khi gia hạn/đổi CA ít nhất 30 ngày: phát hành bản cập nhật app đưa pin mới vào `BACKUP`, sau khi đa số người dùng cập nhật mới kích hoạt chứng chỉ mới trên Caddy/Cloudflare.

---

## 7. CHECKLIST TÔI CẦN TỰ LÀM / KIỂM TRA BẰNG TAY (F16a)

- [ ] **1. Điền đầy đủ thông tin hạ tầng**: thay tất cả `[TÔI CẦN ĐIỀN]` trong `/etc/pocketbase/pocketbase.env`, `Caddyfile` (hoặc `cloudflared-config.yml`), `.env` (hoặc `local.properties` trên máy dev).
- [ ] **2. Kiểm tra dịch vụ systemd chạy non-root**:
  - `ps aux | grep pocketbase` -> user phải là `pocketbase` (không phải `root`).
  - `ss -tulpn | grep 8090` -> chỉ lắng nghe `127.0.0.1:8090`.
- [ ] **3. Kiểm tra HTTPS & trang web pháp lý**:
  - Mở `https://[DOMAIN]/api/health` -> trả về `{"code":200,"message":"API is healthy.",...}`.
  - Mở `https://[DOMAIN]/delete-account.html` và `https://[DOMAIN]/privacy.html` -> hiển thị đúng tiếng Việt có dấu.
- [ ] **4. Kiểm tra Google Cloud Console**:
  - Đã tạo **OAuth 2.0 Client ID (Web application)** -> dán vào `GOOGLE_WEB_CLIENT_ID` ở cả server và app.
  - Đã tạo **OAuth 2.0 Client ID (Android)** với đúng `package name` (`com.aistudio.applet.ofedqc` hoặc package phát hành của bạn) và **SHA-1** của keystore ký APK (debug & release).
- [ ] **5. Kiểm tra luồng Auth thật trên thiết bị/mạng thật**:
  - Đăng ký Email + Mật khẩu → kiểm tra bản ghi xuất hiện trong bảng `users` ở Admin UI.
  - Bấm "Quên mật khẩu" → kiểm tra nhận được email khôi phục qua SMTP đã cấu hình.
  - Đăng nhập Google trên Android → kiểm tra `/api/custom/google-signin` trả `200 OK`, tạo user `authProvider="GOOGLE"`.
  - Nhập năm sinh `< 16` tuổi (ví dụ `2013`) → hoàn thành cổng UI, nhưng đồng bộ vẫn bị chặn cho đến khi phụ huynh mở liên kết email; kiểm tra liên kết dùng một lần, hết hạn sau 1 giờ, và `parentConsentAt`/`parentEmail` chỉ được lưu sau xác nhận.
  - Nhấn Xóa tài khoản 2 bước → kiểm tra user biến mất khỏi bảng `users` trong PocketBase Admin UI.

---

## 8. THÔNG BÁO ĐẨY F20a (tùy chọn – mặc định TẮT)

- **Migration:** `pb_migrations/1700000004_f20a_push_infra.js` tạo `device_tokens` và `push_outbox`.
- **Hook:** `pb_hooks/f20a_push.pb.js` cung cấp:
  - `POST /api/custom/push/register` (bắt buộc đăng nhập, rate limit 30 lượt/5 phút/tài khoản).
  - `POST /api/custom/push/unregister` (chỉ xóa token của chính tài khoản).
  - `POST /api/custom/push/enqueue` (chỉ server/ops; bảo vệ bằng `PUSH_ENQUEUE_SECRET`).
- **Worker gửi FCM HTTP v1:** `server/push/fcm_sender.mjs` (Node ≥ 18, không phụ thuộc thư viện).
  Xem hướng dẫn đầy đủ tại `server/push/README.md`.
- **Thêm biến môi trường** vào `/etc/pocketbase/pocketbase.env` (chmod 600):
  ```properties
  PUSH_ENQUEUE_SECRET=[TÔI CẦN ĐIỀN: chuỗi ngẫu nhiên >= 32 ký tự, ví dụ openssl rand -hex 32]
  ```
- **Service account FCM** đặt NGOÀI repo: `/etc/pocketbase/fcm-service-account.json` (chmod 600, chown pocketbase).
- **Bật tính năng trong app:** thêm vào `.env`/`local.properties` khi build:
  ```properties
  ENABLE_PUSH=true
  ```
  Khi `ENABLE_PUSH=false` (mặc định), app hoạt động y như trước và **không** xin quyền, không đăng ký token.

### Checklist tay cho F20a
- [ ] Tạo Firebase service account (Firebase Console → Project settings → Service accounts → Generate new private key), tải JSON, đặt ngoài repo, `chmod 600`.
- [ ] Cấu hình `server/push/push.env` và chạy thử `node server/push/fcm_sender.mjs` khi outbox trống → log “Không có thông báo chờ gửi.”
- [ ] Enqueue thử bằng `curl` với `X-Push-Admin-Secret`, kiểm tra worker gửi thành công và bản ghi `push_outbox.status = sent`.
- [ ] Gỡ app/đổi token (xóa dữ liệu Firebase) rồi enqueue lại → worker phải đánh dấu `failed` và xóa dòng `device_tokens` (mục “token hết hạn”).
- [ ] Trên điện thoại: bật thông báo → kiểm tra bảng `device_tokens` có token; tắt thông báo/đăng xuất → token biến mất.
- [ ] Máy không có Google Play Services → mục “Thông báo đẩy” phải ẩn hoàn toàn, app không lỗi.
- [ ] Từ chối quyền thông báo → app tắt công tắc, hiện hướng dẫn mở Cài đặt hệ thống.

> **PHÁP LÝ:** Việc dùng Google FCM với người dùng dưới 16 tuổi và chuyển dữ liệu ra nước ngoài cần luật sư rà soát. Xem `docs/dev/LEGAL_FLAGS.md` và `docs/play_data_safety_draft.md`.

## 9. Xác nhận phụ huynh qua email (P2.5 – cần triển khai)

- Migration `1700000005_parental_consent_verification.js` tạo collection challenge riêng
  không có API rules cho client; khi migration chạy, trạng thái đồng ý cũ của tài khoản
  dưới 16 tuổi/không rõ tuổi được đặt lại để xác nhận lại.
- Hook gửi liên kết token dùng một lần, hết hạn sau 1 giờ. Token và email nằm trong
  challenge tạm; xác nhận xóa challenge. Cron xóa các challenge hết hạn mỗi 30 phút.
- `PB_PUBLIC_URL` phải là origin HTTPS công khai và phải phục vụ
  `parental-consent.html`. SMTP sender và SMTP server phải hoạt động.
- Push/pull sync và đăng ký FCM bị chặn ở server với tài khoản chưa xác nhận; app cũng
  giữ outbox cục bộ.
- **Chưa triển khai/xác minh:** cần backup trước migration, kiểm tra quyền truy cập
  API, xác nhận `PB_PUBLIC_URL`, SMTP, HTTPS và luồng thật trên server. Xem
  `docs/dev/BLOCKERS.md` và `docs/dev/LEGAL_FLAGS.md`.
