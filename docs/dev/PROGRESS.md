# PROGRESS – Học Tốt THCS

> Cập nhật: 2026-11-15. Mỗi dòng chỉ ghi **DONE** khi có bằng chứng chạy thật.
> Cột "Bằng chứng" ghi lệnh/test/log cụ thể, không ghi lời khẳng định suông.

## 0. Đường cơ sở Bước 0 (baseline)

| Hạng mục | Lệnh | Kết quả thật | Ghi chú |
|---|---|---|---|
| Unit test toàn bộ | `.\gradlew.bat testDebugUnitTest --console=plain` (JAVA_HOME = Android Studio JBR) | **BUILD SUCCESSFUL in 2m 32s**, toàn bộ test hiện có PASSED | Chạy ngày 2026-11-13, trước khi thêm F20a |
| APK/AAB baseline | – | **CHƯA ĐO** | Sẽ đo ở P6 (hiệu năng/kích thước) |
| Lint baseline | – | **CHƯA CHẠY** | Sẽ chạy ở P7 |
| Khởi động nguội | – | **CHƯA ĐO** | Cần máy thật/macrobenchmark (P6) |

**Tóm tắt trạng thái 5 dòng**
1. App đã có F01–F15 (học offline, luyện tập, kiểm tra, flashcard, gamification, thống kê, nhắc học, tìm kiếm, công cụ, cài đặt, sao lưu, báo cáo PDF, pháp lý).
2. F16a (tài khoản PocketBase + Google Sign-In + xóa tài khoản) và F16b (đồng bộ outbox/conflict) đã có code + test tích hợp.
3. F19 có migration/hook lớp học nhưng chưa có UI đầy đủ; F20 AI Tutor mới là bản mô phỏng cục bộ, chưa gọi server.
4. F20a (FCM tối thiểu) vừa được triển khai trong phiên này, mặc định TẮT bằng `ENABLE_PUSH=false`.
5. Chưa có video online (P4), in-app update (P5), đo hiệu năng (P6), hoàn thiện phát hành (P7).

## 1. Bảng công việc theo mục 6

| ID | Tên | Trạng thái | Bằng chứng / ghi chú |
|---|---|---|---|
| P1.1 | Quản lý khóa/bí mật + quét bí mật | MỘT PHẦN | Có `.gitignore`, `local.properties`, BuildConfig, Secrets Gradle Plugin; **chưa có script quét bí mật** |
| P1.2 | Network security config | DONE | `network_security_config.xml` chặn cleartext; test `httpsEnforcedForNonLocalUrls` PASSED |
| P1.3 | /server README + backup + giám sát | MỘT PHẦN | README/systemd/restic/Uptime Kuma có sẵn; việc triển khai thật là của người dùng (BLOCKERS) |
| P1.4 | Migration users + API rules + rate limit | DONE | `1700000001_f16a_users_schema.js`, `AccountAuthUnitTest`, `PocketBaseAuthIntegrationTest` PASSED |
| P1.5 | Khung mạng app + test MockWebServer | DONE | `PocketBaseNetworkModule`, `PocketBaseAuthIntegrationTest` (timeout/401/429/5xx) PASSED |
| P2.1 | AccountEntity + migration 9→10 | DONE | `RoomMigration9To10Test` PASSED |
| P2.2 | AuthRepository email/Google/refresh | DONE | `AccountAuthUnitTest`, `PocketBaseAuthIntegrationTest` PASSED |
| P2.3 | Màn hình tài khoản | DONE | `AccountFlowComposeUiTest` PASSED |
| P2.4 | Nâng cấp Khách → tài khoản giữ dữ liệu | DONE | `upgradeFromGuestToEmail_preservesLocalRoomProgress...` PASSED |
| P2.5 | Trẻ dưới 16 + đồng ý phụ huynh | MỘT PHẦN | Thêm liên kết xác nhận email dùng một lần, migration reset tài khoản trẻ cũ, app/server chặn sync; **chưa chạy build/test hoặc triển khai PB, cách xác minh/hiệu lực pháp lý chờ luật sư** |
| P2.6 | Xóa tài khoản + dữ liệu server | DONE | `deleteAccountFlow_...`, hook xóa user + collection; bổ sung xóa `device_tokens`/`push_outbox` (F20a) |
| P2.7 | Pháp lý cuối P2 | MỘT PHẦN | Đã cập nhật privacy + Data safety; chờ luật sư/Play Console |
| P3.1–P3.5 | Đồng bộ outbox/delta/conflict | DONE | `PocketBaseSyncIntegrationTest`, `SyncConflictResolverTest` PASSED |
| P3.6 | Nhiều thiết bị + xem trước | MỘT PHẦN | Có test 2 thiết bị; **chưa thấy màn hình xem trước gộp lần đầu** |
| P3.7 | UX trạng thái đồng bộ | MỘT PHẦN | Có trạng thái trong code sync; chưa rà đủ UI/@Preview |
| P3.8 | F15b độc lập + restore qua outbox | DONE | Test backup/restore hiện có PASSED |
| P3.9 | Pháp lý cuối P3 | MỘT PHẦN | Privacy/Data safety đã có; chờ rà soát |
| P4.1–P4.5 | Video bài học online | TODO | Chưa có player/model video/credit/license |
| P5.1 | In-App Updates | TODO | Chưa có `AppUpdateManager`/Fake |
| P5.2–P5.3 | Content pack từ xa + ký | MỘT PHẦN | Có seeder/ContentVersion cục bộ; **chưa có tải manifest/checksum/chữ ký** |
| P6.1–P6.7 | Hiệu năng | TODO | Chưa có Macrobenchmark/Baseline Profile/đo trước-sau |
| P7.1–P7.8 | Chất lượng, vận hành, phát hành | TODO | Chưa chạy lint/ktlint, accessibility toàn diện, CI, Play Console |
| P8 | Liên kết phụ huynh | TODO | Cần `ENABLE_PARENT=true` + cho phép |
| P9 | Lớp học/giáo viên | MỘT PHẦN | Có migration + hook join-class; **chưa có UI/duyệt giáo viên/API rules đầy đủ** |
| P10/F20a | FCM tối thiểu | DONE (mã nguồn + test) | `:app:testDebugUnitTest` **52 suite/206 test PASSED**; `node --test server/push` **14/14 PASSED**; `:app:assembleDebug` **BUILD SUCCESSFUL**; gửi thật còn BLOCKED. Xem `docs/dev/reports/F20a.md` |
| P10/F20b | Hiển thị thông báo + kênh + giờ yên lặng | TODO | Chờ lệnh "tiếp" |
| P10/F20c | Gia sư AI qua server | TODO | Hiện chỉ là mô phỏng cục bộ |
| P10/F20d | Chi phí/ngân sách AI | TODO | Chờ F20c |

## 2. Việc đang làm (DOING)

- P2.5: siết xác nhận phụ huynh qua email. **Chưa hoàn tất quality gate; chưa triển khai migration lên server.**
- Kiểm tra môi trường lúc 2026-11-15: tìm thấy Android Studio JBR tại `C:\Program Files\Android\Android Studio\jbr`; Node có sẵn.
- Thêm `AccountAuthUnitTest.emailSignIn_doubleTapSubmitsOnlyOnce`; đã cố chạy cùng `PocketBaseSyncIntegrationTest`, nhưng Gradle dừng tại `:app:generateDebugRFile` vì không xóa được `app\build\intermediates\compile_r_class_jar\debug\generateDebugRFile\R.jar` (file đang bị khóa). Test **chưa chạy**.
- `node --check` các hook/migration và parse script trong trang xác nhận: PASSED ngày 2026-11-15.
- F20a đã dừng đúng điểm chờ lệnh; nội dung lịch sử dưới đây là bằng chứng từ 2026-11-13, không phải kết quả kiểm thử hiện tại.
- Ghi chú kiểm chứng: lần chạy toàn bộ đầu tiên sau F20a có 2 test flaky
  (`ReviewSessionViewModelTest` – pass khi chạy lại; `WrongBankFlowComposeUiTest` –
  do Robolectric không đẩy main looper trong `waitUntil`). Đã sửa **harness** của test
  UI (giữ nguyên assertion/timeout) và chạy lại: toàn bộ 206 test pass. Chi tiết ở
  `docs/dev/reports/F20a.md` mục 5.

## 3. Việc bị chặn (BLOCKED)

- Gửi FCM thật: cần service account + biến môi trường + thiết bị thật. Xem `BLOCKERS.md`.
- Luật sư rà soát FCM/trẻ em/chuyển dữ liệu xuyên biên giới. Xem `LEGAL_FLAGS.md`.
- P2.5 production: cần backup và triển khai migration/hook/trang HTML trên PocketBase thật; cấu hình HTTPS `PB_PUBLIC_URL` + SMTP và thử end-to-end.
- Chưa có kết quả Kotlin compile/unit test/assemble/lint hiện hành cho thay đổi P2.5; lỗi khóa `R.jar` cần xử lý trước khi chạy lại. Không dùng lại các kết quả F20a làm bằng chứng.
