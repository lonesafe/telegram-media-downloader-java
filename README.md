# Telegram Media Downloader - Java 版 (TDLib)

> 基于 Python 项目 `telegram_media_downloader` 转换为 **SpringBoot + TDLib + Vue3 + Element Plus** 技术栈

## 项目概述

本项目是一个 Telegram 媒体资源下载工具，支持：

- ✅ 从 Telegram 频道/群组下载媒体文件（视频、音频、图片、文档等）
- ✅ **Bot 机器人模式**：通过 Bot 命令下载或转发消息
- ✅ **Web UI 界面**：实时查看下载进度、管理任务
- ✅ **云盘上传**：支持 Rclone、阿里云盘等
- ✅ **消息转发**：支持将消息转发到其他频道/群组
- ✅ **监听转发**：自动监听并转发新消息
- ✅ **下载过滤**：支持自定义过滤规则（DSL）
- ✅ **多语言**：支持 English、中文、Русский、Українська
- ✅ **跨平台**：自动检测系统类型，加载对应的 TDLib 库

## 技术栈

### 后端
- **Java 17+** + **Spring Boot 3.2**
- **TDLib** (Telegram Database Library) - 完整 MTProto 协议支持
- **Spring Security** + JWT 认证
- **Spring WebSocket** - 实时进度推送
- **JPA/Hibernate** - 数据持久化
- **H2/MySQL** - 数据库
- **Redis** (可选) - 缓存

### 前端
- **Vue 3** + Composition API
- **Element Plus** - UI 组件库
- **Axios** - HTTP 客户端
- **Vue Router** + **Pinia** - 路由和状态管理
- **ECharts** - 进度图表
- **WebSocket** - 实时通信

## TDLib 集成说明

### TDLib 是什么？

TDLib (Telegram Database Library) 是 Telegram 官方维护的跨平台 C++ 库，提供完整的 MTProto 协议实现。相比 Bot API，TDLib 可以：

- ✅ 访问用户账号（不是 Bot）
- ✅ 获取完整聊天历史
- ✅ 下载任意媒体文件
- ✅ 监听新消息
- ✅ 转发消息
- ✅ 搜索消息

### TDLib 目录结构（跨平台支持）

项目会根据当前操作系统和架构**自动选择**对应的 TDLib 库文件：

```
tdlib/
├── windows/
│   ├── x64/
│   │   └── tdjni.dll        # Windows 64位
│   └── x86/
│       └── tdjni.dll        # Windows 32位
├── linux/
│   ├── x64/
│   │   └── libtdjni.so      # Linux 64位 (Intel/AMD)
│   ├── arm64/
│   │   └── libtdjni.so      # Linux ARM64 (树莓派4/服务器)
│   └── arm/
│       └── libtdjni.so      # Linux ARM32
├── macos/
│   ├── x64/
│   │   └── libtdjni.dylib   # macOS Intel
│   └── arm64/
│       └── libtdjni.dylib   # macOS Apple Silicon (M1/M2/M3)
└── freebsd/
    └── x64/
        └── libtdjni.so      # FreeBSD 64位
```

**系统自动检测：**
- 操作系统：Windows、Linux、macOS、FreeBSD、OpenBSD、NetBSD、Solaris
- CPU 架构：x64、x86、arm64、arm、ppc64、mips64、riscv64 等
- 日志输出：启动时会显示检测到的系统和库路径

### 编译 TDLib

#### Windows (MSVC)

```bash
# 1. 安装 Visual Studio Build Tools 2019+
# 2. 安装 CMake (https://cmake.org/download/)
# 3. 安装 vcpkg
git clone https://github.com/Microsoft/vcpkg.git
cd vcpkg
.\bootstrap-vcpkg.bat
.\vcpkg install openssl:x64-windows zlib:x64-windows

# 4. 克隆 TDLib
git clone https://github.com/tdlib/td.git
cd td

# 5. 编译 TDLib + JNI
mkdir jnibuild && cd jnibuild
cmake -DCMAKE_BUILD_TYPE=Release ^
      -DTD_ENABLE_JNI=ON ^
      -DCMAKE_TOOLCHAIN_FILE=%VCPKG_DIR%\scripts\buildsystems\vcpkg.cmake ^
      -DCMAKE_INSTALL_PREFIX=..\example\java\td ..
cmake --build . --target install

# 6. 复制到项目目录
copy example\java\bin\tdjni.dll your-project\tdlib\windows\x64\
```

#### Linux

```bash
# 安装依赖
sudo apt-get install build-essential cmake libssl-dev zlib1g-dev

# 克隆并编译
git clone https://github.com/tdlib/td.git
cd td
mkdir jnibuild && cd jnibuild
cmake -DCMAKE_BUILD_TYPE=Release -DTD_ENABLE_JNI=ON ..
cmake --build . --target install

# 复制到项目目录
cp bin/libtdjni.so your-project/tdlib/linux/x64/
```

#### macOS

```bash
# 安装依赖
brew install cmake openssl zlib

# 克隆并编译
git clone https://github.com/tdlib/td.git
cd td
mkdir jnibuild && cd jnibuild
cmake -DCMAKE_BUILD_TYPE=Release -DTD_ENABLE_JNI=ON ..
cmake --build . --target install

# 复制到项目目录
cp bin/libtdjni.dylib your-project/tdlib/macos/arm64/  # Apple Silicon
# 或
cp bin/libtdjni.dylib your-project/tdlib/macos/x64/    # Intel
```

详细编译说明：https://tdlib.github.io/td/build.html?language=Java

### 项目配置

1. 将编译好的 TDLib 库文件放到对应目录（见上方目录结构）

2. 配置 `application.yml`：
```yaml
telegram:
  api-id: 12345678
  api-hash: your_api_hash_here
  tdlib-directory: ./tdlib  # TDLib 根目录
  database-directory: ./sessions/tdlib
  files-directory: ./sessions/tdlib/files
```

3. 从 https://my.telegram.org/apps 获取 API ID 和 Hash

## 快速开始

### 1. 编译并配置 TDLib

```bash
# 克隆项目
git clone https://github.com/your-repo/telegram-media-downloader-java.git
cd telegram-media-downloader-java

# 编译 TDLib（参考上方说明）
# 将库文件放到 tdlib/<os>/<arch>/ 目录
```

### 2. 启动后端

```bash
cd backend

# 使用 Maven 启动
./mvnw spring-boot:run

# 或打包后运行
./mvnw package -DskipTests
java -jar target/telegram-media-downloader-1.0.0.jar
```

### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

### 4. 访问 Web UI

打开浏览器访问：http://localhost:3000

默认登录密码：`123456`

### 5. 配置 Telegram

1. 进入 Web UI → 菜单 → **Telegram**
2. 填写 API ID 和 API Hash
3. 点击 **保存配置**
4. 点击 **授权** 完成登录

## Bot 命令

在 Telegram 中向 Bot 发送以下命令：

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |
| `/download <链接> <起始ID> <结束ID> [过滤]` | 下载消息 |
| `/forward <源聊天> <目标聊天> [起始ID] [结束ID]` | 转发消息 |
| `/listen_forward <源聊天> <目标聊天>` | 监听转发 |
| `/set_language <en\|zh\|ru\|ua>` | 设置语言 |
| `/stop [任务ID]` | 停止任务 |
| `/get_info <消息链接>` | 获取聊天/消息信息 |

### 下载示例

```
/download https://t.me/channel/100 1 50
/download -1001234567890 1 0 file_size > 10MB
```

### 过滤表达式

```
file_size > 10MB
message_date >= '2024-01-01'
caption contains 'video'
file_name r'.*\.(mp4|mkv)$'
```

## 项目结构

```
telegram-media-downloader-java/
├── backend/
│   ├── src/main/java/com/tgdownloader/
│   │   ├── TelegramMediaDownloaderApplication.java
│   │   ├── config/
│   │   │   ├── TDLibConfig.java           # TDLib 配置
│   │   │   ├── SecurityConfig.java         # Spring Security
│   │   │   └── WebSocketConfig.java        # WebSocket
│   │   ├── controller/                     # REST API
│   │   ├── service/
│   │   │   ├── DownloadCoreService.java    # 核心下载服务
│   │   │   ├── CloudDriveService.java      # 云盘上传
│   │   │   ├── BotCommandService.java       # Bot 命令处理
│   │   │   ├── ConfigService.java          # 配置管理
│   │   │   └── LanguageService.java        # 多语言
│   │   ├── telegram/
│   │   │   └── TelegramClientService.java  # TDLib 客户端
│   │   ├── util/
│   │   │   ├── SystemInfo.java             # 系统检测工具
│   │   │   └── ByteFormatUtil.java         # 字节格式化
│   │   ├── entity/                          # JPA 实体
│   │   ├── model/                           # 枚举和模型
│   │   └── repository/                      # 数据仓库
│   ├── tdlib/                               # TDLib 原生库
│   │   ├── windows/x64/tdjni.dll
│   │   ├── linux/x64/libtdjni.so
│   │   └── macos/arm64/libtdjni.dylib
│   ├── pom.xml
│   └── resources/
│       └── application.yml
│
├── frontend/
│   ├── src/
│   │   ├── views/                          # Vue 页面
│   │   │   ├── TelegramConfig.vue          # Telegram 配置页
│   │   │   ├── Downloads.vue               # 下载管理页
│   │   │   └── ...
│   │   ├── api/                             # API 调用
│   │   └── router/                          # 路由
│   └── package.json
│
└── README.md
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `TELEGRAM_API_ID` | Telegram API ID | - |
| `TELEGRAM_API_HASH` | Telegram API Hash | - |
| `TELEGRAM_BOT_TOKEN` | Bot Token (可选) | - |
| `TDLIB_DIR` | TDLib 根目录 | `./tdlib` |
| `DOWNLOAD_PATH` | 下载保存路径 | `./downloads` |
| `WEB_PASSWORD` | Web 登录密码 | `123456` |
| `CLOUD_ENABLE` | 启用云盘上传 | `false` |
| `CLOUD_ADAPTER` | 云盘适配器 | `rclone` |

## 与原 Python 项目对比

| 功能 | Python (Pyrogram) | Java (TDLib) |
|------|-------------------|--------------|
| 协议支持 | MTProto | MTProto (完整) |
| 用户账号 | ✅ | ✅ |
| Bot 账号 | ✅ | ✅ |
| 下载媒体 | ✅ | ✅ |
| 转发消息 | ✅ | ✅ |
| 监听新消息 | ✅ | ✅ |
| 搜索消息 | ✅ | ✅ |
| 云盘上传 | ✅ (rclone/aligo) | ✅ (rclone/aligo) |
| Web UI | Flask | Spring Boot + Vue3 |
| 过滤 DSL | PLY 解析器 | 手写解析器 |
| 跨平台 | ✅ | ✅ (自动检测) |
| 配置管理 | YAML 文件 | 数据库 + Web UI |

## License

MIT (与原项目一致)
