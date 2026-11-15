#!/usr/bin/env bash
set -euo pipefail

# F16b Script diễn tập khôi phục (Restore Drill) kiểm chứng bản sao lưu Restic
# Mục tiêu: bảo đảm bản sao lưu giải mã được và SQLite data.db không bị hỏng.

ENV_FILE="${POCKETBASE_BACKUP_ENV:-/etc/pocketbase/restic.env}"
RESTORE_TMP_DIR="/tmp/pocketbase_restore_drill_$$"

if [[ -f "$ENV_FILE" ]]; then
    # shellcheck disable=SC1090
    source "$ENV_FILE"
fi

: "${RESTIC_REPOSITORY:?Thiếu RESTIC_REPOSITORY trong $ENV_FILE}"
: "${RESTIC_PASSWORD:?Thiếu RESTIC_PASSWORD trong $ENV_FILE}"

cleanup() {
    rm -rf "$RESTORE_TMP_DIR"
}
trap cleanup EXIT

mkdir -p "$RESTORE_TMP_DIR"

echo "[restic_restore_test] Danh sách snapshot gần nhất:"
restic snapshots --latest 5

echo "[restic_restore_test] Giải mã snapshot mới nhất vào $RESTORE_TMP_DIR..."
restic restore latest --target "$RESTORE_TMP_DIR"

RESTORED_DB="$(find "$RESTORE_TMP_DIR" -name 'data.db' | head -n 1 || true)"
if [[ -z "$RESTORED_DB" || ! -f "$RESTORED_DB" ]]; then
    echo "[restic_restore_test] LỖI: Không tìm thấy file data.db trong bản khôi phục!" >&2
    exit 1
fi

INTEGRITY="$(sqlite3 "$RESTORED_DB" "PRAGMA integrity_check;")"
if [[ "$INTEGRITY" != "ok" ]]; then
    echo "[restic_restore_test] LỖI: Database khôi phục hỏng integrity: $INTEGRITY" >&2
    exit 1
fi

USER_COUNT="$(sqlite3 "$RESTORED_DB" "SELECT COUNT(*) FROM users;" || echo "0")"
echo "[restic_restore_test] THÀNH CÔNG: PRAGMA integrity_check = ok | Số tài khoản users trong backup = $USER_COUNT"
