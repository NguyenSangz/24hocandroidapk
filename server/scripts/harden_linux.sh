#!/usr/bin/env bash
set -euo pipefail

# F16b Script làm cứng bảo mật máy chủ Linux (Ubuntu/Debian) cho PocketBase Self-Hosted
# Lưu ý: Đọc kỹ và thay SSH_PORT nếu bạn dùng cổng SSH khác 22 trước khi chạy với sudo.

SSH_PORT="${SSH_PORT:-22}"

if [[ "$(id -u)" -ne 0 ]]; then
    echo "Vui lòng chạy script này bằng sudo/root." >&2
    exit 1
fi

echo "[1/4] Cập nhật gói bảo mật tự động (unattended-upgrades)..."
apt-get update -y
apt-get install -y ufw fail2ban unattended-upgrades sqlite3
dpkg-reconfigure -f noninteractive unattended-upgrades || true

echo "[2/4] Cấu hình tường lửa UFW (chỉ mở SSH, 80, 443; chặn cổng PocketBase 8090 từ ngoài)..."
ufw default deny incoming
ufw default allow outgoing
ufw allow "${SSH_PORT}/tcp" comment 'SSH'
ufw allow 80/tcp comment 'HTTP Acme Challenge'
ufw allow 443/tcp comment 'HTTPS Public API'
ufw deny 8090/tcp comment 'Block direct PocketBase port'
ufw --force enable
ufw status verbose

echo "[3/4] Cấu hình fail2ban chống dò mật khẩu SSH và spam API..."
cat >/etc/fail2ban/jail.d/hoctot-thcs.conf <<EOF
[DEFAULT]
bantime = 1h
findtime = 10m
maxretry = 5
ignoreip = 127.0.0.1/8 ::1

[sshd]
enabled = true
port = ${SSH_PORT}
mode = aggressive
EOF

systemctl enable --now fail2ban
systemctl restart fail2ban

echo "[4/4] Tạo cấu hình SSH chỉ cho phép khóa công khai (Public Key Auth)..."
cat >/etc/ssh/sshd_config.d/99-hoctot-hardening.conf <<'EOF'
# CẢNH BÁO: Chỉ bật khi bạn ĐÃ kiểm tra đăng nhập bằng SSH key thành công!
PermitRootLogin prohibit-password
PasswordAuthentication no
PubkeyAuthentication yes
X11Forwarding no
MaxAuthTries 4
EOF

echo "Kiểm tra cú pháp sshd..."
sshd -t
echo "Gợi ý: Hãy mở sẵn 1 phiên SSH khác rồi mới chạy: sudo systemctl reload ssh"
echo "Hoàn tất làm cứng cơ bản Linux!"
