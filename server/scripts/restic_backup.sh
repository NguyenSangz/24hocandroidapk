#!/usr/bin/env bash
set -euo pipefail

# F16b Script sao lưu an toàn SQLite PocketBase + mã hóa off-site bằng Restic
# Cài đặt chạy định kỳ qua systemd timer (server/systemd/restic-backup.timer)

ENV_FILE="${POCKETBASE_BACKUP_ENV:-/etc/pocketbase/restic.env}"
PB_DATA_DIR="${PB_DATA_DIR:-/var/lib/pocketbase}"
STAGING_DIR="${STAGING_DIR:-/var/backups/pocketbase/staging}"

if [[ -f "$ENV_FILE" ]]; then
    # shellcheck disable=SC1090
    source "$ENV_FILE"
fi

: "${RESTIC_REPOSITORY:?Thiếu RESTIC_REPOSITORY trong $ENV_FILE (ví dụ s3:https://... hoặc b2:bucket:/pocketbase)}"
: "${RESTIC_PASSWORD:?Thiếu RESTIC_PASSWORD trong $ENV_FILE}"

mkdir -p "$STAGING_DIR"
chmod 700 "$STAGING_DIR"

cleanup() {
    rm -f "$STAGING_DIR/data.db" "$STAGING_DIR/auxiliary.db"
}
trap cleanup EXIT

echo "[restic_backup] Tạo bản snapshot SQLite nhất quán..."
if [[ -f "$PB_DATA_DIR/data.db" ]]; then
    sqlite3 "$PB_DATA_DIR/data.db" ".backup '$STAGING_DIR/data.db'"
fi
if [[ -f "$PB_DATA_DIR/auxiliary.db" ]]; then
    sqlite3 "$PB_DATA_DIR/auxiliary.db" ".backup '$STAGING_DIR/auxiliary.db'"
fi

echo "[restic_backup] Kiểm tra tính toàn vẹn SQLite trước khi đẩy lên cloud..."
if [[ -f "$STAGING_DIR/data.db" ]]; then
    INTEGRITY="$(sqlite3 "$STAGING_DIR/data.db" "PRAGMA integrity_check;")"
    if [[ "$INTEGRITY" != "ok" ]]; then
        echo "[restic_backup] LỖI: PRAGMA integrity_check thất bại: $INTEGRITY" >&2
        exit 1
    fi
fi

echo "[restic_backup] Khởi tạo kho restic nếu chưa tồn tại..."
if ! restic snapshots >/dev/null 2>&1; then
    restic init
fi

echo "[restic_backup] Đẩy bản sao lưu mã hóa off-site..."
restic backup "$STAGING_DIR" \
    --tag "hoctot-thcs" \
    --tag "pocketbase"

echo "[restic_backup] Dọn dẹp các snapshot cũ (giữ 7 ngày gần nhất, 4 tuần gần nhất)..."
restic forget \
    --keep-daily 7 \
    --keep-weekly 4 \
    --prune

echo "[restic_backup] Hoàn tất sao lưu an toàn lúc $(date -Iseconds)."
