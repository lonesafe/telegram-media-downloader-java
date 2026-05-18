# ─────────────────────────────────────────────────────────────────────────────
# Telegram Media Downloader - 单容器全集模式
#
# 包含：JDK 17 + MySQL 8.0 + Nginx + Spring Boot
# 进程管理：supervisord
#
# 【宿主机编译步骤】
#   后端: cd backend && mvn package -DskipTests
#         → 产物: backend/target/telegram-media-downloader-1.0.0.jar
#   前端: cd frontend && npm install && npm run build
#         → 产物: frontend/dist/
#
# 【Docker 启动】
#   docker compose up -d --build
#
# ─────────────────────────────────────────────────────────────────────────────

FROM ubuntu:22.04

LABEL maintainer="tgdownloader"

ENV DEBIAN_FRONTEND=noninteractive \
    TZ=Asia/Shanghai \
    MYSQL_ROOT_PASSWORD=root_password \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    DB_HOST="localhost" \
    DB_PORT="3306"

# ── 安装基础依赖 ────────────────────────────────────────────────────────────
RUN rm -rf /var/lib/apt/lists/* && \
    # 备份原源并替换为网易163源
    cp /etc/apt/sources.list /etc/apt/sources.list.bak && \
    echo "deb http://mirrors.163.com/ubuntu/ jammy main restricted universe multiverse" > /etc/apt/sources.list && \
    echo "deb http://mirrors.163.com/ubuntu/ jammy-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb http://mirrors.163.com/ubuntu/ jammy-updates main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb http://mirrors.163.com/ubuntu/ jammy-proposed main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb http://mirrors.163.com/ubuntu/ jammy-backports main restricted universe multiverse" >> /etc/apt/sources.list

# ── 安装 MySQL 8.0 ─────────────────────────────────────────────────────────
RUN apt update
RUN apt-get install -y wget nginx curl gnupg2 ca-certificates supervisor logrotate lsb-release
RUN wget https://dev.mysql.com/get/mysql-apt-config_0.8.39-1_all.deb
RUN echo "mysql-apt-config mysql-apt-config/select-server select mysql-8.0" | debconf-set-selections

RUN apt update
#RUN apt-get install -y --no-install-recommends mysql-community-server
RUN dpkg -i mysql-apt-config_0.8.39-1_all.deb
RUN rm mysql-apt-config_0.8.39-1_all.deb
RUN apt-get  install -y mysql-server openjdk-17-jre-headless
RUN mkdir -p /var/run/mysqld /var/log/mysql
RUN chown mysql:mysql /var/run/mysqld /var/log/mysql
RUN chmod 777 /var/run/mysqld


# ── 预创建目录 ─────────────────────────────────────────────────────────────
RUN mkdir -p /data/downloads /data/logs /data/tdlib_db /data/temp /var/lib/mysql /docker-entrypoint-initdb.d /app

# ── 复制构建产物 ───────────────────────────────────────────────────────────
# 后端 JAR
COPY telegram-media-downloader-1.0.0.jar /app/app.jar

# 前端静态文件
COPY frontend/dist/ /usr/share/nginx/html/
RUN chmod -R 777 /usr/share/nginx/html

# Nginx 配置
COPY frontend/nginx.conf /etc/nginx/nginx.conf
# 禁止 Nginx daemon 模式（由 supervisord 管理）
RUN echo "daemon off;" >> /etc/nginx/nginx.conf

# MySQL 初始化脚本
COPY sql/init.sql /docker-entrypoint-initdb.d/00-init.sql

# 启动脚本
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Supervisor 配置
COPY docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# ── 暴露端口 ───────────────────────────────────────────────────────────────
# 80: Nginx（前端 + API 反向代理）
# 3306: MySQL（可选，仅宿主机调试用）
EXPOSE 80 3306

ENTRYPOINT ["/entrypoint.sh"]
