# ARCHITECTURE – Kiến trúc hiện tại (tóm tắt)

## 1. Tầng app (Android, Kotlin + Compose)

```
UI (Compose screens)
  └─ ViewModel (SettingsViewModel, ...)
       └─ Repository interface (domain/repository)
            ├─ Local: Room DAO + DataStore (UserPreferencesManager)
            └─ Remote: Retrofit + OkHttp (PocketBase)
```

- **Offline-first:** Room là nguồn dữ liệu UI đọc; mạng chỉ là lớp đồng bộ phía sau.
- **Chế độ Khách mặc định:** toàn bộ tính năng học tập chạy không cần tài khoản.
- **DI thủ công:** `AppContainer` / `DefaultAppContainer` khởi tạo lazy các repository,
  `AppViewModelProvider` truyền dependency vào ViewModel.
- **Clock/Repository/Theme dùng chung:** không tạo lớp thời gian riêng ở F20a.

## 2. Xác nhận phụ huynh P2.5 (chưa triển khai production)

```
ParentalConsentScreen
  └─ AccountViewModel / AuthRepository
       └─ POST /parental-consent/request (email challenge, chưa cấp consent)
            └─ email → parental-consent.html (nút xác nhận chủ động)
                 └─ POST /parental-consent/confirm
                      └─ cập nhật users + xóa challenge trong transaction
```

- UI checkbox/câu hỏi không phải bằng chứng đồng ý. Chỉ phản hồi server xác nhận mới mở sync.
- `SyncRepositoryImpl` chặn đồng bộ khi account cần xác nhận; PocketBase push/pull và đăng ký
  FCM cũng chặn ở server. Local outbox vẫn được giữ để sync sau.
- Challenge tạm giữ email/token tối đa 1 giờ; collection không truy cập qua client API rules;
  token xóa khi xác nhận hoặc cleanup cron. Token hiện lưu rõ trong DB, cần hardening.
- Migration đặt lại consent của tài khoản trẻ cũ/không có năm sinh; cần backup và triển khai có
  kiểm soát. Năm sinh tự khai không phải xác minh tuổi.

## 3. Luồng thông báo đẩy F20a

```
SettingsScreen (công tắc + xin quyền)
  └─ SettingsViewModel (PushSettingsController)
       └─ PushTokenManager
            ├─ AuthRepository.accountState  (đăng nhập/đăng xuất)
            ├─ SettingsRepository.pushEnabled (bật/tắt)
            └─ PushTokenRepository
                 ├─ PushLocalStore (DataStore: token + thời điểm)
                 ├─ FcmGateway (FirebaseMessaging, ẩn sau interface)
                 └─ PushTokenRemoteDataSource (Retrofit /api/custom/push/*)
```

- `PocketBaseAuthRepository` gọi `onSessionEnding` **trước** khi xóa phiên để token được xóa trên server.
- `THCSFirebaseMessagingService` nhận token mới + payload; F20a chỉ xác thực payload,
  F20b sẽ dựng chữ cục bộ và hiển thị theo kênh.
- Payload chỉ có `type` + `content_id`; parser whitelist và loại bỏ mọi khóa khác.

## 4. Server (PocketBase + worker Node)

```
App ──HTTPS──> PocketBase
                ├─ users, sync_* (F16b)
                ├─ device_tokens (F20a)
                └─ push_outbox (F20a)
                       ▲
                       │ admin API
                push-sender (Node, systemd timer)
                       │ HTTPS (JWT RS256)
                       ▼
                Google FCM HTTP v1 ──> thiết bị
```

- `device_tokens`: unique `token`, relation `userId` cascade delete, `updatedAt` autodate.
- `push_outbox`: chỉ admin/worker; `dedupeKey` unique; trạng thái `pending/sent/failed`.
- Endpoint app: `POST /api/custom/push/register|unregister` (bắt buộc đăng nhập, rate limit).
- Endpoint ops: `POST /api/custom/push/enqueue` (header `X-Push-Admin-Secret`).
- Worker: không phụ thuộc npm; xử lý token chết, backoff 429/5xx, giới hạn tốc độ gửi.

## 5. Cờ tính năng

| Cờ | Mặc định | Tác dụng |
|---|---|---|
| `ENABLE_PUSH` | false | Bật UI + đăng ký token FCM |
| `ENABLE_AI_TUTOR` | false | Dành cho F20c (chưa triển khai) |
| `ENABLE_VIDEO` | false | P4 (chưa triển khai) |
| `ENABLE_PARENT` | false | P8 (chưa triển khai) |
| `ENABLE_CLASSES` | false | P9 (chưa triển khai) |

## 6. Nguyên tắc bất biến đang giữ

1. Không quảng cáo, không SDK theo dõi.
2. Không bí mật trong repo (service account ngoài repo, `.env` gitignore).
3. Mọi lớp mạng sau interface, test bằng bản giả.
4. Không phá vỡ F01–F15; thay đổi nhỏ có lý do.
5. Điểm pháp lý đánh dấu rõ, không tự khẳng định tuân thủ.
