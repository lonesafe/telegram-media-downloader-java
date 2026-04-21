# Telegram Media Downloader Java 转换总结

## 📅 2026-04-14

## 任务目标
将 Python 项目 `telegram_media_downloader` 转换为 Java + TDLib + SpringBoot + Vue3 技术栈

## 新增功能：配置 Web 端管理

### 数据库持久化
- `TelegramConfig` 实体：存储 API ID/Hash、Bot Token、TDLib 路径等
- `TelegramConfigRepository`：数据库操作
- `ConfigService`：启动时自动加载配置

### Web 配置页面
- `TelegramConfig.vue`：完整的配置管理界面
- 支持测试连接
- 支持授权流程（手机号→验证码→密码）

### API 端点
| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/telegram/config` | GET | 获取配置 |
| `/api/telegram/config` | POST | 保存配置 |
| `/api/telegram/test-connection` | POST | 测试连接 |
| `/api/telegram/status` | GET | 获取状态 |
| `/api/telegram/auth/phone` | POST | 发送手机号 |
| `/api/telegram/auth/code` | POST | 验证验证码 |
| `/api/telegram/auth/password` | POST | 验证密码 |

## TDLib 集成架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Vue3 Frontend                          │
│                  (Element Plus + WebSocket)                  │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP/WebSocket
┌────────────────────────────▼────────────────────────────────┐
│                   Spring Boot Backend                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              DownloadCoreService                      │  │
│  │  - createDownloadTask()    - createForwardTask()     │  │
│  │  - createListenForwardTask()                        │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              BotCommandService                        │  │
│  │  - /download  - /forward  - /stop  - /help          │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              CloudDriveService                        │  │
│  │  - uploadByRclone()  - uploadByAligo()              │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              FilterEngine                            │  │
│  │  - DSL: file_size > 10MB, message_date >= '...'   │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────┘
                             │ JSON RPC
┌────────────────────────────▼────────────────────────────────┐
│                      TDLib (C++ Library)                     │
│  - tdjson.dll / tdjson.so / tdjson.dylib                    │
│  - 完整 MTProto 协议支持                                      │
│  - getChatHistory, downloadFile, forwardMessages 等           │
└─────────────────────────────────────────────────────────────┘
```

## 新增核心文件

### TelegramClient.java (TDLib 封装)
- 进程间通信（TDLib JSON 接口）
- 异步请求/响应处理
- 事件监听器机制
- 文件下载进度回调
- 授权状态管理

### DownloadCoreService.java (核心下载)
对应原 Python: `media_downloader.py` + `module/app.py`

| 原 Python 函数 | Java 方法 |
|--------------|----------|
| `download_chat_task` | `downloadChatMessages()` |
| `download_media` | `downloadMessage()` |
| `forward_message` | `forwardMessages()` |
| `set_listen_forward_msg` | `createListenForwardTask()` |

### BotCommandService.java (Bot 命令)
对应原 Python: `module/bot.py`

| 命令 | 功能 |
|------|------|
| `/help` | 显示帮助 |
| `/download` | 下载消息 |
| `/forward` | 转发消息 |
| `/listen_forward` | 监听转发 |
| `/set_language` | 设置语言 |
| `/stop` | 停止任务 |
| `/get_info` | 获取聊天信息 |

### CloudDriveService.java (云盘上传)
对应原 Python: `module/cloud_drive.py`

- `uploadByRclone()` - Rclone 上传
- `uploadByAligo()` - 阿里云盘上传
- `zipFile()` - 上传前压缩
- 目录缓存避免重复创建

### FilterEngine.java (过滤器)
对应原 Python: `module/filter.py`

支持语法：
```
file_size > 10MB
message_date >= '2024-01-01 00:00:00'
caption contains 'video'
file_name r'.*\.(mp4|mkv)$'
```

### LanguageService.java (多语言)
对应原 Python: `module/language.py`

支持语言：EN, ZH, RU, UA

## 使用前提

1. **编译 TDLib**（用户已完成）
2. 将 `tdjson.dll/so/dylib` 放到 `backend/tdlib/` 目录
3. 配置 `application.yml` 中的 API ID/Hash
4. 运行 `./mvnw spring-boot:run`

## 下一步工作

1. 完善 TDLib 进程通信（TelegramClient）
2. 测试 Bot 命令处理
3. 实现完整的过滤器 DSL
4. 添加 WebSocket 实时推送
5. 编写单元测试

## 文件位置
`C:\Users\lones\.qclaw\workspace\telegram-media-downloader-java\`
