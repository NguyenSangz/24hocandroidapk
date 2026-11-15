# Pipeline nội dung (F17a)

Tài liệu này mô tả cách thêm/cập nhật nội dung học tập cho ứng dụng **Học Tốt THCS** mà không cần thay đổi code.

## 1. Kiến trúc tổng quan

```
assets/
├── sample_curriculum.json      ← Gói gốc: 15 môn + nội dung Toán, Tiếng Anh (không đổi tên file/packageId)
└── curriculum/                 ← Các gói nội dung bổ sung (F17a)
    ├── gdcd_kntt_l6.json       ← Gói thí điểm GDCD lớp 6
    └── <gói-mới>.json
```

- `CurriculumDataSeeder` nạp **gói gốc trước**, sau đó mọi file trong `assets/curriculum/` theo **thứ tự tên file**.
- Room là nguồn dữ liệu duy nhất của UI (offline-first). Gói lỗi không chặn các gói khác.
- Sau khi seed, `SearchIndexer` tự dựng lại chỉ mục FTS nên nội dung mới tìm kiếm được ngay.

## 2. Định dạng gói nội dung

```json
{
  "packageId": "gdcd_kntt_l6_v1",     // BẮT BUỘC, không đổi trong suốt vòng đời gói
  "contentVersion": 1,                 // BẮT BUỘC, tăng +1 mỗi lần cập nhật nội dung
  "packageTitle": "Mô tả ngắn gói",
  "subjects": [],                      // [] nếu gói chỉ bổ sung nội dung cho môn đã có
  "grades": [
    {
      "grade": 6,
      "chapters": [
        {
          "id": "g6_gdcd_c1",
          "subjectId": "gdcd",
          "grade": 6,
          "order": 1,
          "title": "Chương 1: ...",
          "description": "...",
          "lessons": [
            {
              "id": "g6_gdcd_c1_l1",
              "chapterId": "g6_gdcd_c1",
              "order": 1,
              "title": "Bài 1: ...",
              "summary": "Nội dung bài học (bắt buộc, xuống dòng bằng \\n)",
              "examples": "Ví dụ minh họa",
              "tips": "Mẹo ghi nhớ"
            }
          ]
        }
      ]
    }
  ],
  "questions": [
    {
      "id": "q_g6_gdcd_c1_l1_1",
      "lessonId": "g6_gdcd_c1_l1",
      "type": "MULTIPLE_CHOICE",       // MULTIPLE_CHOICE | TRUE_FALSE | FILL_BLANK | MATCHING | SORTING | SHORT_ESSAY
      "content": "Nội dung câu hỏi",
      "options": ["A", "B", "C", "D"], // TRUE_FALSE: ["Đúng","Sai"]; FILL_BLANK: []
      "correctAnswer": "A",            // FILL_BLANK: chuỗi đáp án; MC/TF: phải nằm trong options
      "hint": "Gợi ý",
      "explanation": "Giải thích (bắt buộc)",
      "difficulty": 1                  // 1: Nhận biết, 2: Thông hiểu, 3: Vận dụng
    }
  ],
  "flashcards": []                     // Tùy chọn; id, grade, subjectId, front, back
}
```

Quy tắc đặt ID (khuyến nghị, dùng trong gói thí điểm):
- Chương: `g<lớp>_<môn>_c<stt>` — ví dụ `g6_gdcd_c1`
- Bài: `g<lớp>_<môn>_c<stt>_l<stt>` — ví dụ `g6_gdcd_c1_l1`
- Câu hỏi: `q_<bài>_<stt>` — ví dụ `q_g6_gdcd_c1_l1_1`

**Không được trùng ID** giữa các gói hoặc với `sample_curriculum.json` — Room dùng `REPLACE` theo PRIMARY KEY nên ID trùng sẽ âm thầm ghi đè dữ liệu cũ. `validate_content.py` đã kiểm tra trùng chéo.

## 3. Content version & upsert

- Bảng `content_versions` (Room, migration 11→12) lưu `{packageId, version, importedAtMillis}`.
- Mỗi lần mở app, seeder so sánh `contentVersion` trong JSON với bản ghi đã lưu:
  - **Lớn hơn** → nhập lại gói (upsert toàn bộ), rồi ghi nhận phiên bản mới.
  - **Bằng hoặc nhỏ hơn** → bỏ qua (không đọc lại toàn bộ file, không đụng DB).
- Khi nhập lại, seeder **bảo toàn dữ liệu người dùng**:
  - `lessons.isCompleted` (tiến độ "đã học")
  - flashcard: `box`, `intervalDays`, `nextReviewTimestamp`, `easeFactor`, `repetitions` (lịch SM-2)
  - `subjects.isPinned` (ghim môn)

## 4. Ẩn môn chưa có nội dung

Tab **Học tập** chỉ hiển thị các môn **có chương cho lớp đang chọn** (query `SELECT DISTINCT subjectId FROM chapters WHERE grade = :grade`). Các môn khai báo sẵn nhưng chưa có gói nội dung không hiện chip rỗng; khi thêm gói cho lớp 7, môn đó tự xuất hiện ở lớp 7.

## 5. Quy trình thêm/cập nhật nội dung

1. Soạn file JSON mới theo schema ở mục 2 (đặt `packageId` mới cho gói mới; giữ nguyên `packageId` và tăng `contentVersion` khi cập nhật gói cũ).
2. Đặt file vào `app/src/main/assets/curriculum/`.
3. Chạy kiểm tra: `python validate_content.py` (đặt `PYTHONIOENCODING=utf-8` nếu console Windows báo lỗi font).
4. Chạy test liên quan: `CurriculumSeederPackageTest`, `RoomMigration11To12Test`, `StudySubjectFilterTest`.
5. Build + chạy app: vào tab Học tập, chọn lớp và kiểm tra môn mới hiện chip, bài/câu hỏi mở được, tìm kiếm thấy nội dung mới.

## 6. Việc còn lại của F17a (các đợt sau)

- Gói KHTN, Lịch sử & Địa lí, Ngữ văn theo từng lớp (giữ nguyên `packageId` riêng từng môn–lớp để cập nhật độc lập).
- Ghi nguồn/người soạn vào `packageTitle` và phần "Giấy phép" khi hoàn thiện F15d.
