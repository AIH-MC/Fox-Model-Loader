<div align="center">
  <img src="images/144725232_p0.png" alt="banner"/>
  <p>图片作者：「pixiv」<a href="https://www.pixiv.net/users/76344429">法师来自未来</a></p>
  <h1>Fox Model Loader / 绯绯狐的模组加载</h1>
  <p>基于 <a href="https://gitgud.io/NoSteveModel/OpenYSM">OpenYSM</a> 项目的 1.21+ 迁移版本，适用于 <strong>Fabric</strong> 加载器</p>
</div>

## 源码地址

本项目基于上游项目 OpenYSM 进行迁移：

- **上游源码**: [https://gitgud.io/NoSteveModel/OpenYSM](https://gitgud.io/NoSteveModel/OpenYSM)

## 关于 YSM 与 OpenYSM

YSM 与 OpenYSM 之间的过往纠葛并非本项目的重点。绯绯狐的模组加载仅专注于将 OpenYSM 迁移至 Minecraft 1.21+ 平台，为社区提供一个可用的模型加载工具。请勿在本仓库中讨论与开发无关的争议话题。

## 说明

本仓库目前包含了基于 OpenYSM 的 1.21+的Fabric 迁移版本。

这里要体现的是 AI Agent 在移植方面的重大成功，在 deepseek-v4-pro「1m」与 Claude Code 的强强配合下，原本需要一周时间的移植工作，成功压缩至一天（这就是不讨论社区的理由）。这说明一件事，我们普通人，自己就可以迁移模组，甚至开发模组。这一次迁移，花费了我 10 CNY，token（词元）消耗数为一亿，在此过程中，解决了从 1.20.1 迁移至 1.21.1 的各种困难，包括但不限于 mixin 变更、Mojang 在 1.21.1 改变变量所带来的一系列问题。帮助我一个不懂 Java 的普通人，解决了大量的依赖、环境变量，以及运行问题。这足以说明，迁移模组本身，是完全可行的。只需要动动手指，你也能迁移成功。

这里建议，迁移一般模组用我上述的组合，便可在一天内完成。DeepSeek 的低成本优势，配合 Claude Code 的高命中，让每个人都能花一点点 token 所需的小钱，便能完成这个费时费力还需要熟悉 Java、Fabric 的工作。这就是 AI Agent 带给我们的意义：提高生产力，让更多人能真正发挥自己的想法。

**请注意：项目并非 Production Ready，可能存在命名语义错误、渲染错误等问题，如果您在使用过程中遇到了任何问题请打开 Issue 反馈，最好附带截图和可能的报错日志。**

---

## 迁移历程

本项目由 **OpenYSM**（原目标 Minecraft 1.20.1）迁移至 **Minecraft 26.1.2 Fabric**。Mojang 在 26.x 版本中对游戏引擎几乎所有子系统做了破坏性改动，迁移过程涉及：

- **渲染管线重写** — `GuiGraphics` 替换为 `GuiGraphicsExtractor`，所有渲染方法重命名（如 `drawString()` → `text()`），34 个文件逐一迁移
- **输入系统翻新** — 鼠标/键盘事件从原始类型改为结构化事件对象（如 `mouseClicked(MouseButtonEvent, boolean)`）
- **实体类层级重组** — `AbstractArrow`、`Boat` 等类的包路径变更，数十个 import 修正
- **NBT/Registry API 改为 Optional 返回值** — `getString()`、`getCompound()` 等返回 `Optional`，`getAllKeys()` 被移除
- **EntityRenderer 新增第三类型参数** — 加入 `extractRenderState()` / `createRenderState()` 抽象
- **无官方 Mojang 映射** — 26.1.2 不再提供 `client_mappings`，自行编写 ASM 9.9 字节码分析工具生成 tiny v2 映射
- **Fabric Loom 升级** — 从 1.8-SNAPSHOT 升级至 1.17.0-alpha.8 以支持 Java 25 字节码
- **Architectury API 无 26.x 支持** — 创建 22 个 stub 类复制所需平台抽象

最终解决 **200+ 编译错误**、**40+ 运行时崩溃**，游戏可正常启动并加载模型。

### 构建环境

| 组件 | 版本 |
| ---- | ---- |
| Java | 25 (Azul Zulu 25.0.2) |
| Gradle | 9.5.1 |
| Fabric Loom | 1.17.0-alpha.8 |
| Fabric Loader | 0.19.2 |
| Fabric API | 0.149.1+26.1.2 |
| Shadow 插件 | 9.0.0-beta4 |

### 经验总结

1. **Stub 策略至关重要** — 只为目标 JAR 中确实不存在的类创建 stub，真实存在的类必须删除 stub 用真实类替代
2. **方法签名必须精确匹配** — 参数类型精确到包名，`Object` 无法替代具体类型
3. **`javap` 是核心工具** — 每个 API 接口通过字节码检查确认，而非靠猜测
4. **批量修复优于手动修改** — 相同类型错误的自动化批处理比逐个手动修复快 10 倍
