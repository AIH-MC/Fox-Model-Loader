# Fox Model Loader 迁移 NeoForge 计划（无 Architectury 依赖）

更新日期: 2026-06-20  
项目路径: `D:\OYSM\Fox-Model-Loader-Fa1.21.1`  
目标版本: Minecraft 1.21.1 / NeoForge 1.21.1

## 核心决策

**不依赖 Architectury API。** NeoForge 独立项目使用 NeoForge 原生 API 构建，彻底消除 multi-loader 抽象层。

### 理由

- Architectury 事件、`@ExpectPlatform`、`DeferredRegister`、`KeyMappingRegistry`、`ReloadListenerRegistry` 全部有 NeoForge 原生等价物，映射关系一对一。
- 去掉 Architectury 后，项目结构从 `common` + `fabric` 双源码集简化为单一 `src/main/java`。
- `@ExpectPlatform` 桥模式（152 个方法）全部内联为直接调用，不再需要运行时分发。
- 配置系统从 `ForgeConfigSpec`（通过 Forge Config API Port）直接迁移到 NeoForge 的 `ModConfigSpec`。
- 预计额外工作量 3-5 天（不含第三方兼容）。

## 项目结构

```
Fox-Model-Loader-NeoForge-1.21.1/
├── build.gradle                  # NeoGradle 构建脚本
├── settings.gradle
├── gradle.properties
├── src/main/java/
│   ├── com/elfmcys/yesstevemodel/
│   │   ├── YesSteveModel.java              # MOD_ID, init(), 配置注册
│   │   ├── neoforge/
│   │   │   ├── YesSteveModelNeoForge.java  # @Mod 入口
│   │   │   └── YesSteveModelNeoForgeClient.java  # 客户端入口
│   │   ├── capability/                     # Data Attachment 实现
│   │   ├── config/                         # ModConfigSpec 配置
│   │   ├── event/                          # NeoForge 事件注册
│   │   ├── network/                        # NeoForge 网络层
│   │   ├── mixin/                          # 共用 mixin（从 common 复制）
│   │   ├── client/                         # 客户端代码（从 common 复制）
│   │   ├── model/                          # 模型系统（从 common 复制）
│   │   └── ...                             # 其余 common 代码
│   └── rip/ysm/
│       ├── api/                            # API（@ExpectPlatform 内联后）
│       └── compat/                         # 第三方兼容（Phase 6 延后）
├── src/main/resources/
│   ├── META-INF/neoforge.mods.toml
│   ├── yes_steve_model.mixins.json        # 共用 mixin 配置
│   ├── yes_steve_model_neoforge.mixins.json
│   ├── assets/yes_steve_model/            # 资源文件（从 common 复制）
│   ├── data/yes_steve_model/              # 数据文件（从 common 复制）
│   └── natives/                           # 原生库（从 common 复制）
└── libs/                                  # 第三方 jar（暂不迁移）
```

## 迁移映射表

### 1. @ExpectPlatform → 直接内联

| 原桥类 | 内联策略 |
|---|---|
| `YSMChannel` (9 方法) | 全部替换为 NeoForge 网络层直接实现 |
| `ConfigRegistration` (1 方法) | 替换为 `ModContainer#registerConfig` |
| `PlatformAPI` (2 方法) | `FMLEnvironment.dist` + `ModList` |
| `ForgeAttributes` (6 方法) | NeoForge 1.21.1 的 `NeoForgeMod` 属性 |
| `CapabilityLifecycle` (2 方法) | NeoForge Data Attachment 无需 revive/invalidate |
| `KeyMappingFactory` (3 方法) | 直接 `new KeyMapping()` |
| `BufferBuilderBridge` (2 方法) | Fabric 上全部返回 false，NeoForge 同理 |
| `RenderLivingBridge` (2 方法) | Fabric 上全部返回 false/空，NeoForge 同理 |
| `EntityDataBridge` (2 方法) | NeoForge `Entity` 有 `getPersistentData()` |
| `ToolActionBridge` (2 方法) | 直接实现，无平台差异 |
| `WeaponActionBridge` (1 方法) | 直接实现，无平台差异 |
| `PlayerDataSaveBridge` (1 方法) | NeoForge `ServerPlayer#save()` |
| Capability `get()` 方法 (×7) | 替换为 Data Attachment `entity.getData()` |

### 2. Architectury 事件 → NeoForge 事件

| 原 Architectury API | NeoForge 等价物 | 文件 |
|---|---|---|
| `LifecycleEvent.SETUP` | `FMLCommonSetupEvent` | CommonEvent.java |
| `LifecycleEvent.SERVER_BEFORE_START` | `ServerStartingEvent` | ServerStartupEvent.java |
| `PlayerEvent.PLAYER_JOIN` | `PlayerEvent.PlayerLoggedInEvent` | EnterServerEvent.java |
| `PlayerEvent.PLAYER_QUIT` | `PlayerEvent.PlayerLoggedOutEvent` | PlayerLogoutEvent.java |
| `PlayerEvent.PLAYER_CLONE` | `PlayerEvent.Clone` | CapabilityEvent.java |
| `EntityEvent.ADD` | `EntityJoinLevelEvent` | CapabilityEvent.java |
| `TickEvent.SERVER_POST` | `ServerTickEvent` | CapabilityEvent.java |
| `CommandRegistrationEvent.EVENT` | `RegisterCommandsEvent` | CommandRegistry.java |
| `ClientLifecycleEvent.CLIENT_STARTED` | `FMLClientSetupEvent` (延迟) | ClientSetupEvent.java |
| `ClientTickEvent.CLIENT_PRE` | `ClientTickEvent` (NeoForge) | ClientTickEvent.java |
| `ClientPlayerEvent.CLIENT_PLAYER_JOIN` | `ClientPlayerNetworkEvent.LoggingIn` | ClientPlayerJoinNotification.java |
| `ClientPlayerEvent.CLIENT_PLAYER_QUIT` | `ClientPlayerNetworkEvent.LoggingOut` | ClientPlayerJoinNotification.java |
| `ClientPlayerEvent.CLIENT_PLAYER_RESPAWN` | `ClientPlayerNetworkEvent.Clone` | ClientPlayerCloneEvent.java |
| `ClientRawInputEvent.KEY_PRESSED` | `InputEvent.Key` | 7 个 input 文件 |
| `ClientRawInputEvent.MOUSE_CLICKED_PRE` | `InputEvent.MouseButton.Pre` | InputStateKey.java |
| `ClientCommandRegistrationEvent.EVENT` | `RegisterClientCommandsEvent` | CommandRegistry.java |
| `HudRenderCallback.EVENT` | `RenderGuiOverlayEvent.Post` | YesSteveModelNeoForgeClient.java |
| 自定义 `SpecialPlayerRenderEvent` | NeoForge `Event<>` + `EventFactory` | SpecialPlayerRenderEvent.java |

### 3. 其他 API 替换

| 原 API | NeoForge 替换 |
|---|---|
| `DeferredRegister` (Architectury) | `DeferredRegister` (NeoForge) |
| `RegistrySupplier` | `DeferredHolder` / `Supplier` |
| `KeyMappingRegistry.register()` | `RegisterKeyMappingsEvent` 注册 |
| `ReloadListenerRegistry.register()` | `AddReloadListenerEvent` 注册 |
| `Platform.getConfigFolder()` | `FMLPaths.CONFIGDIR.get()` |
| `Platform.getModVersion()` | `ModList.getModContainerById().getVersion()` |
| `GameInstance.getServer()` | `ServerLifecycleHooks.getCurrentServer()` |
| `ForgeConfigSpec` | `ModConfigSpec` (NeoForge) |
| `ModConfig.Type` (Forge) | `ModConfig.Type` (NeoForge `net.neoforged.fml.config`) |
| Cardinal Components | NeoForge Data Attachments |
| `EventResult.pass()` | `Event.Result.DEFAULT` |

### 4. Cardinal Components → NeoForge Data Attachments

| CCA Component | Attachment Type | 注册 |
|---|---|---|
| `StarModelsComponent` | `AttachmentType<StarModelsCapability>` | `NeoForgeRegistries.ATTACHMENT_TYPES` |
| `AuthModelsComponent` | `AttachmentType<AuthModelsCapability>` | 同上 |
| `ModelInfoComponent` | `AttachmentType<ModelInfoCapability>` | 同上 |
| `ProjectileModelComponent` | `AttachmentType<ProjectileModelCapability>` | 同上 |
| `VehicleModelComponent` | `AttachmentType<VehicleModelCapability>` | 同上 |

- `RespawnCopyStrategy.ALWAYS_COPY` → `copyOnDeath(true)` + `copyOnDimensionChange(true)`
- 客户端 `PlayerCapability` 等 transient 数据保持 `WeakHashMap` store（与 Fabric 一致）

## 分阶段计划

### Phase 0: 项目骨架 ✓
- 创建 `Fox-Model-Loader-NeoForge-1.21.1` 目录
- `build.gradle` (NeoGradle)、`settings.gradle`、`gradle.properties`
- `neoforge.mods.toml`、`@Mod` 入口
- 复制 common 代码、资源、natives、mixin 配置
- **验收**: `./gradlew compileJava` 产生平台缺口编译错误（非构建脚本错误）

### Phase 1: 配置系统迁移
- `ForgeConfigSpec` → `ModConfigSpec`
- `ConfigRegistration` 内联 → `ModContainer#registerConfig`
- **验收**: 配置文件正确生成

### Phase 2: 网络层迁移
- `YSMChannel` 全部 9 个方法内联为 NeoForge 实现
- `PayloadRegistrar` 注册 `YSMPayload`
- `PacketDistributor` 替换发送方法
- **验收**: `compileJava` 通过

### Phase 3: Data Attachments 替换 Cardinal Components
- 注册 5 个 `AttachmentType`
- 替换所有 `ComponentKey.get(entity)` → `entity.getData()`
- **验收**: 玩家数据持久化、死亡复制正常

### Phase 4: 事件系统迁移
- 所有 Architectury 事件替换为 NeoForge 原生事件
- `@ExpectPlatform` 注解全部移除，方法体替换为直接实现
- **验收**: 进入主菜单、进入世界、HUD 显示正常

### Phase 5: 客户端体验补齐
- HUD overlay 注册
- Key mapping 注册
- 声音系统迁移
- **验收**: 完整客户端功能可用

### Phase 6: 第三方兼容（延后）
- 逐个确认 NeoForge 1.21.1 可用依赖
- 先做无外部 API 的兼容
- **验收**: 不阻塞核心 mod 发布

## 风险清单

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| NeoForge 1.21.1 API 变化 | 中 | 以官方文档为准，逐个验证 |
| 网络 payload 大小限制 | 中高 | 保留 chunk 机制，显式检查大小 |
| Mixin 在 NeoForge 下行为差异 | 中 | 逐个测试，必要时重写 |
| 第三方兼容依赖缺失 | 高 | MVP 不含，逐个确认后再迁移 |
| 客户端类在专服加载 | 高 | `@EventBusSubscriber(Dist.CLIENT)` 分离 |
