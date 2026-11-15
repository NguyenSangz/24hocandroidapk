# F20a – Worker gửi thông báo đẩy (FCM HTTP v1)

Thư mục này chứa worker Node **không phụ thuộc thư viện ngoài** để gửi thông báo
đẩy từ hàng đợi `push_outbox` của PocketBase qua **Firebase Cloud Messaging HTTP v1**.

## 1. Vì sao không dùng FCM legacy key

- FCM legacy server key đã bị Google khai tử. HTTP v1 dùng **service account** ký JWT
  RS256 và đổi lấy access token ngắn hạn.
- **Không đặt service account trong repo.** Chỉ đặt trên máy chủ với `chmod 600`.
- App chỉ chứa `google-services.json` (client ID công khai); khóa bí mật chỉ ở server.

## 2. Chuẩn bị (việc của bạn)

1. Vào **Firebase Console** → chọn project → ⚙️ *Project settings* → tab *Service accounts*.
2. Bấm **Generate new private key** → tải file JSON.
3. Đưa file lên máy chủ, ví dụ:
   ```bash
   sudo mv ~/Downloads/gen-lang-client-*.json /etc/pocketbase/fcm-service-account.json
   sudo chown pocketbase:pocketbase /etc/pocketbase/fcm-service-account.json
   sudo chmod 600 /etc/pocketbase/fcm-service-account.json
   ```
4. Thêm biến vào `/etc/pocketbase/pocketbase.env`:
   ```properties
   PUSH_ENQUEUE_SECRET=...   # chuỗi ngẫu nhiên >= 32 ký tự, KHÔNG commit
   ```
5. Tạo file cấu hình worker `/etc/pocketbase/push.env` từ mẫu `push.env.example`
   (chmod 600). Điền `PB_ADMIN_EMAIL`, `PB_ADMIN_PASSWORD` của tài khoản admin PocketBase.

## 3. Chạy thử một lượt

```bash
cd /opt/hoc-tot-thcs/server/push
node --env-file=/etc/pocketbase/push.env fcm_sender.mjs
```

- Outbox trống → log `Không có thông báo chờ gửi.`
- Gửi thành công → bản ghi `push_outbox.status = "sent"`, `sentAt` được đặt.
- Token chết (404/UNREGISTERED) → `status = "failed"`, xóa dòng `device_tokens` tương ứng.
- Lỗi tạm (429/5xx) → `attempts + 1`, `scheduledAt` lùi theo backoff 30s → 60s → 2m → … tối đa 1h.
- Sai service account/quyền (401/403) → log lỗi xác thực và dừng lượt, không đánh dấu sai.

## 4. Chạy định kỳ bằng systemd timer

Có sẵn 2 file mẫu:

- `../systemd/push-sender.service`
- `../systemd/push-sender.timer`

Cài đặt:

```bash
sudo cp server/systemd/push-sender.service /etc/systemd/system/
sudo cp server/systemd/push-sender.timer /etc/systemd/system/
# Sửa WorkingDirectory/User nếu thư mục code khác /opt/hoc-tot-thcs
sudo systemctl daemon-reload
sudo systemctl enable --now push-sender.timer
sudo systemctl list-timers push-sender.timer
journalctl -u push-sender.service -n 50 --no-pager
```

Timer mặc định chạy mỗi phút. Worker tự giới hạn `MAX_PER_RUN` (mặc định 50)
và giãn cách `SEND_INTERVAL_MS` (mặc định 50ms ≈ 20 tin/giây) để tránh dồn tải.

## 5. Đưa thông báo vào hàng đợi (chỉ server/ops)

Endpoint `POST /api/custom/push/enqueue` được bảo vệ bằng header
`X-Push-Admin-Secret`. Ứng dụng **không** gọi endpoint này.

```bash
curl -sS -X POST "https://api.tenmiencuatoi.vn/api/custom/push/enqueue" \
  -H "Content-Type: application/json" \
  -H "X-Push-Admin-Secret: $PUSH_ENQUEUE_SECRET" \
  -d '{
    "userId": "USER_RECORD_ID",
    "type": "study_reminder",
    "contentId": "lesson_toan_6_bai_1",
    "dedupeKey": "reminder-2026-11-13",
    "scheduledAt": 0
  }'
```

- `type` chỉ nhận: `study_reminder`, `assignment_due`, `weekly_report`, `new_content`, `system`.
- `contentId` chỉ chữ/số/`_`/`.`/`-`, tối đa 128 ký tự.
- `dedupeKey` (tùy chọn) chống gửi trùng; server tự ghép thêm 12 ký tự cuối token.
- **Payload FCM chỉ có `type` + `content_id`.** Không gửi tên, điểm, email hay nội dung bài làm.

## 6. Kiểm thử

```bash
node --test server/push
```

Bộ test kiểm tra: chữ ký JWT RS256, đổi access token, whitelist payload,
phân loại lỗi FCM (token chết / retry / auth), backoff, rate limiter và
rate limit đăng ký token phía hook.

> **CHƯA KIỂM CHỨNG ĐƯỢC TỰ ĐỘNG:** việc gửi thật tới thiết bị cần service account
> thật + thiết bị thật. Ghi vào `docs/dev/MANUAL_TESTS.md`.

## 7. Bảo mật & pháp lý

- Không log token, không log payload chứa dữ liệu cá nhân (payload vốn đã tối giản).
- Service account và `push.env` phải `chmod 600`, `chown pocketbase:pocketbase`.
- Dùng FCM với học sinh dưới 16 tuổi và chuyển dữ liệu ra nước ngoài:
  **CẦN LUẬT SƯ** – xem `docs/dev/LEGAL_FLAGS.md`.
