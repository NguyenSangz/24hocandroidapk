# Server PocketBase (F16b) – Đồng bộ Offline-First, Sao lưu Restic & Bảo mật Linux

Tài liệu này bổ sung cho `server/README.md` (F16a), hoàn thiện tầng **đồng bộ đám mây 10 collection**, **sao lưu mã hóa off-site**, **làm cứng máy chủ Linux** và **giám sát uptime**.

---

## 1. Thông tin cần điền (`[TÔI CẦN ĐIỀN]`)

| Tham số | Placeholder | Ghi chú |
|---|---|---|
| **Kho lưu Restic off-site** | `[TÔI CẦN ĐIỀN: ví dụ b2:my-pb-backup:/hoctot hoặc s3:https://...]` | Backblaze B2 / Cloudflare R2 / AWS S3 / máy SFTP khác |
| **Mật khẩu mã hóa Restic** | `[TÔI CẦN ĐIỀN: mật_khẩu_mạnh_ít_nhất_24_ký_tự]` | Lưu giữ ở password manager cá nhân — mất khóa này sẽ không giải mã được backup! |
| **Khóa truy cập S3/B2 (nếu dùng)** | `[TÔI CẦN ĐIỀN: AWS_ACCESS_KEY_ID / B2_ACCOUNT_ID ...]` | Điền vào `/etc/pocketbase/restic.env` (`chmod 600`) |
| **Cổng SSH thực tế** | `[TÔI CẦN ĐIỀN: mặc định 22]` | Dùng trong `server/scripts/harden_linux.sh` |
| **Kênh nhận cảnh báo Uptime** | `[TÔI CẦN ĐIỀN: Telegram Bot / Email / Discord Webhook]` | Cấu hình trong Uptime Kuma |

---

## 2. Triển khai Migration & Hook đồng bộ F16b lên VPS

```bash
# 1. Chép migration và hook đồng bộ F16b
sudo cp server/pb_migrations/1700000002_f16b_sync_collections.js /opt/pocketbase/pb_migrations/
sudo cp server/pb_hooks/f16b_sync_hooks.pb.js /opt/pocketbase/pb_hooks/
sudo chown -R pocketbase:pocketbase /opt/pocketbase/pb_migrations /opt/pocketbase/pb_hooks

# 2. Khởi động lại PocketBase để áp dụng migration tạo 10 collection sync_*
sudo systemctl restart pocketbase
sudo journalctl -u pocketbase -n 50 --no-pager
```

### Kiến trúc đồng bộ phía server
- **10 collection**: `sync_lesson_progress`, `sync_attempts`, `sync_tests`, `sync_wrong_questions`, `sync_flashcards`, `sync_xp_events`, `sync_badges`, `sync_settings`, `sync_vocab_packs`, `sync_irregular_verbs`.
- **Chỉ mục chống trùng & Delta Cursor**:
  - Mọi collection đều có `UNIQUE INDEX (userId, entityKey)` và `INDEX (userId, updated)`.
  - `sync_xp_events` có thêm `UNIQUE INDEX (userId, sourceType, sourceId)` đảm bảo XP **không bao giờ bị cộng đúp** ngay cả khi client gửi lại nhiều lần.
  - Trường `updated` (`AutodateField`) do **server tự đặt**, làm con trỏ delta đáng tin cậy cho `/api/custom/sync/pull?since=...`.
- **Giới hạn an toàn & Rate limit**:
  - Tối đa **60 request / 5 phút / userId** (vượt quá trả `429 Too Many Requests`).
  - Tối đa **50 bản ghi / lô push**, mỗi payload JSON giới hạn **<= 16 KB** ở hook (và `32 KB` ở DB schema).

---

## 3. Sao lưu mã hóa tự động (Restic + SQLite `.backup`)

```bash
# 1. Cài đặt restic và sqlite3
sudo apt-get update && sudo apt-get install -y restic sqlite3

# 2. Tạo file biến môi trường bảo mật cho Restic
sudo tee /etc/pocketbase/restic.env >/dev/null <<'EOF'
RESTIC_REPOSITORY=[TÔI CẦN ĐIỀN: b2:ten-bucket:/pocketbase]
RESTIC_PASSWORD=[TÔI CẦN ĐIỀN: MAT_KHAU_MA_HOA_MANH]
# Nếu dùng Backblaze B2 hoặc Cloudflare R2/S3, điền thêm:
# B2_ACCOUNT_ID=[TÔI CẦN ĐIỀN]
# B2_ACCOUNT_KEY=[TÔI CẦN ĐIỀN]
# AWS_ACCESS_KEY_ID=[TÔI CẦN ĐIỀN]
# AWS_SECRET_ACCESS_KEY=[TÔI CẦN ĐIỀN]
EOF
sudo chmod 600 /etc/pocketbase/restic.env

# 3. Cài script & systemd timer chạy 03:15 mỗi ngày
sudo mkdir -p /opt/pocketbase/scripts /var/backups/pocketbase/staging
sudo cp server/scripts/restic_backup.sh server/scripts/restic_restore_test.sh /opt/pocketbase/scripts/
sudo chmod +x /opt/pocketbase/scripts/*.sh
sudo cp server/systemd/restic-backup.service server/systemd/restic-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now restic-backup.timer
```

---

## 4. Làm cứng Linux & Giám sát Uptime Kuma

```bash
# 1. Chạy script UFW + fail2ban + unattended-upgrades (lưu ý mở sẵn SSH key trước!)
sudo SSH_PORT=22 bash server/scripts/harden_linux.sh

# 2. Khởi chạy Uptime Kuma (chỉ mở localhost 127.0.0.1:3001)
cd server/monitoring
sudo docker compose -f docker-compose.uptime-kuma.yml up -d
```
Mở Uptime Kuma qua SSH tunnel: `ssh -L 3001:127.0.0.1:3001 user@server` → `http://127.0.0.1:3001` → thêm monitor HTTPS kiểm tra `https://[DOMAIN]/api/health` mỗi 60 giây + bật cảnh báo hết hạn chứng chỉ SSL (cảnh báo trước 14 ngày).

---

## 5. CHECKLIST TÔI CẦN TỰ LÀM / KIỂM TRA BẰNG TAY (F16b)

- [ ] **1. Kiểm tra 10 collection `sync_*` sau migration**: Mở PocketBase Admin UI (`/_/`) qua SSH tunnel và xác nhận cả 10 collection `sync_*` đã xuất hiện, `sync_xp_events` có unique index `(userId, sourceType, sourceId)`.
- [ ] **2. Kiểm tra tường lửa UFW & cổng 8090**:
  - Chạy `sudo ufw status verbose`: chỉ mở cổng SSH, `80`, `443`; cổng `8090` ở trạng thái `DENY IN`.
  - Kiểm tra fail2ban hoạt động: `sudo fail2ban-client status sshd`.
- [ ] **3. Chạy thử sao lưu & diễn tập khôi phục lần đầu**:
  - Chạy `sudo /opt/pocketbase/scripts/restic_backup.sh` → xác nhận tạo snapshot thành công.
  - Chạy `sudo /opt/pocketbase/scripts/restic_restore_test.sh` → kết quả phải in ra `PRAGMA integrity_check = ok`.
  - Lên lịch diễn tập khôi phục định kỳ mỗi tháng 1 lần.
- [ ] **4. Bật giám sát Uptime Kuma**: Thêm monitor cho `https://[DOMAIN]/api/health` và kiểm tra thử nhận cảnh báo qua Telegram/Email.
- [ ] **5. Kiểm thử đồng bộ 2 thiết bị thật (hoặc 2 máy ảo)**:
  - Thiết bị A hoàn thành 1 bài học + làm đúng vài câu hỏi + ôn 1 flashcard → bấm **Đồng bộ ngay** (hoặc chờ tự động) → kiểm tra bản ghi trên server.
  - Thiết bị B đăng nhập cùng tài khoản → kéo đồng bộ → xác nhận tiến độ bài học, XP (không bị cộng đúp) và thẻ flashcard khớp với Thiết bị A.
  - Bật chế độ Máy bay trên Thiết bị A, làm tiếp 1 bài kiểm tra (không lỗi, không chặn) → tắt chế độ Máy bay → kiểm tra dữ liệu tự đồng bộ lên server và hiển thị ở Thiết bị B.
  - Kiểm tra công tắc **Chỉ đồng bộ qua Wi-Fi**: bật công tắc, dùng mạng di động 4G → xác nhận app tạm hoãn sync và ghi rõ trạng thái chờ Wi-Fi.
