# Yes Steve Model / fox-model-loader 26.1.2 修复记录

> 更新时间：2026-05-29 | 整体状态：全部完成 | 产物：`fabric/build/libs/fox-model-loader-fabric-1.0.3.jar` | JDK 25 + Gradle 9.5.1

## 阶段摘要 (13 项，均已完成)

| 日期 | 主题 | 要点 |
|------|------|------|
| 05-21 | MC 26 基础迁移 | 网络、渲染、tick/命令、第一人称手臂、滑翔组件适配 |
| 05-24 | GUI/动画/同步 | 动画轮盘、GUI 预览、HUD 字体、Mixin 签名、LAN 同步 |
| 05-25 | 模型面板/交互 | 导入入口、手持预览、元数据、投射物同步、tooltip、配置保存 |
| 05-25 | 物品/姿态 | 三叉戟/矛分流、剑/弓/船动画回归 |
| 05-26 | GPU direct GL | 修复崩溃/矩阵错位/空 mesh/贴图绑定；仍为实验路径 |
| 05-26 | 船/竹筏/箱船 | 禁用 YSM 对 `AbstractBoat` 载具替换，保留 Molang 变量 |
| 05-28 | Android 模型面板 | 修复内置/杂项/导入模型不可见 (zstd-jni so 加载失败) |
| 05-29 | HeadlessException | Android 跳过 AWT/Swing 文件选择器 |
| 05-29 | FCL 目录桥接 | 接入 `CallbackBridge.nativeClipboard(2002, ...)` 目录面板 fallback |
| 05-29 | Zalith 2 目录桥接 | 同上；修复取消后选择器状态残留 |
| 05-29 | .ysm 选择器/桌面 | 多选、tinyfd 桌面选择器、取消状态释放、暂存清理 |
| 05-29 | 导入/工具/鞘翅 | 导入扩展 .ysm/.zip/文件夹；工具动作拆分 pickaxe/spade/hoe/axe；默认模型跳过 YSM 鞘翅 |
| 05-31 | 性能 P0/P1/P2 与实验性测试设置 | 模型内存 profiler、纹理上传后可选释放 byte[]、GPU 状态缓存、不透明纹理跳过透明 pass、fallback 临时对象复用、GPU/native cache LRU、配置界面新增“实验性测试”分组 |

## 关键结论

- **ZSTD**：Android 不依赖 native，走 raw ZSTD frame + Java fallback；远端 `openysm.cpp` 仅作参考。
- **GPU**：GUI/第一人称/手持优先回退普通流程；世界渲染用 `Camera.getViewRotationProjectionMatrix()`；实验功能，异常时确认 fallback。2026-05-31 已增加 GPU 路径状态缓存，并让纯不透明纹理跳过透明 pass。
- **动画/载具**：三叉戟优先 `trident`→回退 `spear`；矛优先 `lance`；工具分类先查 `ItemTags`；`AbstractBoat` 不再替换。
- **导入链路**：客户端→服务端校验 (id/size/sha256)→写入 `custom/`→`loadModels()` 重扫。支持 `.ysm`、`.zip`、文件夹；`.7z` 跳过。

## 2026-05-31 性能与实验性测试设置

- 版本号调整为 `1.2.0`，导出产物为 `fabric/build/libs/fox-model-loader-fabric-1.2.0.jar`。
- 新增 `ModelMemoryProfiler`，默认关闭；覆盖缓存读取、解密、二进制解析、客户端映射、Assembly 构建、纹理解码/上传、GPU mesh 构建/释放、GPU cache LRU 释放等检查点。
- `OuterFileTexture` 支持配置开启后在 GPU 上传成功时释放 Java 侧图片 `byte[]`，用于压低 heap 常驻。
- 模型替换、服务端同步移除、LRU cache trim 路径统一释放纹理、native cache、GPU mesh/direct buffer。
- GPU renderer 路径新增轻量 GL 状态缓存，减少重复 program、texture、sampler、SSBO 绑定；已有半透明扫描结果会用于纯不透明纹理跳过透明 pass。
- Java fallback 路径复用矩阵、向量、骨骼数组 scratch，减少每帧临时对象分配。
- 新增保守 P2 LRU：只释放未使用模型的 GPU/native render cache，不移除 `ModelAssembly` 本体；GUI 打开时不淘汰，当前世界玩家使用中的模型和 `default` 模型不淘汰。
- 配置界面新增 `实验性测试` 分组，包含：
  - `模型内存分析日志`
  - `纹理上传后释放字节缓存`
  - `GPU 缓存模型上限`
  - `未使用模型缓存保留时间`
- `GPU 渲染器` 选择保持在原 `性能` 分组，不放入 `实验性测试`。
- P3 评估结论：优先考虑共享 `BoneUploadScratch` / `BoneRingBuffer`；暂不建议直接释放完整 CPU baked mesh、重写 GeoModel compact 或推进 Compute Skinning，需先基于多人多模型 profiler 日志确认瓶颈。

## FML 武器动作进度

- 2026-05-29 已保存到 `D:\OYSM\FML-长矛-三叉戟-重锤动作彻底修复计划书.md`：除“第一人称”和“长矛第三人称动作适配”外，其余项目标记为已完成。
- 2026-05-29 第一人称评估已封存：当前第一人称走独立 `fp.arm`/手臂渲染链路，只渲染左右手臂 mesh；第三人称手持武器依赖 `CustomPlayerItemInHandLayer` 按手骨/locator 挂载物品，第一人称未接入该层。无需改代码时，只能通过 `fp_arm` 资源补手臂动作，或依赖真实第一人称/Real Camera 类外部方案复用第三人称模型；“第一人称完全像第三人称一样显示武器和动作”暂不处理。

## Android 修复要点

- **内置模型不可见** → zstd-jni so 不兼容 Android bionic；改为 raw ZSTD frame 缓存，跳过 `zstd-jni`，优先 raw-frame 解压再 Java fallback。
- **导入模型不可见** → 沙箱 `Permission denied`；新增客户端上传→服务端写入流程，配置 `AllowModelUpload`/`ModelUploadMaxMiB`。
- **文件选择** → 优先 FCL `ACTION_OPEN_DOCUMENT`→AndroidX `GetMultipleContents`→FCL `FileBrowser`；不可用时走启动器目录桥接 (`nativeClipboard 2002`)；桌面优先 tinyfd→AWT/Swing。
- **HeadlessException** → `isHeadless()` 检查 + Android 跳过 JVM 对话框；无桥时返回 `no_android_picker`。
- **多选** → AndroidX `GetMultipleContents`、`EXTRA_ALLOW_MULTIPLE`、`ClipData`；桌面 AWT `setMultipleMode`/Swing `setMultiSelectionEnabled`；Zalith 2 `OpenFolder.kt` MIME 改为 `*/*`。
- **状态残留** → `pickYsmFile()` 可替换旧请求；`removed()` 调 `cancelPicking()`；暂存目录打开前清理 + 读取后删除。

## Zalith 2 APK

- `:ZalithLauncher:assembleDebug` 成功 (`GRADLE_USER_HOME=D:\Tools\GradleHome`)
- 产物：`ZalithLauncher-Debug-2.4.4.apk` (344 MB, arm64/x86 多架构)
- SHA256: `0A1DEEC2285C0958246E72BDC694ACED84766039FFB3003DA49E75F35A8723AE`

## 使用建议

- Android 换 jar 后清理 `config/yes_steve_model/cache/server` 和 `cache/client`。
- Android 优先用导入界面 `Choose .ysm File(s)`；桌面可拖拽导入。
- 不要手动复制已报 `Permission denied` 的 `.ysm` 文件。
- GPU 异常时优先确认 fallback 是否生效；Zalith 2 目录面板 Import 需新 APK。

## 2026-06-01 资源站 WebView 引擎评估封存

- 结论：可以为资源站增加 WebView 引擎，但不建议作为主模组默认能力；更适合作为实验性、客户端可选、可回退的资源站增强入口。
- 当前稳定默认实现应继续保留原生 `ResourceStationScreen`，因为现有资源站已接入模型选择入口、URL 管理、GitHub/index.json 列表、预览图、下载队列、失败重试，并复用 `ClientModelManager.importLocalModel` 与 `ModelUploadSession` 导入/上传链路。
- 桌面端若需要浏览器能力，优先评估 MCEF/JCEF 类独立 Chromium 方案；但这会引入 native 体积、版本兼容、启动下载、输入焦点、渲染线程和长期安全维护成本，不宜无条件打进默认路径。
- `system WebView` 不适合作为 Fabric/OpenYSM 通用桌面方案：Windows 需 WebView2，macOS 需 WKWebView，Linux 通常需 WebKitGTK，三端都需要 native/JNI 与窗口句柄适配，维护成本高于资源站功能本身。
- Android/FCL/Pojav 场景理论上可以通过启动器或 Android 层桥接调用系统 WebView，但它应只作为 Android 特化实验能力；必须有无桥接时的原生资源站回退。
- WebView 只能负责浏览和选择资源，不能直接写模型目录或绕过校验导入。真正下载、体积限制、扩展名限制、modelId 安全归一化、导入与上传仍必须回到现有 Java 侧 `ModelRepoClient` / `ModelUploadSession` 链路。
- 推荐架构：新增 `ResourceStationEngine` 抽象，默认 `NativeResourceStationEngine` 使用当前原生 GUI；实验 `WebViewResourceStationEngine` 仅在依赖存在、平台支持、配置开启时启用。
- WebView 与 Java 侧通信建议使用受限 URL scheme 或白名单 bridge，例如 `ysm://download?...`；Java 侧仍需校验 `https`、`.ysm`/`.zip`、最大下载体积、来源域名/manifest，不持久化敏感 cookie，并限制外链跳转。
- 落地顺序建议：先做最小 spike，检测 MCEF 或 Android bridge 是否存在，打开固定测试页，页面按钮只触发现有下载/导入队列；spike 通过后再决定是否投入完整 WebView 资源站。
