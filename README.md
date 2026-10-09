# MT padPhotos（MT轮播相册）

把一台闲置的安卓平板改造成**常驻的物理电子相册**：设备长期放在家里滚动展示 NAS 相册库中的照片和视频，支持开机自启、自动轮播、后台同步与离线观看。

> **本项目是 MT Photo（NAS 相册软件）的专用客户端，不是通用相册应用。** 认证体系（API Key → auth_code）、数据接口（`/api-album/filesV2`、`/gateway/*`、`/file-delete-log`）与媒体画质规格（s260/proxy 等）均直接建立在 MT Photo OpenAPI 之上，离开 MT Photo 无法工作。
>
> 📘 **使用手册（含真机截图）**：[docs/使用说明书.md](docs/使用说明书.md)

## 功能特性

1. **开机自启直达轮播**：开机自动拉起 App，直接进入上次播放的相册并从上次位置继续
2. **照片视频混合轮播**：照片按设定时长（默认 5 秒）自动切换；视频完整播放后自动续播下一项；默认静音、可一键取消静音
3. **播放控制**：三种顺序（正向/倒序/随机）、四种过渡（淡入淡出/滑动/硬切/肯伯恩斯）、手势翻页、播放列表多选删除与撤销、拖动排序
4. **相册管理**：展示账号下全部相册（含数量）、下拉刷新、全局搜索
5. **自动同步**：登录后同步相册列表与删除日志；进入相册时按需拉取文件清单并与本地快照做增量 diff；WorkManager 按设定频率后台同步（5 分钟节流）
6. **离线观看**：图片磁盘缓存（可用空间的 80%，缓存 key 与服务器地址解耦并剥离 auth_code）+ 过期 auth_code 兜底，断网后看过的照片依然可看
7. **连接状态提示**：服务器连不上时顶部红色横幅提示并保留离线内容，恢复后自动消失，支持手动重试
8. **服务器配置随时改**：设置页可直接修改 NAS 地址与 API Key（先验证后保存、失败自动回滚），IP 漂移不再需要重装或重登
9. **安全与适配**：API Key 加密存储（EncryptedSharedPreferences）、PIN 锁（仅冷启动）、国产 ROM 自启动引导（小米等）、首次使用教程

## 运行环境

| 项 | 值 |
|---|---|
| 目标设备 | 安卓平板（参考设备：Helio G99 / 4GB RAM / 10.61" 2000×1200 横屏） |
| 系统 | Android 12+（真机验证于 Android 14） |
| 网络 | 与 NAS 同局域网 |
| 服务端 | **MT Photo**（需开启 API，使用管理员 API Key 登录） |

## 技术栈

Kotlin + Jetpack Compose（单 Activity，Material 3）· Room · Retrofit/OkHttp · Coil 2.7（含自定义缓存 Keyer）· Media3 ExoPlayer · WorkManager · 手写依赖注入（无第三方 DI 框架）

**认证链路**：API Key 换取 `auth_code`（24 小时有效、剩 1 小时内自动刷新），所有媒体 URL 携带 auth_code；离线时回退过期码命中磁盘缓存。

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
