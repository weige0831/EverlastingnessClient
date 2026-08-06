# 真机端到端验证步骤

> 本清单供**在具备 Minecraft 1.7.10 + 桌面 GUI + 微软账号的本地环境**中,完整闭环验证 Everlastingness Client。本仓库的开发会话在无 GUI 沙箱中执行,真机验证需由你在本地完成。
>
> 按顺序执行;每步给出**预期结果**,不符即停下排查(并到 [Issues](https://github.com/weige0831/EverlastingnessClient/issues) 反馈)。

---

## 前置条件

- Windows/macOS/Linux **桌面**环境(非 SSH/无头)。
- 已购 Minecraft Java Edition 的**微软账号**。
- 本仓库源码已 clone,或已下载 CI 产物。
- 已按 [README](../README.md) 成功构建过启动器(`dotnet build` 0 错误)与客户端 jar(`:v1_7_10:reobfJar`)。

---

## 第 1 步:构建启动器与 1.7.10 客户端 jar

```bash
# 启动器
cd launcher
dotnet build Everlastingness.Launcher.sln        # 预期:0 警告 0 错误
dotnet run --project Everlastingness.Launcher     # 预期:启动器窗口出现

# 客户端 1.7.10 注入 jar(legacy 链路,真机闭环用 1.7.10)
cd ../client
./gradlew :v1_7_10:reobfJar                       # 预期:BUILD SUCCESSFUL
ls v1_7_10/build/libs/                            # 预期:v1_7_10-1.0.0-SNAPSHOT.jar
```

把产物 jar 复制到客户端资源目录(启动器从这里取注入 jar):

```bash
# 推荐:运行本仓库提供的 staging 脚本(自动放好 client jar + 下载 mixin/launchwrapper)
bash scripts/stage-client-1.7.10.sh
```

该脚本会:
- 把 `v1_7_10-1.0.0-SNAPSHOT.jar` 复制为 `~/.everlastingness/client/everlastingness-1.7.10.jar`(启动器配置文件预期的文件名);
- 下载 `mixin-0.8.7.jar` 与 `launchwrapper-1.12.jar` 到同一目录(注入所需运行时,URL 已验证 200);
- 打印各文件就位状态。

> 也可手动:见脚本内容了解文件名与来源。**文件名必须与 `ClientProfiles.cs` 中 Legacy profile 的声明一致**(脚本已处理好)。

---

## 第 2 步:配置微软认证 ClientId

1. 按 [README 的"认证配置"](../README.md#认证配置)注册 Azure 公共客户端应用,取 ClientId。
2. 写入 `~/.everlastingness/game/settings.json`:
   ```json
   { "AzureClientId": "你的-client-id", "InjectClient": true, "LastVersion": "1.7.10" }
   ```
   或设环境变量 `LAUNCHER_AZURE_CLIENT_ID`。

---

## 第 3 步:启动器登录

1. `dotnet run --project Everlastingness.Launcher`。
2. 点 **"微软登录"** → 浏览器自动打开 → 完成微软授权。
3. **预期**:右上角显示你的 Minecraft 用户名。

> ❌ 若提示"未配置 Azure ClientId" → 回第 2 步。

---

## 第 4 步:下载并启动 1.7.10(注入)

1. 版本下拉选 **`1.7.10`**。
2. 确认勾选 **"启动时注入 Everlastingness 客户端"**。
3. 点 **"启动游戏"**。
4. 启动器底部"游戏日志"区开始出现输出。

**预期**:
- 进度条显示下载 1.7.10 原版文件。
- 游戏窗口启动,进入主菜单。
- 日志含 `[Everlastingness]` 开头行(如 `Bootstrap`/`HUD overlay enabled`)。

> ❌ 若游戏启动但**无** `[Everlastingness]` 日志 → Mixin 未注入。检查:client jar 是否就位、`launchwrapper`/`mixin` jar 是否存在、`-Dmixin.configs` 是否生效。这是 Phase 3b 的 standalone host 在真机的首次验证点。

---

## 第 5 步:验证五个功能模块(进入单人世界)

进入任意单人世界,逐项确认:

| # | 模块 | 操作 | 预期 |
|---|---|---|---|
| 1 | **HUD** | 进入世界 | 屏幕左上角显示 `XYZ: ...` 与 `FPS: ...` 两行 |
| 2 | **按键绑定** | 按 `R` | 日志打印 `HUD enabled/disabled`,HUD 消失/出现 |
| 3 | **配置 GUI** | 按 `RIGHT SHIFT` | 弹出模块列表界面,有 HUD/Cape/FPS 等按钮 + Done |
| 4 | **配置 GUI 切换** | 在 GUI 点 HUD 按钮 | 按钮文字切换 `[ON]`/`[OFF]`,HUD 对应显隐 |
| 5 | **披风** | 第三人称视角(F5)看向自己 | 背后显示蓝色 Everlastingness 披风 |
| 6 | **FPS 优化** | GUI 中开启 "FPS Boost",生成大量实体(如动物农场)远离 | 远处实体消失,FPS 数字上升 |

---

## 第 6 步:记录结果

完成上述后,把结果回填到 [反作弊兼容性自测清单](./anticheat-checklist.md) 的"自测记录模板",单人世界行应为全 ✅。

若任一项不符合预期,在 [Issues](https://github.com/weige0831/EverlastingnessClient/issues) 提交,附:
- 启动器"游戏日志"区截图;
- `~/.everlastingness/game/` 下相关文件;
- 该项预期 vs 实际。

---

## 闭环判定

**全部 6 项模块表现符合预期 = Phase 3b/3c 真机闭环完成**,即 standalone Mixin host 与五个功能模块在真机 MC 上实际生效,本项目的实施计划(Phase 0–4 的可验证部分)全部完成。

真机验证的剩余风险点(已在代码与 README 标注):
- standalone Mixin host 服务被 ServiceLoader 选中且 transformer 正确应用(Phase 3b);
- 各版本 API 名映射在生产 jar 中正确(已通过 refmap + javap 在构建期验证,真机进一步确认)。
