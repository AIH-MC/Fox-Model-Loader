# NeoForge 独立迁移精简计划书

> 项目：`D:\OYSM\openysm-26.1.2`  
> 范围：将 Fabric 版迁移为独立 NeoForge 项目  
> 结论：应另起独立项目，不建议在当前 Fabric 工程内做混合加载器。

## 一、目标

将 Fox Model Loader 迁移为独立 NeoForge 项目，而不是在当前 Fabric 项目中混合 Fabric / NeoForge。

## 二、现状依据

当前仓库是 Fabric-only：

```text
settings.gradle 只 include fabric
root build.gradle 对 subprojects 统一应用 fabric-loom
fabric/build.gradle 依赖 Fabric API、Cardinal Components API、Forge Config API Port
common 内存在 Fabric / Architectury shim
Platform 直接依赖 FabricLoader
fabric.mod.json、Fabric entrypoint、Fabric mixin 配置都在 fabric 子项目
```

因此不建议在当前工程内新增 `neoforge` 子项目。

## 三、推荐结构

```text
Fox-Model-Loader/
    保留 Fabric 版

Fox-Model-Loader-NeoForge/
    独立 NeoForge 版
```

NeoForge 版使用标准单加载器结构：

```text
src/main/java
src/main/resources
META-INF/neoforge.mods.toml
yes_steve_model.mixins.json
```

## 四、实施阶段

### P0：空 NeoForge 项目

1. 新建独立目录。
2. 使用 NeoForge MDK / ModDevGradle。
3. 配好 `settings.gradle`、`build.gradle`、`gradle.properties`。
4. 新增空 `@Mod` 主类。
5. 新增 `neoforge.mods.toml`。
6. 新增空 mixin json。
7. `runClient` 能进主菜单。

不要一开始复制全部源码。

### P1：复制 common 并清理依赖

1. 复制 `common/src/main/java` 到 `src/main/java`。
2. 复制 `common/src/main/resources` 到 `src/main/resources`。
3. 删除或替换：

```text
net.fabricmc.*
net.fabricmc.fabric.api.*
org.ladysnake.cca.*
dev.architectury.* shim
Forge Config API Port
Fabric entrypoint
fabric.mod.json
```

4. 编译失败按模块分类，不要一次性硬修所有错误。

### P2：平台能力替换

按顺序迁移：

```text
配置系统 -> NeoForge 原生 Config
数据组件 -> NeoForge Data Attachments
网络 -> NeoForge Payload Networking
事件 -> NeoForge Event Bus
命令 -> NeoForge command registration
资源重载 -> NeoForge reload listener
```

先跑通最小闭环：

```text
客户端选择模型
-> 服务端收到
-> 服务端保存/广播
-> 其他客户端显示变化
```

### P3：渲染与 Mixin

逐个启用：

```text
玩家第三人称渲染
第一人称手部渲染
键盘输入
鼠标输入
HUD
声音
实体效果
```

每启用一个 mixin，都运行客户端和服务端测试。

## 五、高风险点

```text
Cardinal Components API -> Data Attachments
玩家死亡/克隆/重进后的模型数据保留
Payload 注册和同步时机
专用服务器误加载 client-only 类
Mixin 目标签名变化
渲染线程和 OpenGL 调用位置
zstd-jni / ImageStream 打包
```

## 六、验收标准

```text
runClient 可进入主菜单
runServer 可启动专用服务器
配置文件正常生成
模型选择 UI 可打开
客户端 A 换模型，客户端 B 可见
玩家退出重进后模型保留
死亡/重生后模型数据保留
第三人称和第一人称渲染正常
专用服务器无 MinecraftClient 类加载错误
```

## 七、明确不做

```text
不在原 Fabric 工程里混合 NeoForge
不继续保留 Fabric API / CCA / Forge Config API Port
不试图同时兼容 Fabric 和 NeoForge
不一次性启用所有 Mixin
```

