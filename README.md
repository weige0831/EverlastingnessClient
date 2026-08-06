# Everlastingness Client

一个对标 **Lunar Client / Badlion Client** 的 Minecraft 优化客户端 + 独立启动器。

- **独立启动器** — C# / .NET 8 + Avalonia,负责微软账号认证、原版下载、版本选择、运行时注入启动
- **客户端核心** — Java,运行时通过 SpongePowered Mixin 注入到原版 Minecraft(不依赖 Forge / Fabric)
- **多版本矩阵** — 1.7.10、1.8.9、1.12.2(legacy · LaunchWrapper)+ 1.16.5、1.17.1、1.18.2、1.19.x、1.20.x、1.21.x(modern · ModLauncher)

> 当前进度:**Phase 1 客户端构建链路完成** —— 启动器端到端可编译运行;客户端 `:v1_7_10` 全流程跑通(RFG setup → MCP 反编译 → Mixin 编译 → reobf → refmap 打包)。详见[实施进度](#实施进度)。

---

## 目录结构

```
EverlastingnessClient/
├── launcher/                          # C# 启动器(.NET 8 + Avalonia)
│   ├── Everlastingness.Launcher/      #   Avalonia UI 主程序(VM/Views)
│   └── Everlastingness.Launcher.Core/ #   认证 / 下载 / 启动 / 注入 核心服务
└── client/                            # Java 客户端(Gradle 多项目)
    ├── common/                        #   版本无关:事件总线 / 模块框架 / Bootstrap
    ├── agent/                         #   Java Agent premain(modern 版本注入入口)
    ├── modules/                       #   功能模块(HUD 等),版本无关
    └── v1_7_10/                       #   1.7.10 子项目(RetroFuturaGradle + MCP + Mixin)
```

---

## 架构

### 注入链路

```
启动器                         客户端(运行时注入)
┌────────────────────┐         ┌──────────────────────────────────────┐
│ 微软账号认证(MSAL) │         │ LaunchWrapper / ModLauncher           │
│        ↓            │         │   + MixinTweaker / Java Agent         │
│ 下载原版 MC        │ ──启动──→ │        ↓                              │
│        ↓            │ -cp…    │ Bootstrap → EverlastingnessClient     │
│ ClientInjector 改写 │ -D…     │   + EventBus + Module 框架            │
│   命令行            │         │        ↓                              │
│        ↓            │         │ 各版本 Mixin 改写原版类 → 触发事件    │
│ 组装 classpath      │         │        ↓                              │
│   + launchwrapper   │         │ 功能模块(HUD 等)响应事件            │
│   + mixin           │         └──────────────────────────────────────┘
│   + 客户端 jar      │
└────────────────────┘
```

### 每个版本时代的注入方式

| 时代 | MC 版本 | 构建工具链 | 映射 | 运行时注入 |
|---|---|---|---|---|
| Legacy | 1.7.10 / 1.8.9 / 1.12.2 | RetroFuturaGradle(仅 1.7.10)/ ForgeGradle 2.x | MCP | LaunchWrapper + `--tweakClass MixinTweaker` + `ClientTweaker` |
| Modern | 1.16.5+ | Fabric Loom(仅开发期) | Mojang 官方 / Yarn | `-javaagent` + ModLauncher |

> ⚠️ **关键工程约束**:Legacy 工具链需要 Gradle 6.9–8.8,Modern(Loom 1.17)需要 Gradle 9.x —— 两者**不能共用同一个 Gradle wrapper**。因此本仓库的 `client/` 默认用 Gradle 8.8(覆盖 legacy + 较老的 Loom);现代版本将在独立的 Gradle 构建中维护。

---

## 构建与运行

### 启动器(C#)

要求:.NET 8 SDK。

```bash
cd launcher
dotnet build Everlastingness.Launcher.sln      # 编译(已验证 0 警告 0 错误)
dotnet run --project Everlastingness.Launcher   # 启动
```

首次运行前,在 `~/.everlastingness/game/settings.json` 配置微软认证用的 Azure 公共客户端 id(注册指引见 [认证配置](#认证配置)),或设置环境变量 `LAUNCHER_AZURE_CLIENT_ID`。未配置时启动器降级为离线模式。

#### 依赖库

- [`CmlLib.Core`](https://github.com/CmlLib/CmlLib.Core) 4.0.6 — 原版版本清单 / 下载 / 启动
- [`CmlLib.Core.Auth.Microsoft`](https://github.com/CmlLib/CmlLib.Core.Auth.Microsoft) 3.3.1(`XboxAuthNet.Game` + MSAL.NET)— 微软账号 → Xbox Live → Minecraft 认证
- [Avalonia](https://avaloniaui.net/) 11.2.7 — 跨平台 UI(注:必须用 11.x;12.x 的源生成器要求 Roslyn 4.14,与 .NET 8 SDK 自带的 4.11 冲突)

#### 认证配置

本启动器用 MSAL.NET 的系统浏览器流做微软登录(跨平台,不依赖 WebView2)。需自注册 Azure 应用:

1. Azure Portal → Azure Active Directory → 应用注册 → 新注册
2. 账户类型选"任何组织目录 + 个人 Microsoft 账户"
3. 重定向 URI:平台"公共客户端/本机",值 `http://localhost`
4. 启用"允许公共客户端流"
5. 复制"应用程序(客户端) ID",填入 `settings.json` 的 `AzureClientId` 字段

### 客户端(Java)

要求:JDK 8(运行)+ JDK 17/21(构建)。首次构建会下载 Gradle 8.8。

```bash
cd client
./gradlew :common:build       # 编译版本无关核心(事件总线 / 模块框架)
./gradlew :modules:build      # 编译功能模块
./gradlew :agent:build        # 编译 Java Agent(modern 注入入口)
# ./gradlew :v1_7_10:build   # 1.7.10 子项目(需联网拉取 MCP / 1.7.10 原版)
```

---

## 实施进度

### ✅ Phase 0 — 基础设施骨架

**启动器**
- [x] C# 解决方案 + Avalonia MVVM 工程 + Core 类库
- [x] `MinecraftLauncherService` — 封装 CmlLib v4(版本清单 / 下载 / 启动,真实 API 已对照源码校正)
- [x] `MinecraftAuthService` — MSAL + XboxAuthNet 微软认证(账号缓存 / 静默刷新 / 登出)
- [x] `ClientInjector` — 按 MC 时代改写 vanilla 命令行(LaunchWrapper tweak / javaagent)
- [x] `ClientProfiles` — 12 个版本的注入配置矩阵
- [x] `LauncherSettings` — 持久化配置
- [x] Avalonia 主屏 UI(版本选择 / 登录 / 启动 / 进度 / 游戏日志)
- [x] **`dotnet build` 0 警告 0 错误,`dotnet run` 启动验证通过**

**客户端**
- [x] Gradle 多项目 + wrapper(Gradle 8.8)
- [x] `:common` — `EverlastingnessClient` 单例 / `EventBus` / `Module` 框架 / `Bootstrap` / `ModuleDiscoverer`(ServiceLoader)
- [x] `:agent` — `EverlastingnessAgent.premain`(modern 注入入口)
- [x] `:modules` — `HudOverlayModule` 示例(证明模块生命周期 + 事件总线端到端可用)
- [x] `:v1_7_10` — RFG + MCP + Mixin 骨架,`ClientTweaker`(legacy 注入入口),示例 `MixinEntityRenderer`

### ⏭ Phase 1 — 1.7.10 客户端构建链路(已完成)

- [x] RFG 插件解析(需 `pluginManagement.resolutionStrategy` 映射,因 RFG 不在 Gradle Plugin Portal)
- [x] foojay toolchain resolver 供 RFG 的 Fernflower 反编译器(JDK 17)
- [x] `setupDecompWorkspace` 跑通(下载 MC 1.7.10 原版 + MCP,反编译,打补丁,约 13 分钟)
- [x] 校验 `MixinEntityRenderer` 注入目标:`updateCameraAndRender(float)` 确实存在于反编译 MC 源(行 1013),SRG 名 `func_78480_b`
- [x] Mixin 注解处理器配置(`-AreobfSrgFile` 指向 `mcp-srg.srg`,`-AoutRefMapFile` 生成 refmap)
- [x] **`reobfJar` 成功**:产出 `v1_7_10-1.0.0-SNAPSHOT.jar`(含 `MixinEntityRenderer`/`ClientTweaker`/`mixins.everlastingness.json`/`mixins.everlastingness.refmap.json`),refmap 正确映射 `updateCameraAndRender → func_78480_b`

### ⏭ Phase 2 — 真机端到端 + 多版本(下一步)

- [ ] 真机验证:启动器登录 → 下载 1.7.10 → 注入启动 → HUD 在游戏内显示
- [ ] 1.20.x modern 版本(Fabric Loom)子项目与 agent 注入链路

### Phase 3–4 — 功能模块 + 分发

详见历史计划文档。

---

## 许可与声明

本项目的"客户端"形态(独立启动器 + 运行时注入)参考了 Lunar / Badlion 的公开行为,**未使用其任何闭源代码**。连接第三方服务器前请遵守各服务器及 Mojang 的服务条款。
