# 用户指南 — Everlastingness Client

本指南面向**最终用户**:如何安装、配置、启动 Everlastingness Client,以及各功能模块的使用方法。

> 面向开发者/贡献者的构建说明见仓库根目录的 [README.md](../README.md)。

---

## 1. 系统要求

| 组件 | 要求 |
|---|---|
| 操作系统 | Windows 10/11(优先)、macOS 12+、Linux(需桌面) |
| .NET 运行时 | 8.0 或更高(启动器自带,无需单独安装若使用安装包) |
| Java | Minecraft 1.7.10 需 **Java 8**;1.16.5+ 需 **Java 17/21**(各版本自带,启动器自动下载) |
| Minecraft 账号 | 一个已购买 Java Edition 的微软账号 |
| 显卡 | 支持 OpenGL 2.1+(MC 自身要求) |

---

## 2. 安装

### 方式 A:使用安装包(Windows,推荐)

1. 从 [Releases](https://github.com/weige0831/EverlastingnessClient/releases) 下载最新 `EverlastingnessSetup.exe`。
2. 双击运行,按向导完成安装。
3. 安装后从开始菜单启动 "Everlastingness Launcher"。

### 方式 B:免安装版(所有平台)

1. 从 Releases 下载 `Everlastingness-Launcher-<version>-<os>.zip`。
2. 解压到任意目录。
3. 运行:
   - Windows:`Everlastingness.Launcher.exe`
   - macOS/Linux:`dotnet Everlastingness.Launcher.dll`(需先装 .NET 8 运行时)

---

## 3. 首次配置:微软账号登录

启动器使用微软账号认证(与官方启动器一致)。首次启动:

1. 主界面点击 **"微软登录"**。
2. 系统浏览器自动打开微软登录页,完成授权。
3. 登录成功后,右上角显示你的 Minecraft 用户名。

> **为何需要 Azure 应用 ClientId?** 启动器用 MSAL.NET 的系统浏览器流登录。安装包/发行版已内置预配置的 ClientId;若你从源码自行构建,需自注册一个 Azure "公共客户端" 应用(重定向 URI `http://localhost`),把 ClientId 填入 `~/.everlastingness/game/settings.json` 的 `AzureClientId` 字段,或设环境变量 `LAUNCHER_AZURE_CLIENT_ID`。详见 [README.md 的"认证配置"](../README.md#认证配置)。

未配置 ClientId 时,启动器**降级为离线模式**(仅离线用户名游玩,无法登录正版服务器)。

---

## 4. 启动游戏

1. 在主界面 **"客户端版本"** 选择目标 Minecraft 版本(如 `1.7.10` 或 `1.20.1`)。
2. 可选:勾选 **"启动时注入 Everlastingness 客户端"**(默认勾选)。
3. 可选:调整 Java 路径(留空自动检测)、最大内存、离线用户名。
4. 点击 **"启动游戏"**。

启动器会:
- 下载该版本的原版 Minecraft 文件(首次较慢);
- 下载对应的 Everlastingness 客户端 jar(注入所需);
- 用 `-javaagent`/LaunchWrapper 注入客户端并启动游戏。

---

## 5. 功能模块使用

启动游戏后,以下功能默认可用或可切换。所有模块都可在**游戏内配置 GUI** 中开关。

| 模块 | 默认状态 | 操作 | 效果 |
|---|---|---|---|
| **HUD 覆盖层** | 开启 | 自动显示 | 屏幕左上角显示坐标 `XYZ:` 与 `FPS:` |
| **按键绑定** | 开启 | 按 `R` | 切换 HUD 模块开关 |
| **配置 UI** | 开启 | 按 `RIGHT SHIFT` | 打开模块配置界面(可开关所有模块) |
| **披风系统** | 开启 | 自动 | 本地玩家显示 Everlastingness 自定义披风 |
| **FPS 优化** | **关闭** | 在配置 GUI 中开启 | 跳过超出剔除距离的实体渲染,提升 FPS |

### 打开配置 GUI

在游戏中按 **右 Shift(RIGHT_SHIFT)** → 列出所有模块,点击按钮切换开关 → 点 **Done** 关闭。

> 所有按键可在启动器设置或(未来)`keybinds.json` 中自定义。

---

## 6. 常见问题

**Q: 启动器打开后是空白/无窗口?**
A: 本启动器是 Avalonia 桌面应用,需要桌面环境。在无 GUI 的服务器/SSH 会话中无法运行。Windows 用户若 WebView2 缺失,安装 [WebView2 Runtime](https://developer.microsoft.com/microsoft-edge/webview2/)。

**Q: 登录提示"未配置 Azure ClientId"?**
A: 见 [第 3 节](#3-首次配置微软账号登录)。这是 MSAL 登录所需的应用注册,非机密信息。

**Q: 游戏启动但 HUD/按键没反应?**
A: 确认:(1)启动时勾选了"注入客户端";(2)该版本的客户端 jar 已下载到 `~/.everlastingness/client/`;(3)查看启动器底部"游戏日志"区是否有 `[Everlastingness]` 开头的输出。若日志显示 Mixin 未应用,可能是该版本支持尚在开发中(见 [支持版本矩阵](../README.md#每个版本时代的注入方式))。

**Q: 连某服务器被踢/封禁?**
A: 各服务器对客户端修改的规则不同。**请先阅读该服务器的规则**,部分服务器禁止任何客户端修改。Everlastingness 不绕过反作弊,但某些模块(如 FPS 实体剔除)可能被部分服务器视为不公平优势。本客户端**不承担**因使用导致的封禁责任。详见 [反作弊兼容性自测清单](./anticheat-checklist.md)。

**Q: 披风不显示?**
A: 披风仅对**已注册**的玩家显示(默认仅本地玩家)。第三人看不到你的披风(那是纯客户端装饰,不上传服务器)。

---

## 7. 卸载

- 安装包版:从"设置 → 应用"卸载,会清除启动器程序;游戏数据保留在 `~/.everlastingness/`。
- 完全清除:删除 `~/.everlastingness/` 目录。

---

## 8. 获取帮助

- 问题反馈:[GitHub Issues](https://github.com/weige0831/EverlastingnessClient/issues)
- 项目主页:<https://github.com/weige0831/EverlastingnessClient>
