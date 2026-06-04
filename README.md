<div align="center">
  <img src="images/144725232_p0.png" alt="Fox Model Loader banner"/>
  <p>图片作者：「pixiv」<a href="https://www.pixiv.net/users/76344429">法师来自未来</a></p>
  <h1>Fox Model Loader / 绯绯狐的模组加载</h1>
  <p>基于 <a href="https://github.com/OpenYSMDev/OpenYSM">OpenYSM</a> 的 Minecraft 自定义玩家模型加载器</p>
  <p>支持 Minecraft 26.1.x、Fabric 与 NeoForge</p>
</div>

## 项目简介

Fox Model Loader 是一款基于 OpenYSM 构建的 Minecraft 模组，可以将原版玩家模型替换为带完整动画的自定义模型。模组使用 Minecraft 基岩版模型与动画文件（`.ysm`），并借助 GeckoLib 实现复杂动画播放，让每位玩家都能拥有独立的角色外观、动作和第一人称手臂表现。

本项目专注于将 OpenYSM 迁移到 Minecraft 1.21+ 与 26.1.x 平台，并持续适配新版客户端的渲染、输入、动画与资源管理变化。

## 主要特性

### 自定义玩家模型

- 加载 `.ysm` 基岩版格式模型，用于替换默认 Steve / Alex 玩家模型。
- 每位玩家可以独立选择模型与纹理。
- 支持在不同模型、纹理和分类之间切换。

### 完整动画系统

- 基于 Molang 表达式的动画引擎，支持复杂状态机。
- 内置行走、奔跑、跳跃、潜行、游泳、飞行、滑翔等移动动画。
- 支持剑、斧、镐、锹、锄、重锤、三叉戟、长矛等武器姿势。
- 支持拉弓、进食、饮用药水、格挡、投掷等使用动画。
- 主手与副手动画可独立响应，右键行为匹配原版逻辑。
- 可按手持物品、穿戴装备、骑乘实体、当前维度等条件触发自定义动画。

### 内置资源站

- 在模型选择界面中浏览远程模型仓库。
- 支持 `.ysm` 与 `.zip` 格式下载。
- 支持搜索、筛选、下载队列和已完成任务管理。
- 内置 CDN 加速，便于快速获取模型资源。

### 模型分类管理

- 支持创建、重命名、删除分类文件夹。
- 支持批量在分类之间移动模型。
- 删除分类时可选择保留或移除其中内容。
- 操作完成后自动刷新缓存。

### 实时模型预览

- GUI 中提供实时 3D 预览。
- 支持旋转、缩放和查看模型细节。

### 第一人称手臂

- 将原版第一人称手臂替换为与当前模型匹配的自定义手臂。
- 支持独立左手、右手骨骼。
- 可同步手持物品与动画。

### 联机同步

- 客户端切换模型后会自动同步到服务端。
- 同一服务器中的其他玩家可以看到对应的自定义模型。
- 服务端可配置模型上传权限与文件大小限制。

### Android 支持

- 支持 PojavLauncher、FCL、Zalith Launcher。
- 内置模型选择器与文件导入功能。
- 提供与桌面端一致的模型选择和使用体验。

## 更新：1.2.2

本次更新主要修复部分模型下的 Elytra 显示问题，并优化模组菜单与下载站界面的使用体验。

- 修复某些模型下可能出现“双鞘翅”的显示问题。
- 在模组菜单中新增“设置”按钮，方便快速进入配置界面。
- 下载站 UI 会根据窗口大小自动缩放与适配，提升不同分辨率下的浏览体验。

## 兼容性

| 项目 | 支持情况 |
| ---- | ---- |
| 模组版本 | 1.2.2 |
| Minecraft | 26.1.x |
| Java | 25 |
| Fabric | 支持 |
| NeoForge | 支持，自 1.2.0 起 |
| Android 启动器 | PojavLauncher / FCL / Zalith Launcher |

## 构建产物

- Fabric: `fox-model-loader-1.2.2-fa26.1.x.jar`
- NeoForge: `fox-model-loader-1.2.2-neo26.1.x.jar`

## 源码与反馈

本项目基于上游 OpenYSM 迁移：

- 上游源码: https://github.com/OpenYSMDev/OpenYSM
- 反馈 QQ 群: 1104823534

YSM 与 OpenYSM 之间的过往争议并非本项目重点。Fox Model Loader 只专注于将 OpenYSM 迁移至新版 Minecraft 平台，并为社区提供可用的自定义玩家模型加载工具。请勿在本仓库中讨论与开发无关的争议话题。

## 迁移历程

本项目经历了两个主要迁移阶段：

- 从 OpenYSM 1.20.1 迁移至 Minecraft 1.21.1 Fabric。
- 从 Minecraft 1.21.1 迁移至 Minecraft 26.1.2 Fabric。

### 阶段一：1.20.1 到 1.21.1

驱动引擎：DeepSeek V4 Pro（1M 上下文） + Claude Code

Minecraft 1.21.1 对游戏 API 做了较大调整，迁移过程主要涉及：

- Mixin 目标方法签名变化，需要逐一适配。
- 核心类字段与方法重命名，引发连锁编译错误。
- Fabric Loom、Gradle 插件和 Fabric API 等依赖升级。

最终完成编译与运行时问题修复，游戏可以正常启动并加载自定义模型。

#### 1.21.1 构建环境

| 组件 | 版本 |
| ---- | ---- |
| Java | 21 |
| Fabric Loom | 1.8-SNAPSHOT |
| Fabric Loader | 0.16.x |
| Fabric API | 1.21.1 对应版本 |

### 阶段二：1.21.1 到 26.1.2

驱动引擎：GPT-5.5 + Codex

Minecraft 26.x 对渲染、输入、实体、NBT、Registry 与构建环境做了大量破坏性改动，迁移难度明显高于 1.21.1。

主要工作包括：

- 将 `GuiGraphics` 相关渲染代码迁移到 `GuiGraphicsExtractor`，并适配新的渲染方法命名。
- 适配新版 `EntityRenderer` 类型参数、`extractRenderState()` 与 `createRenderState()`。
- 修复模型骨骼绑定、纹理映射、动画状态机在新 API 下的兼容问题。
- 适配新版鼠标和键盘输入事件对象。
- 修复 `AbstractArrow`、`Boat` 等实体类包路径变化导致的 import 问题。
- 适配 NBT 与 Registry API 中 Optional 返回值和键集合访问方式变化。
- 在缺少官方 Mojang 映射的情况下，通过 ASM 9.9 字节码分析工具生成 tiny v2 映射文件。
- 将 Fabric Loom 升级至 1.17.0-alpha.8，以支持 Java 25 字节码。
- 在 Architectury API 暂无 26.x 支持时，创建必要 stub 类补齐平台抽象。

最终解决 200+ 个编译错误和 40+ 个运行时崩溃，游戏可正常启动并加载模型。

#### 26.1.2 构建环境

| 组件 | 版本 |
| ---- | ---- |
| Java | 25 (Azul Zulu 25.0.2) |
| Gradle | 9.5.1 |
| Fabric Loom | 1.17.0-alpha.8 |
| Fabric Loader | 0.19.2 |
| Fabric API | 0.149.1+26.1.2 |
| Shadow 插件 | 9.0.0-beta4 |

## 迁移经验

1. Stub 只应为目标 JAR 中确实不存在的类创建，真实存在的类必须使用真实实现。
2. 方法签名需要精确匹配到包名和参数类型，不能用 `Object` 粗略替代。
3. `javap` 和字节码检查比猜测 API 更可靠。
4. 相同类型错误应优先批量修复，避免逐个手动处理。
5. 渲染管线、模型渲染和输入事件系统是 26.x 迁移中最需要重点验证的部分。
