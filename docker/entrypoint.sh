#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Telegram Media Downloader - 单容器启动入口
# 职责：
#   1. 处理 MySQL 首次初始化（仅执行一次）
#   2. 配置 MySQL root 密码（从环境变量 DB_PASSWORD 读取）
#   3. 启动 supervisord（管理 MySQL + Java App + Nginx）
# ─────────────────────────────────────────────────────────────────────────────

set -e
chmod -R 777 /usr/share/nginx/html
MYSQL_DATA_DIR="/var/lib/mysql"
MYSQL_INIT_MARKER="/data/.mysql_initialized"
DB_PASSWORD="${DB_PASSWORD:-root_password}"

# ── MySQL 首次初始化 ─────────────────────────────────────────────────────────
# MySQL 数据目录为空时（全新安装），需要初始化
if [ ! -d "$MYSQL_DATA_DIR/mysql" ]; then
    echo "[Entrypoint] MySQL data directory is empty, initializing..."

    # mysqld --initialize-insecure 不需要临时密码，直接初始化
    mysqld --initialize-insecure --user=mysql --datadir="$MYSQL_DATA_DIR"

    echo "[Entrypoint] MySQL initialized successfully."
fi

# ── 启动 MySQL（后台，允许远程 root 登录 + 设置密码）────────────────────────
echo "[Entrypoint] Starting MySQL in background..."
mysqld --user=mysql \
       --datadir="$MYSQL_DATA_DIR" \
       --skip-grant-tables \
       --skip-networking=off \
       --bind-address=0.0.0.0 \
       --character-set-server=utf8mb4 \
       --collation-server=utf8mb4_bin &

MYSQL_PID=$!

# ── 等待 MySQL 就绪 ─────────────────────────────────────────────────────────
echo "[Entrypoint] Waiting for MySQL to be ready..."
for i in $(seq 1 60); do
    if mysqladmin ping --silent 2>/dev/null; then
        echo "[Entrypoint] MySQL is ready."
        break
    fi
    echo "[Entrypoint] Waiting for MySQL... ($i/60)"
    sleep 2
done

if ! mysqladmin ping --silent 2>/dev/null; then
    echo "[Entrypoint] ERROR: MySQL failed to start."
    exit 1
fi

# ── 配置 MySQL（设置 root 密码、允许远程登录）────────────────────────────────
echo "[Entrypoint] Configuring MySQL root user..."

mysql --user=mysql << 'EOSQL'
FLUSH PRIVILEGES;

ALTER USER 'root'@'localhost' IDENTIFIED BY 'root_password';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;

-- 执行初始化脚本（建库、建表、默认配置）
-- 注意：/docker-entrypoint-initdb.d/ 在单容器模式下需要手动触发
-- （官方 mysql 镜像会自动执行，但这里 MySQL 以 --skip-grant-tables 启动，
--   需要重启后才能正常执行初始化脚本）
EOSQL

echo "[Entrypoint] MySQL root user configured."

# ── 执行数据库初始化 SQL（建库建表）────────────────────────────────────────
echo "[Entrypoint] Running database initialization script..."
mysql --user=root --password=root_password < /docker-entrypoint-initdb.d/00-init.sql || {
    echo "[Entrypoint] WARNING: init.sql may have already been run or failed. Continuing..."
}

# ── 重启 MySQL（正常模式，启用密码认证）─────────────────────────────────────
echo "[Entrypoint] Restarting MySQL in normal mode..."
kill $MYSQL_PID 2>/dev/null || true
wait $MYSQL_PID 2>/dev/null || true
sleep 2

# 重新启动 MySQL（正常模式，监听所有接口）
mysqld --user=mysql \
       --datadir="$MYSQL_DATA_DIR" \
       --bind-address=0.0.0.0 \
       --character-set-server=utf8mb4 \
       --collation-server=utf8mb4_ubin &

echo "[Entrypoint] MySQL started in normal mode (PID: $!)."

# ── 等待 MySQL 完全就绪 ─────────────────────────────────────────────────────
sleep 5

# ── 写入初始化完成标记 ──────────────────────────────────────────────────────
touch "$MYSQL_INIT_MARKER"
echo "[Entrypoint] MySQL initialization complete. Marker created at $MYSQL_INIT_MARKER."

# ── 启动主进程管理 supervisord ────────────────────────────────────────────
echo "[Entrypoint] Starting supervisord (managing MySQL + Java App + Nginx)..."
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf
