# Báo cáo tổng quan & Hướng phát triển — Học Tốt THCS

> Tài liệu tổng hợp từ việc đọc mã nguồn thực tế (đường dẫn file:line đều trích dẫn tương đối từ gốc repo `C:/Users/Admin/Desktop/24hoc adr`).
> Mọi khẳng định đều bám vào code đã đọc; những chỗ chưa chắc được đánh dấu **[cần xác minh]**.

---

## 1. Tổng quan dự án

**Mục đích:** Ứng dụng học & luyện tập dành cho học sinh THCS (lớp 6–9), theo **Chương trình GDPT 2018**. Thiết kế **offline-first**: mọi tính năng học tập đều chạy đầy đủ trên máy không cần mạng; tài khoản/đồng bộ là tùy chọn.

**Đối tượng:** Học sinh 11–15 tuổi, có cơ chế bảo vệ trẻ em dưới 16 tuổi (Parental Gate) theo Nghị định 13/2023/NĐ-CP và Google Play Families Policy (xem `app/src/main/java/com/example/ui/screens/account/AccountScreen.kt:851-921`).

**Stack công nghệ (version thật từ `gradle/libs.versions.toml`):**

| Thành phần | Phiên bản | Nguồn |
|---|---|---|
| Android Gradle Plugin | 9.1.1 | `gradle/libs.versions.toml:2` |
| Kotlin | 2.2.10 | `gradle/libs.versions.toml:11` |
| Compose BOM | 2024.09.00 | `gradle/libs.versions.toml:12` |
| Material 3 | (theo Compose BOM) | `app/build.gradle.kts:118` |
| Room | 2.7.0 (runtime/ktx/compiler) | `gradle/libs.versions.toml:15-17` |
| Navigation Compose | 2.8.9 | `gradle/libs.versions.toml:14` |
| Retrofit | 2.12.0 | `gradle/libs.versions.toml:22` |
| OkHttp | 4.10.0 | `gradle/libs.versions.toml:33` |
| Moshi | 1.15.2 (+ codegen) | `gradle/libs.versions.toml:34-35` |
| Firebase BOM | 34.17.0 | `gradle/libs.versions.toml:38` |
| Firebase AI (Gemini) | (theo BOM) — **đã khai báo nhưng chưa dùng**, xem mục 2 | `app/build.gradle.kts:132` |
| DataStore Preferences | 1.1.7 | `gradle/libs.versions.toml:36` |
| Coil | 2.7.0 | `gradle/libs.versions.toml:21` |
| Credentials / Google ID | 1.5.0 / 1.1.1 | `gradle/libs.versions.toml:41-42` |
| KSP | 2.3.5 | `gradle/libs.versions.toml:13` |
| Core library desugaring | 2.1.5 | `gradle/libs.versions.toml:43` |
| Robolectric (test) | 4.16.1 | `gradle/libs.versions.toml:37` |
| Gradle (wrapper) | 9.3.1 | `gradle/wrapper/gradle-wrapper.properties:3` |

**Build config (`app/build.gradle.kts`):**
- `applicationId = "com.aistudio.applet.ofedqc"`, `versionCode = 1`, `versionName = "1.0"`
- `minSdk = 23`, `targetSdk = 37`, `compileSdk = 37` (`app/build.gradle.kts:17-20`)
- Core library desugaring đã bật cho `java.time` trên minSdk 23 (`app/build.gradle.kts:74`)
- Secrets Gradle Plugin đọc `.env` / `.env.example` (`app/build.gradle.kts:96-100`), BuildConfig field `POCKETBASE_BASE_URL`, `GOOGLE_WEB_CLIENT_ID`, `POCKETBASE_CERT_PIN_PRIMARY/BACKUP` (`app/build.gradle.kts:37-40`)
- `network_security_config.xml` được tham chiếu trong manifest (`app/src/main/AndroidManifest.xml:17`)

**Kiến trúc tổng thể (UI → Domain → Data):**
- **UI** (`app/src/main/java/com/example/ui/`): Jetpack Compose + Material 3, Navigation Compose, ViewModel. 4 tab.bottom nav: Học tập / Luyện tập / Thống kê / Cá nhân (`ui/components/BottomNavBar.kt:30-41`).
- **Domain** (`app/src/main/java/com/example/domain/`): model, repository interface, evaluator (chấm câu, SM-2, thống kê), gamification (XP/Level/Streak/Badge), search query builder, tool registry.
- **Data** (`app/src/main/java/com/example/data/`): Room (`AppDatabase` v12, 22 entity, 16 DAO), remote PocketBase (Retrofit/OkHttp/Moshi), sync outbox, search FTS4 indexer, seed ContentLoader, KeystoreTokenStorage.
- **DI** (`app/src/main/java/com/example/AppContainer.kt`): DI thủ công qua interface `AppContainer` + `DefaultAppContainer`, không dùng Hilt/Dagger. `THCSApplication` nạp container (`THCSApplication.kt:18`).

**Backend PocketBase (self-hosted):** Nguồn trong `server/` — PocketBase ≥ v0.25.x trên Linux, qua Caddy hoặc Cloudflare Tunnel, systemd non-root, restic backup, uptime-kuma monitoring. 3 hook JS và 3 migration:
- `server/pb_migrations/1700000001_f16a_users_schema.js`
- `server/pb_migrations/1700000002_f16b_sync_collections.js`
- `server/pb_migrations/1700000003_f19_classes_and_assignments.js`
- `server/pb_hooks/f16a_auth_and_account.pb.js`, `f16b_sync_hooks.pb.js`, `f19_class_hooks.pb.js`
- Web pháp lý: `server/web/delete-account.html`, `server/web/privacy.html`
- Hướng dẫn triển khai: `server/README.md` (F16a) và `server/README_F16b.md`.

**API surface phía app (`PocketBaseApi.kt`):** đăng ký, auth-with-password, auth-refresh, request-password-reset, update user, google-signin (`api/custom/google-signin`), parental-consent, delete account (`api/custom/account`), sync push/pull (`api/custom/sync/push`, `/pull`) (`app/src/main/java/com/example/data/remote/pocketbase/PocketBaseApi.kt:15-64`).

---

## 2. Tính năng hiện có

Nhóm theo mảng nghiệp vụ. Trạng thái: ✅ hoàn chỉnh / ⚠️ dở hoặc placeholder / ❌ có sẵn backend-domain nhưng chưa wire UI.

### 2.1 Học tập & bài học

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Splash, nạp nội dung từ assets, retry khi lỗi | `ui/screens/splash/SplashScreen.kt`, `SplashViewModel.kt` | ✅ |
| Chọn lớp 6–9 lần đầu | `ui/screens/grade/SelectGradeScreen.kt` | ✅ |
| Tab Học tập (danh sách môn, ẩn môn chưa có nội dung) | `ui/screens/study/StudyScreen.kt` | ✅ |
| Danh sách chương theo môn | `ui/screens/study/ChapterListScreen.kt` | ✅ |
| Chi tiết bài học (summary/examples/tips) | `ui/screens/study/LessonDetailScreen.kt` | ✅ |
| Pipeline nội dung mở rộng qua gói JSON, version & upsert | `docs/content_pipeline.md`, `data/seed/ContentLoader*`, `ContentVersionEntity` | ✅ |

### 2.2 Luyện tập & kiểm tra

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Cấu hình phiên luyện tập (môn/chương/số câu 5-10-15/độ khó Dễ-Vừa-Khó-Hỗn hợp 4:4:2) | `ui/screens/practice/PracticeScreen.kt:144-373` | ✅ |
| Làm câu hỏi luyện tập + kết quả | `ui/screens/practice/PracticeQuizScreen.kt`, `PracticeResultScreen.kt` | ✅ |
| Đề kiểm tra tổng hợp 15 phút, bấm giờ, chấm thang 10 | `ui/screens/exam/ExamTakingScreen.kt` | ✅ |
| 6 loại câu hỏi: MC / True-False / FillBlank / Matching / Ordering / ShortAnswer | `ui/components/question/*` | ✅ |
| Ôn lại câu sai sau phiên | `ui/screens/wrongbank/ReviewSessionScreen.kt` | ✅ |
| Bộ màn hình kiểm tra cũ (TestConfig/Taking/Result/Review/History) | `ui/screens/test/*` | ⚠️ Tồn tại trong code nhưng **không** được `NavGraph.kt` đăng ký; dường như bị thay bởi `ExamTakingScreen` — **[cần xác minh]** có còn dùng hay nên xóa |

### 2.3 Flashcard & ôn tập ngắt quãng

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Học thẻ ghi nhớ theo SM-2 (4 nút Again/Hard/Good/Easy) | `ui/screens/flashcard/FlashcardStudyScreen.kt`, `domain/evaluator/SpacedRepetitionScheduler.kt` | ✅ |
| Ngân hàng câu sai, gom tự động, đến hạn ôn | `ui/screens/wrongbank/WrongBankScreen.kt` | ✅ |
| Lịch sử review log | `ReviewLogEntity`, migration 5→6 | ✅ |

### 2.4 Gamification

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Level + XP + thanh tiến trình cấp | `ui/screens/profile/ProfileScreen.kt:362-415`, `domain/gamification/LevelCalculator.kt`, `XpManager.kt` | ✅ |
| Streak (chuỗi ngày) + Streak Freeze (tối đa 2 lượt, mua bằng 200 XP) | `ProfileScreen.kt:419-522`, `StreakCalculator.kt` | ✅ |
| Lịch học 7 ngày gần nhất | `ProfileScreen.kt:527-602` | ✅ |
| Lưới huy hiệu + chi tiết huy hiệu | `ProfileScreen.kt:604-663`, `BadgeDetailDialog.kt`, `BadgeEvaluator.kt`; config `assets/gamification/badges.json` | ✅ |
| Lịch sử XP | `ui/screens/profile/XpHistoryScreen.kt` | ✅ |

### 2.5 Thống kê học tập

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Tab Thống kê (biểu đồ tổng quan) | `ui/screens/stats/StatsScreen.kt` | ✅ |
| Thống kê theo môn | `ui/screens/stats/SubjectStatsScreen.kt` | ✅ |
| Biểu đồ: thanh ngang độ chính xác môn, so sánh nhóm, tiến trình bài, đường điểm kiểm tra, cột học theo ngày | `ui/components/chart/*` | ✅ |
| Phân tích điểm mạnh/yếu, xu hướng, theo dõi thời gian học | `domain/evaluator/StrengthWeaknessAnalyzer.kt`, `TrendCalculator.kt`, `StudyTimeTracker.kt`, `StatsAggregator.kt` | ✅ |

### 2.6 Tìm kiếm

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Tìm toàn ứng dụng, FTS4 có dấu + không dấu | `ui/screens/search/SearchScreen.kt`, `data/search/SearchIndexer.kt`, `domain/search/SearchQueryBuilder.kt`, `TextNormalizer.kt` | ✅ |

### 2.7 AI (Gemini)

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Dependency `firebase-ai` | `app/build.gradle.kts:132` | ❌ **Đã khai báo nhưng không có code nào gọi Gemini/AI** trong toàn bộ `app/src/main/java` (đã grep case-insensitive `gemini`, `GenerativeModel`, `Firebase.ai`, `firebaseAI` → 0 kết quả). Chưa có màn hình/ViewModel/use case AI nào — **[cần xác minh]** kế hoạch sử dụng |

### 2.8 Tài khoản & đồng bộ dữ liệu

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Chế độ Khách mặc định, giữ dữ liệu máy | `AccountScreen.kt:344-553` | ✅ |
| Đăng ký/đăng nhập Email + mật khẩu | `AccountScreen.kt:386-509`, `PocketBaseAuthRepository.kt` | ✅ |
| Đăng nhập Google (Credential Manager) | `AccountScreen.kt:526-538`, `util/GoogleSignInHelper` | ✅ |
| Quên mật khẩu (SMTP) | `AccountScreen.kt:812-849` | ✅ |
| Parental Gate (<16t): phép tính + email phụ huynh + checkbox + timestamp máy chủ | `AccountScreen.kt:851-921` | ✅ |
| Xóa tài khoản 2 bước (giữ/xóa dữ liệu máy) | `AccountScreen.kt:923-1006` | ✅ |
| Đồng bộ đám mây: Sync now, Wi-Fi only, retry failed | `AccountScreen.kt:1009-1180`, `data/repository/SyncRepositoryImpl.kt` | ✅ |
| Outbox offline-first: batch 50, exponential backoff, delta cursor, conflict resolver | `SyncRepositoryImpl.kt:38-63`, `data/sync/SyncConflictResolver.kt`, `SyncOutboxEntity` | ✅ |
| Token mã hóa Android Keystore | `data/local/security/KeystoreTokenStorage.kt`, `TokenStorage.kt` | ✅ |
| Sanitizer log mạng | `data/remote/NetworkLogSanitizer.kt` | ✅ |

### 2.9 Cài đặt & pháp lý

| Tính năng | Màn hình / file | Trạng thái |
|---|---|---|
| Giao diện sáng/tối/hệ thống, cỡ chữ bài học, giảm chuyển động | `ui/screens/settings/SettingsScreen.kt:209-328` | ✅ |
| Chọn lớp 6–9, bộ sách (KNTT/CTST/Cánh Diều/Chuẩn chung) | `SettingsScreen.kt:330-381` | ✅ |
| Số câu mặc định, âm thanh, rung, tốc độ TTS tiếng Anh | `SettingsScreen.kt:384-487` | ✅ |
| Mục tiêu hàng ngày (số câu/phút), nhắc học, level-up dialog | `SettingsScreen.kt:490-612` | ✅ |
| Sao lưu / khôi phục / xuất PDF / giới thiệu | `SettingsScreen.kt:614-695` (callbacks `onNavigateToBackup/Restore/PdfReport/About`) | ⚠️ **Placeholder** — callback rỗng `{ /* Sẽ mở F15b khi chọn */ }` trong `NavGraph.kt:302-305` |
| Đặt lại cài đặt, xóa toàn bộ dữ liệu 2 bước | `SettingsScreen.kt:697-735`, 831-884 | ✅ |
| Chính sách quyền riêng tư | `ui/screens/legal/PrivacyPolicyScreen.kt`, `assets/legal/privacy_policy.md` | ✅ |
| Bản nháp Data Safety Play Console | `docs/play_data_safety_draft.md`, `server/legal/google_play_data_safety_draft.md` | ✅ (nháp) |

### 2.10 Thông báo cục bộ

| Tính năng | File | Trạng thái |
|---|---|---|
| Notification channel + reminder, test notification | `util/NotificationHelper.kt`, `THCSApplication.kt:39-51`, `SettingsViewModel.kt:188` | ✅ (chỉ local notification, **không có FCM push** — **[cần xác minh]** nhu cầu push) |

### 2.11 Công cụ học tập theo môn (F14)

| Tính năng | File | Trạng thái |
|---|---|---|
| Route khai báo: ToolsHub, ScientificCalculator, GraphPlotter, PeriodicTable, HistoryTimeline, GeoMap, EnglishDictionary, IrregularVerbs | `ui/navigation/Screen.kt:40-48` | ❌ **Route khai báo nhưng `NavGraph.kt` không đăng ký** → chưa vào được UI |
| Domain logic ToolRegistry + CalculatorParser | `domain/tool/ToolRegistry.kt`, `domain/tools/calculator/CalculatorParser.kt` | ✅ (có logic) |
| Asset công thức máy tính | `assets/tools/calculator/formulas.json`, `schema.json` | ✅ (có dữ liệu) |

### 2.12 Lớp học (F19)

| Tính năng | File | Trạng thái |
|---|---|---|
| Backend: join-class, classes, class_members, assignments | `server/pb_hooks/f19_class_hooks.pb.js`, `server/pb_migrations/1700000003_f19_classes_and_assignments.js` | ✅ (backend) |
| Room entity + DAO | `data/local/entity/ClassEntities.kt`, `data/local/dao/ClassDao.kt` | ⚠️ File tồn tại nhưng **chưa wire vào `AppDatabase`** (v12 không có bảng class, `AppDatabase.kt:50-77`), chưa có DAO method trong DB, chưa có screen |

---

## 3. Chất lượng kỹ thuật hiện tại

- **Lint:** Theo báo cáo được cung cấp, `app:lintDebug` chạy thành công với **0 error / 65 warning** (phần lớn là gợi ý nâng cấp dependency). **[cần xác minh]** vì tài liệu này không tự chạy `./gradlew lintDebug` (theo chỉ thị không build).
- **Core library desugaring:** Đã bật (`app/build.gradle.kts:74`), cho phép dùng `java.time` trên minSdk 23.
- **Room migrations:** Có 9 migration từ v3→v12, đều bảo toàn dữ liệu người dùng (`AppDatabase.kt:105-523`), có test cho từng bước (xem mục 4).
- **Backup/restore rule:** `allowBackup=false` (`AndroidManifest.xml:12`), có `data_extraction_rules.xml` và `backup_rules.xml`.
- **Mạng:** `network_security_config.xml` chặn cleartext, hỗ trợ certificate pinning qua BuildConfig pin.
- **65 warning còn lại:** cần rà soát theo từng nhóm (dependency nâng cấp, deprecated API, v.v.).

---

## 4. Điểm yếu / nợ kỹ thuật

| # | Vấn đề | Bằng chứng code | Tác động |
|---|---|---|---|
| 1 | **R8/minify đang tắt** | `app/build.gradle.kts:64` `isMinifyEnabled = false` (buildType release) | APK release lớn; `firebase-ai` + `material-icons-extended` nặng nhưng không được tree-shake/r8 |
| 2 | **`firebase-ai` khai báo nhưng không dùng** | `app/build.gradle.kts:132`; grep toàn code 0 kết quả | Tăng kích thước APK mà chưa có giá trị; có thể gỡ nếu chưa dùng ngay |
| 3 | **Dependency comment-out** | `firebase-firestore`, `firebase-auth` (`app/build.gradle.kts:134-135`); `accompanist-permissions`, `camera-*`, `play-services-location` (`app/build.gradle.kts:110-115,146`) | Tăng độ nhiễu trong build file |
| 4 | **Gradle wrapper thiếu script** | `gradle/wrapper/gradle-wrapper.properties` có (Gradle 9.3.1) nhưng **thiếu `gradlew` / `gradlew.bat`** ở gốc repo | Build không reproduceable trên máy sạch; CI khó chạy |
| 5 | **DI thủ công, không Hilt** | `AppContainer.kt:40-180` | Hoạt động ổn nhưng khó scope, test thay thế chậm hơn Hilt |
| 6 | **Chưa có CI** | Không thấy `.github/workflows` hay thư mục CI nào (grep/glob) **[cần xác minh]** | Không tự kiểm tra PR, regression dễ lọt |
| 7 | **Sync nền phụ thuộc process app còn sống** | `THCSApplication.kt:23-36` dùng `CoroutineScope(Dispatchers.IO)` + `syncNetworkChecker.observeChanges`; **không dùng WorkManager** | Khi app bị kill, đồng bộ không chạy cho đến khi mở lại app |
| 8 | **Màn hình test/* mồ côi** | `ui/screens/test/{TestConfig,TestTaking,TestResult,TestReview,TestHistory}Screen.kt` không có trong `NavGraph.kt` | Code chết, khó bảo trì |
| 9 | **F14 tools chưa wire UI** | Route khai báo (`Screen.kt:40-48`) nhưng `NavGraph.kt` không đăng ký; domain + asset đã có | Tính năng chưa tới tay người dùng |
| 10 | **F19 classes nửa vời** | Backend + Room entity/DAO có, nhưng chưa add vào `AppDatabase` v12, chưa có screen | Nguy cơ schema lệch backend khi triển khai sau |
| 11 | **Placeholder F15b/c/d trong Settings** | `NavGraph.kt:302-305` callback rỗng cho backup/restore/PDF/about | Người dùng bấm không có phản hồi |
| 12 | **`google-services.json` commit trong repo** | `app/google-services.json` xuất hiện trong glob | Nguy cơ lộ cấu hình Firebase **[cần xác minh]** nội dung nhạy cảm |
| 13 | **`exportSchema = false` cho Room** | `AppDatabase.kt:76` | Không có schema JSON history, khó audit migration |
| 14 | **`fallbackToDestructiveMigration()`** | `AppDatabase.kt:543` | Nếu quên viết migration ở phiên bản sau, dữ liệu người dùng sẽ bị xóa sạch |

> **Về kiểm thử tự động:** Khác với giả định "chưa có test", thực tế `app/src/test/java/com/example/` **có 45 file test** (xem glob), bao phủ khá tốt ở tầng domain/data: Room migration 4→5, 6→7, 7→8, 8→9, 9→10, 10→11, 11→12; `SearchIndexerTest`, `SearchBenchmarkTest`, `SyncConflictResolverTest`, `PocketBaseAuthIntegrationTest`, `PocketBaseSyncIntegrationTest`, `BadgeEvaluatorTest`, `XpManagerTest`, `LevelCalculatorTest`, `StreakCalculatorTest`, `StatsAggregatorTest`, `StrengthWeaknessAnalyzerTest`, `TrendCalculatorTest`, `ReviewSchedulerTest`, `TextNormalizerTest`, `CurriculumSeederPackageTest`, `StudySubjectFilterTest`, `PracticeViewModelTest`, `ReviewSessionViewModelTest`, `SplashViewModelTest`, `SelectGradeViewModelTest`, `SettingsRepositoryTest`, `PracticeComposeUiTest` (Robolectric Compose UI), `ScientificCalculatorParserTest`, `ToolRegistryTest`. Điểm yếu thực sự là **UI/instrumented coverage mỏng** (chỉ 1 Compose UI test) và **chưa có CI chạy test**.

---

## 5. Hướng phát triển đề xuất

Ưu tiên: **P0** = chặn release/sức khỏe cơ bản · **P1** = giá trị lớn, nên làm sớm · **P2** = nên có trong tầm nhìn.

### 5.1 Ngắn hạn (1–2 tuần)

| Hạng mục | Ưu tiên | Lý do (liên kết mục 4) |
|---|---|---|
| Khôi phục `gradlew` / `gradlew.bat` | P0 | Mục 4-4: build reproduceable, CI chạy được |
| Rà soát 65 lint warning, dọn nhóm dễ (nâng cấp dependency) | P1 | Mục 3: giữ 0 error, giảm warning |
| Bật R8 `isMinifyEnabled = true` + viết proguard cho Room/Moshi/Retrofit/Firebase | P1 | Mục 4-1: giảm đáng kể APK release |
| Đánh giá gỡ tạm `firebase-ai` nếu chưa dùng trong quý này | P1 | Mục 4-2: giảm kích thước, tránh code chết |
| Quyết định test/* : wire vào NavGraph hoặc xóa | P2 | Mục 4-8: giảm code chết |
| Quyết định F14 tools : wire UI hoặc xóa route placeholder | P2 | Mục 4-9 |

### 5.2 Trung hạn (1–2 tháng)

| Hạng mục | Ưu tiên | Lý do |
|---|---|---|
| Thêm CI (GitHub Actions/GitLab CI): lint + test + build debug APK trên PR | P0 | Mục 4-6: chặn regression |
| Mở rộng kiểm thử: thêm Compose UI test cho các screen chính, instrumented test, báo cáo coverage | P1 | Tăng độ tin cậy trước khi mở rộng tính năng |
| Đổi sync nền sang WorkManager (OneTime + Periodic + NetworkType.UNMETERED) | P1 | Mục 4-7: sync đúng khi app bị kill |
| AI tutoring: wire `firebase-ai` (Gemini) cho "giải thích câu hỏi", "gợi ý cá nhân hóa dựa trên StrengthWeakness" | P1 | Đã có domain StrengthWeakness + dependency; biến từ ❌ thành ✅ |
| Gamification mở rộng: huy hiệu mới, thử thách tuần, streak mục tiêu tùy chỉnh | P1 | Đã có hạ tầng `BadgeEvaluator`, `badges.json` dễ mở rộng |
| Wire F15b Backup/Restore (đang placeholder) | P1 | Mục 4-11: hoàn thiện Settings |
| FCM push notification cho nhắc học + lên cấp | P2 | Mục 2.10: hiện chỉ local notification |
| Wire F19 Lớp học UI (đã có backend + entity) | P2 | Mục 4-10: đưa tính năng lớp học tới người dùng |
| Analytics nhẹ (aggregate từ `study_sessions` đã có) | P2 | Đã có dữ liệu, chỉ cần dashboard export |

### 5.3 Dài hạn (tháng 3–6)

| Hạng mục | Ưu tiên | Lý do |
|---|---|---|
| Hoàn thiện F17a content pipeline: gói KHTN, Lịch sử & Địa lí, Ngữ văn theo từng lớp | P1 | `docs/content_pipeline.md:100-103` còn liệt kê việc còn lại |
| Tablet / adaptive layout | P2 | Tận dụng Compose, mở rộng đối tượng |
| Đa nền tảng (KMP cho iOS) | P2 | Tăng thị trường, tái dùng domain layer |
| Tính năng doanh thu: in-app purchase gói nội dung cao cấp (giữ không quảng cáo theo Families Policy) | P2 | Đã đúng Families Policy (không ads, không leaderboard công khai) |
| Web companion cho phụ huynh xem tiến độ | P3 | Tận dụng backend PocketBase đã có |
| Bật `exportSchema = true` + bỏ `fallbackToDestructiveMigration` | P1 | Mục 4-13, 4-14: an toàn migration dài hạn |

---

## 6. Đề xuất cấu trúc tài liệu tiếp theo

| Tài liệu đề xuất | Mục đích | Liện quan |
|---|---|---|
| `docs/ARCHITECTURE.md` | Mô tả layer UI → Domain → Data, AppContainer, luồng dữ liệu offline-first | Tổng quan kiến trúc |
| `docs/DATA_MODEL.md` | Schema Room v12 (22 entity), sync collections, FTS4 | `AppDatabase.kt` |
| `docs/SYNC_DESIGN.md` | Outbox, delta cursor, conflict resolver, Wi-Fi only, backoff | `SyncRepositoryImpl.kt`, `docs/content_pipeline.md` |
| `docs/CONTENT_AUTHOR_GUIDE.md` | Mở rộng `content_pipeline.md`: hướng dẫn soạn gói môn–lớp mới | F17a |
| `docs/TESTING_STRATEGY.md` | Cách chạy 45 test, target coverage, thêm UI/instrumented test | Mục 4 (kiểm thử) |
| `docs/RELEASE_CHECKLIST.md` | R8 + proguard, signing, Play Console, Data Safety, Families Policy | Mục 5.1 (R8), `docs/play_data_safety_draft.md` |
| `docs/AI_FEATURES_SPEC.md` | Kế hoạch dùng Gemini: use case, prompt, chi phí, fallback offline | Mục 2.7 (AI ❌) |
| `docs/DEPLOYMENT.md` | Mở rộng `server/README.md` + `README_F16b.md`: vận hành, backup restic, monitoring | `server/` |
| `docs/ROADMAP.md` | Tách roadmap (mục 5) thành tài liệu riêng, theo milestone có ngày | Hướng phát triển |

---

*Phạm vi tài liệu: chỉ đọc code, không sửa source, không build. Các điểm đánh dấu **[cần xác minh]** nên được kiểm tra lại trước khi dùng làm cơ sở ra quyết định.*
