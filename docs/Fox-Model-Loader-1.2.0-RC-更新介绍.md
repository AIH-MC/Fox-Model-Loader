# Fox Model Loader 1.2.0 RC 更新介绍

**版本：** 1.2.0 RC  
**构建日期：** 2026-06-01  
**适用 Minecraft：** 26.1.2  
**加载器：** Fabric  

---

## 📋 更新概览

Fox Model Loader 1.2.0 RC 是一个以**稳定性、性能可观测性和资源站功能**为核心的大型更新。本轮更新围绕三条主线展开：动画诊断基线建设、资源站（实验性）完整功能上线、以及模型分类面板新增。

---

## 🆕 新功能

### 资源站（实验性）

模型选择界面新增「资源站」入口，支持浏览远程模型资源库，一键下载并导入 `.ysm` / `.zip` 模型文件。

- **多源支持**：兼容 OpenYSM `index.json` 格式和 GitHub 仓库地址（根地址、`tree/branch`、`tree/branch/path`）
- **默认资源站**：内置 [Elaina69/Yes-Steve-Model-Repo](https://github.com/Elaina69/Yes-Steve-Model-Repo) 和 [sdf123098/YSM-Model](https://github.com/sdf123098/YSM-Model) 两个默认源
- **URL 管理**：支持添加、删除、保存资源站地址，上一个/下一个源快速切换
- **搜索与排序**：支持按名称、文件名、描述、作者、标签搜索过滤；支持按名称/大小/来源排序
- **元数据展示**：列表显示资源大小、作者、标签、描述；预览图异步下载并显示
- **下载队列**：支持单个资源入队或当前过滤结果全部入队，串行执行避免 `ModelUploadSession` 并发冲突，显示队列数/完成数/失败数，失败任务可重试
- **中国大陆加速**：GitHub API 默认优先尝试代理前缀（`gh.llkk.cc`、`gh-proxy.com`、`gh.ddlc.top`），Raw 文件下载优先走 jsDelivr CDN，失败后逐级回退
- **安全设计**：禁止路径穿越、限制下载体积、中文文件名仅作显示用，modelId 通过安全归一化生成

> **注意**：资源站下载的模型通过 `ModelUploadSession` 导入，在远程服务器场景下不会误写客户端本地 custom 目录，而是走服务端校验→写入→重载→同步的标准链路。

### 动画帧分析器（AnimationFrameProfiler）

新增动画帧级诊断工具，用于定位动画卡顿、低帧感等问题的根因。

- **Animation Frame Profiler**：收集每帧动画的 `tickCount`、`partialTick`、`frameTime`、`renderTickTime`、`seekTime`、`boneCount`、`controllerCount`、`costMs` 等诊断数据
- **动画详细调试日志**：为每次动画计算输出详细 `[YSM-ANIM]` 前缀日志（日志量大，仅建议短时间测试开启）
- **重复动画计算警告**：检测同一实体在同一渲染帧内是否重复 evaluate，输出警告

配置项位于 **设置 → 实验性测试**：
- `AnimationFrameProfiler`（动画帧分析）
- `动画详细调试日志`
- `重复动画计算警告`

### 模型内存分析器（ModelMemoryProfiler）

新增模型全生命周期内存检查点日志，覆盖读取、解密/解压、解析、纹理解码、纹理上传、GPU Mesh 构建、加入缓存、卸载等阶段。配置默认关闭，仅建议测试时开启。

### 模型分类面板

模型选择界面新增完整的分类管理功能，支持对模型进行分组整理：

- **新建分类**：创建自定义分类文件夹，将模型归类管理
- **分类改名**：重命名已有分类；若目标分类已存在，自动合并而非生成带后缀的新分类
- **分类删除**：删除分类时弹出确认界面，提供「保留模型」（将模型移至上级目录后删除空分类）和「删除模型」（递归删除分类及其中所有模型）两个选项
- **移动模型**：将选中的模型批量移动到指定分类
- **实时刷新**：分类的改名、删除操作会同步更新内部缓存，移除旧路径下已加载模型并触发本地模型重载

### 新增配置项

| 配置项 | 说明 |
|--------|------|
| `model_memory_profiler` | 模型内存分析日志 |
| `animation_frame_profiler` | 动画帧分析 |
| `animation_debug_log` | 动画详细调试日志 |
| `warn_repeated_animation_evaluation` | 重复动画计算警告 |
| `release_texture_bytes_after_upload` | 纹理上传后释放 Java 侧 byte[]，降低 heap 常驻 |
| `max_cached_gpu_models` | GPU 缓存模型上限（LRU 卸载） |
| `unused_model_ttl_seconds` | 未使用模型缓存保留时间 |

---

## 🐛 问题修复

### 右键双手挥动修复

修复了副手持药水等物品时，主手持剑、斧、镐、锹、锄、重锤右键会出现双手同时挥动的问题。

**修改内容**：原先仅对「副手盾牌 + 主手工具/武器」跳过合成右键挥手，现在扩展为「副手非空 + 主手为右键兜底工具/武器」统一跳过合成主手挥手，覆盖所有 ItemTags 标签。

### 联机模型同步修复

彻底修复了联机时，客户端查看其他玩家模型只显示为模组默认模型的问题。现在其他玩家的自定义模型能够正常同步和渲染。

---

## ⚠️ 已知限制与不做事项

- 不使用 `System.gc()` 作为核心内存方案
- 不删除 GPU Renderer / Native Renderer / Java fallback 任何一条渲染路径
- 不在后台线程调用 OpenGL
- 不做 GitHub 登录/token、评分评论收藏、断点续传
- 不新增 Mixin / Coremod / ASM
- 动画帧分析器仅为诊断基线，不改变动画行为本身

---

## 🔧 构建信息

```
构建产物：fox-model-loader-fabric-1.2.0.jar
构建命令：.\gradlew.bat clean :fabric:build
构建状态：BUILD SUCCESSFUL
```

---

## 📌 下一步计划

按「小步验证」策略继续推进：

1. 使用 AnimationFrameProfiler 收集 60FPS / 120FPS / 多玩家场景日志
2. 判断是否存在同一实体同一 render frame 重复 evaluate
3. 如确认重复 evaluate，复用 EntityFrameStateTracker 做同帧防重入
4. 如确认时间采样语义混乱，新增明确字段 `logicTick / partialTick / renderTickTime`
5. 如果 JFR / Spark 证明 NativeModelRenderer 热路径存在明显 Matrix / Vector 分配，做 scratch 复用

**仍然暂缓**：姿态插值重构、clip 烘焙、动画 LOD、武器动作状态隔离。
