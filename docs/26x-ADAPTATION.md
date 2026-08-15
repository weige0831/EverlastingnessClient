# Minecraft 26.1 / 26.2 适配现状（2026-08-16 调研）

## 关键发现

1. **Mojang 从 26.x 起停止混淆客户端 jar**。
   26.1 / 26.2 的 `client.jar` 内全是 official 命名（`net/minecraft/client/Minecraft.class`、
   `Camera`、`GameNarrator`、`LevelRenderer` 等），不再是 1.21.x 及之前的
   `enn`/`fjq`/`drq` 短混淆名。

2. **因此 Fabric 没有也无需发布 26.x 的 intermediary / Yarn**。
   `maven.fabricmc.net/net/fabricmc/intermediary/maven-metadata.xml` 最新为
   `1.21.11`；`meta.fabricmc.net/v2/versions/yarn/26.1`、`26.2` 均为空 `[]`。
   这不是"发布延迟"，而是 26.x 不再需要映射层的自然结果。

3. **Mojang 也不在 version manifest 提供 `client_mappings`（ProGuard client.txt）**。
   26.1/26.2 的 downloads 只有 `client` / `server` 两个 key。
   `loom.officialMojangMappings()` 因此报
   `Failed to find official mojang mappings for 26.2`。

## 对本项目的影响

现有 v1_20_x 源码以 **Yarn 命名**编写（`MinecraftClient`、`World`、
`WorldRenderer`），而 26.x 运行时是 **Mojang official 命名**（`Minecraft`、
`Level`、`LevelRenderer`）——不只是混淆差异，而是**两套不同的命名体系**：

| Yarn (1.16–1.21.11) | Mojang official (26.x) |
|---|---|
| `MinecraftClient` | `Minecraft` |
| `World` | `Level` |
| `WorldRenderer` | `LevelRenderer` |
| `InGameHud` | `Gui` |
| `GameRenderer` | `GameRenderer` |

## 适配路径（待做）

26.x 需要一个专门的 **Yarn→Mojang 名称适配层**，两条可选路线：

1. **源码适配器**（推荐）：为 26.x 新建 `v26_x` 模块，将 28 个 mixin 的
   import/目标改成 Mojang official 名。由于运行时不混淆，**无需 obf-rewrite、
   无需 refmap**，构建即所得——注入链比 1.21.x 更简单（agent 原样可用，
   客户端类名直接匹配）。工作量主要是 28 个文件的改名 + 少数 API 签名漂移。

2. **等 Fabric 发布 26.x Yarn**：若 Fabric 后续为 26.x 提供 yarn（社区强烈
   要求的话），则现有源码直接换 mappings 坐标构建，再加 identity 化的
   obf 流水线（26.x 无混淆 → obf_rewrite 退化为恒等）。

构建工具链已就绪并验证无回归：Gradle 9.5.1 + Loom 1.17.19 + JDK 25
（`client-modern` 当前配置，1.20.1 全流水线 27 applied 复测通过）。

## 当前结论

26.1/26.2 的"外部阻塞"本质是 **Mojang 破坏性变更 + Fabric 映射链终止**，
不是等待发布的暂时状态。需要按上述路线 1 做一次源码级适配。
