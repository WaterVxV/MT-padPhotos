# MT padPhotos（MT轮播相册）

把一台闲置的安卓平板改造成**常驻的物理电子相册**：设备长期放在家里滚动展示 NAS 相册库中的照片和视频，支持开机自启、自动轮播、后台同步与离线观看。

> **本项目是 [MT Photo](https://mtmt.tech)（NAS 相册软件）的专用客户端，不是通用相册应用。** 认证体系（API Key → auth_code）、数据接口（`/api-album/filesV2`、`/gateway/*`、`/file-delete-log`）与媒体画质规格（s260/proxy 等）均直接建立在 MT Photo OpenAPI 之上，离开 MT Photo 无法工作。

| | |
|---|---|
| ![主界面](docs/images/screenshot-main.png) | ![控制层](docs/images/screenshot-controls.png) |

📘 **使用手册（面向使用者，含真机截图）**：[docs/使用说明书.md](docs/使用说明书.md)

## 功能特性

1. **开机自启直达轮播**：开机自动拉起 App，直接进入上次播放的相册并从上次位置继续
2. **照片视频混合轮播**：照片按设定时长（默认 5 秒）自动切换；视频完整播放后自动续播下一项；默认静音、可一键取消静音
3. **播放控制**：三种顺序（正向/倒序/随机）、四种过渡（淡入淡出/滑动/硬切/肯伯恩斯）、手势翻页、顶栏「从头开始」一键回到第一张、播放列表多选删除与撤销、拖动排序
4. **断点续播**：按媒体文件 ID 记忆播放位置（刷新打乱顺序也能对上），且只保留最近播放的相册
5. **相册管理**：展示账号下全部相册（含数量）、下拉刷新、全局搜索
6. **自动同步**：登录后同步相册列表与删除日志；进入相册时按需拉取文件清单并与本地快照做增量 diff；WorkManager 按设定频率后台同步（5 分钟节流）
7. **离线观看**：图片磁盘缓存（可用空间的 80%）+ 过期 auth_code 兜底，断网后看过的照片依然可看
8. **连接状态提示**：服务器连不上时顶部红色横幅提示并保留离线内容，恢复后自动消失，支持手动重试
9. **服务器配置随时改**：设置页可直接修改 NAS 地址与 API Key（先验证后保存、失败自动回滚），IP 漂移不再需要重装或重登
10. **安全与适配**：API Key 加密存储（EncryptedSharedPreferences）、PIN 锁（仅冷启动）、国产 ROM 自启动引导（小米/OPPO/vivo/华为/荣耀/魅族/三星）、首次使用教程

## 运行环境

| 项 | 值 |
|---|---|
| 目标设备 | 安卓平板（参考设备：Helio G99 / 4GB RAM / 10.61" 2000×1200 横屏） |
| 系统 | Android 12+（真机验证于 Android 14） |
| 网络 | 与 NAS 同局域网 |
| 服务端 | **[MT Photo](https://mtmt.tech)**（需开启 API，使用管理员 API Key 登录） |

## 技术栈

Kotlin + Jetpack Compose（单 Activity，Material 3）· Room · Retrofit/OkHttp · Coil 2.7（含自定义缓存 Keyer）· Media3 ExoPlayer · WorkManager · 手写依赖注入（无第三方 DI 框架）

## 工作原理

### 认证链路

```
API Key（用户输入，Keystore 加密存储）
   │  POST /auth/auth_code
   ▼
auth_code（24 小时有效，剩余 <1 小时时自动刷新）
   │  拼接在所有媒体 URL 上
   ▼
图片 / 视频请求
```

- API Key **只在登录和验证时**发给服务器换 auth_code，日常请求不携带
- auth_code 存于内存 + 加密存储；离线时回退使用过期码，只为命中磁盘缓存（服务器反正也连不上）
- 无效 API Key 的识别：MT Photo 对无效 Key 也返回 HTTP 201，仅以响应体 `msg` 字段区分，客户端对此做了适配

### 图片缓存机制（离线观看的核心）

| 设计点 | 实现 | 目的 |
|---|---|---|
| 缓存位置 | `cacheDir/image_cache`（App 私有内部存储） | 系统相册、文件管理器、其他 App 均无法读取，**不会被当成图片扫出来** |
| 缓存文件名 | Coil 哈希名、无扩展名 | 即使被导出也不会被系统识别渲染 |
| 缓存上限 | 动态 = 可用空间的 80% | 大库不撑爆存储，超限自动淘汰最旧 |
| 缓存 Key | `StripAuthCodeKeyer`：**剥离 `scheme://host` 前缀和 `auth_code` 参数**，只保留路径+业务参数 | ① 换 IP / 换端口 / 内网穿透地址变化**不丢缓存**；② auth_code 每天都是新的，不剥离则缓存永远 miss |
| 离线兜底 | 网络不可用时用「过期的 auth_code」发请求，Coil 直接命中磁盘缓存 | 断网后看过的照片依然可看 |

限制：视频走服务器在线转码流（非静态 URL），**不进离线缓存**，断网时视频不可播。

### 同步策略（为 9 万张级别的大库设计）

1. **登录时**只同步相册列表和删除日志（增量游标），不拉文件清单
2. **进入相册时**按需拉取该相册的文件列表，与本地快照做增量 diff——翻完一次后二次进入近乎瞬时
3. **后台**由 WorkManager 按用户设定频率（关闭/15 分钟/30 分钟/1 小时/仅手动）轮询删除日志，保证「删除」能在所有相册间生效（5 分钟节流防抖）

### 连接状态监测

- `ConnectionMonitor` 单例：AuthManager 与 SyncManager 的**任何一次成功 HTTP 响应**都视为「服务器可达」；任何连接类异常（无法解析/拒绝/超时）标记「不可达」
- UI 侧收集状态流，不可达时显示顶部横幅「无法连接服务器，正在显示离线内容」+ 重试按钮；恢复后自动消失
- 判定刻意宽松：API Key 错误等**应用层错误不算断连**，避免误报

### 断点续播

- Room 表 `play_positions`：`album_id`（主键）+ `file_id`（当前播放的媒体文件 ID）+ 更新时间
- 按 **file_id** 而非序号记录——刷新导致顺序打乱也能恢复到同一张
- 只保留**最近播放的一个相册**的进度：进入任何相册时清除其他相册的记录（`DELETE WHERE album_id != 当前`），符合「电子相册按内容切换浏览」的实际习惯

### 视频播放

- Media3 ExoPlayer 硬解；播放失败自动重试一次（转码流冷启动或 auth_code 竞态时第二次通常成功）
- 退出视频页先 `stop()` + `clearMediaItems()` 再 `release()`：直接 release 转码中的大视频流可能阻塞主线程数秒
- 控制层可见时才每 500ms 采样上报进度，进度条拖动可 seek

## 项目结构

```
app/src/main/java/io/github/watervxv/mtpadphotos/
├── data/
│   ├── local/db/        # Room：相册、媒体、播放位置、同步状态
│   ├── media/           # CacheManager 等媒体文件操作
│   ├── remote/          # AuthManager（认证）、Retrofit 接口与 DTO
│   ├── repo/            # AlbumRepository 接口与实现（Default/Fake）
│   └── sync/            # SyncManager（同步）、ConnectionMonitor（连接监测）
├── domain/              # SlideshowController（轮播状态机）
├── di/                  # AppContainer（手写依赖注入）
└── ui/                  # Compose 界面：main / slideshow / settings / onboarding
```

## 构建

```powershell
# 需要 JDK 17
.\gradlew.bat assembleDebug          # debug 包，可直接安装使用
.\gradlew.bat assembleRelease        # release 包，需自行配置签名（见下）
```

### 签名配置（可选）

`gradle.properties` 中默认不含签名信息，此时 release 包不签名（仅用于验证构建）。如需出可安装的 release 包，在 `gradle.properties`（或 `~/.gradle/gradle.properties`）中添加：

```properties
RELEASE_STORE_FILE=/path/to/your/release.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_alias
RELEASE_KEY_PASSWORD=your_key_password
```

构建脚本通过 `findProperty` 读取以上属性，未配置时自动降级，不会报错。

## 已知限制

1. 仅支持 MT Photo 作为数据源；接入其他相册软件需重写整个数据层
2. 视频走在线转码流，不进离线缓存（断网时视频不可播）
3. 智能聚合相册（全部项目/照片/视频/往年今日）已移除，仅保留用户相册
4. 大规模库（数万张以上）的同步详情并行数、进度实时化仍有优化空间

## License

[MIT](LICENSE)
