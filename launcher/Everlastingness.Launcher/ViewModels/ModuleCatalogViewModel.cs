using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.Json;

namespace Everlastingness.Launcher.ViewModels;

/// <summary>One game module shown on the Modules page.</summary>
public partial class ModuleItemViewModel : ViewModelBase
{
    public string Id { get; init; } = "";
    public string Name { get; init; } = "";
    public string Description { get; init; } = "";
    public string Category { get; init; } = "";
    public bool DefaultEnabled { get; init; }

    bool _enabled;
    public bool Enabled
    {
        get => _enabled;
        set
        {
            if (SetProperty(ref _enabled, value))
            {
                Owner?.Persist(this, value);
            }
        }
    }

    ModuleCatalogViewModel? Owner { get; set; }

    internal void Bind(ModuleCatalogViewModel owner, bool enabled)
    {
        Owner = owner;
        _enabled = enabled;
    }
}

/// <summary>
/// The 38-module catalogue shown in the UI. Reads/writes the same
/// ~/.everlastingness/client/modules.json the in-game ClientConfig uses,
/// so toggles here apply on next launch (and survive restarts).
/// </summary>
public partial class ModuleCatalogViewModel : ViewModelBase
{
    /// <summary>Static catalogue mirroring client/modules sources — id/name/description/category/default.</summary>
    static readonly (string Id, string Name, string Desc, string Cat, bool Def)[] Catalogue =
    {
        ("perspective",       "Perspective 自由视角",   "按住切换第三人称任意视角",                      "CAMERA", true),
        ("cape",              "Cape 披风",              "渲染 Everlastingness 定制披风",                "VISUAL", true),
        ("fullbright",        "Fullbright 全亮度",      "洞穴/夜晚全亮，无黑暗区域",                    "VISUAL", true),
        ("zoom",              "Zoom 缩放",              "按 C 键平滑变焦（可调倍率）",                  "VISUAL", true),
        ("custom_crosshair",  "Custom Crosshair",       "自定义十字准星样式与颜色",                     "VISUAL", true),
        ("block_outline",     "Block Outline",          "方块描边颜色自定义",                           "VISUAL", true),
        ("motion_blur",       "Motion Blur 动态模糊",   "镜头运动模糊效果",                             "VISUAL", false),
        ("nick_hider",        "Nick Hider 昵称隐藏",    "隐藏自己/他人昵称（录像友好）",                "VISUAL", false),
        ("time_changer",      "Time Changer 时间",      "客户端锁定世界时间",                           "VISUAL", false),
        ("weather_changer",   "Weather Changer 天气",   "客户端覆盖天气（晴/雨/雷）",                   "VISUAL", false),
        ("hud",                "HUD Overlay",            "基础 HUD 信息叠加层",                          "HUD", true),
        ("cps_counter",       "CPS 计数器",             "左右键每秒点击数",                             "HUD", true),
        ("ping_display",      "Ping 显示",              "服务器延迟毫秒数",                             "HUD", true),
        ("direction_hud",     "Direction 朝向",         "朝向/罗盘 HUD",                                "HUD", true),
        ("armor_status",      "Armor Status",           "盔甲耐久 HUD",                                 "HUD", true),
        ("clock_armor_hud",   "Clock & Armor",          "时钟+盔甲组合 HUD",                            "HUD", true),
        ("keystrokes",        "Keystrokes 按键显示",     "WASD/鼠标按键可视化",                          "HUD", false),
        ("potion_effects",    "药水效果 HUD",           "当前药水效果与剩余时间",                       "HUD", false),
        ("memory_usage",      "内存占用",               "JVM 内存使用/上限",                            "HUD", false),
        ("playtime",          "游玩时长",               "本次会话累计时间",                             "HUD", false),
        ("server_address",    "服务器地址",             "当前服务器地址 HUD",                           "HUD", false),
        ("combo_counter",     "Combo 连击",             "连击计数",                                     "COMBAT", false),
        ("reach_display",     "Reach 距离显示",         "最近攻击距离",                                 "COMBAT", false),
        ("damage_tint",       "Damage Tint",            "受击红色屏幕边缘着色",                         "COMBAT", false),
        ("hitbox",            "Hitbox 碰撞箱",          "实体碰撞箱可视化",                             "COMBAT", false),
        ("toggle_sneak",      "Toggle Sneak",           "潜行切换（保持蹲伏状态提示）",                 "COMBAT", false),
        ("smooth_scroll",     "Smooth Scroll 平滑滚动", "物品栏平滑滚动",                               "INPUT", true),
        ("auto_text",         "Auto Text 快捷消息",     "按 G/H 键发送预设消息",                        "INPUT", false),
        ("chat_timestamps",   "Chat Timestamps",        "聊天时间戳前缀",                               "UTILITY", false),
        ("screenshot_viewer", "Screenshot 增强",        "截图自动复制到剪贴板",                         "UTILITY", false),
        ("shulker_preview",   "Shulker Preview",        "潜影盒物品悬浮预览",                           "UTILITY", true),
        ("scoreboard",        "Scoreboard 记分板",      "侧边记分板隐藏/美化",                          "UTILITY", false),
        ("waila",             "WAILA 方块信息",         "准星指向方块/实体信息",                        "UTILITY", false),
        ("coord_copy",        "坐标复制",               "按键复制当前坐标",                             "UTILITY", false),
        ("chunk_borders",     "Chunk Borders 区块边界", "F9 区块边界可视化",                            "PERFORMANCE", false),
        ("fog",               "Fog 雾效移除",           "移除/推远雾效提升可见度",                      "PERFORMANCE", false),
        ("hurt_cam",          "Hurt Cam 受击镜头",      "受击镜头晃动关闭",                             "PERFORMANCE", false),
        ("fps_boost",         "FPS 优化",               "渲染路径优化提升帧率",                         "PERFORMANCE", true),
    };

    public ObservableCollection<ModuleItemViewModel> Modules { get; } = new();

    public IReadOnlyList<string> Categories { get; } =
        Catalogue.Select(m => m.Cat).Distinct().OrderBy(c => c).ToArray();

    string? _selectedCategory;
    public string? SelectedCategory
    {
        get => _selectedCategory;
        set => SetProperty(ref _selectedCategory, value);
    }

    static readonly string ConfigPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        ".everlastingness", "client", "modules.json");

    public ModuleCatalogViewModel()
    {
        var flags = LoadFlags();
        foreach (var m in Catalogue)
        {
            var item = new ModuleItemViewModel
            {
                Id = m.Id, Name = m.Name, Description = m.Desc,
                Category = m.Cat, DefaultEnabled = m.Def,
            };
            item.Bind(this, flags.TryGetValue(m.Id, out var f) ? f : m.Def);
            Modules.Add(item);
        }
    }

    internal void Persist(ModuleItemViewModel item, bool enabled)
    {
        var flags = LoadFlags();
        flags[item.Id] = enabled;
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(ConfigPath)!);
            File.WriteAllText(ConfigPath,
                JsonSerializer.Serialize(flags, new JsonSerializerOptions { WriteIndented = true }),
                new UTF8Encoding(false));
        } catch (Exception e)
        {
            Console.Error.WriteLine($"[modules] could not save {ConfigPath}: {e.Message}");
        }
    }

    static Dictionary<string, bool> LoadFlags()
    {
        try
        {
            if (File.Exists(ConfigPath))
            {
                var doc = JsonSerializer.Deserialize<Dictionary<string, bool>>(File.ReadAllText(ConfigPath));
                if (doc != null) return doc;
            }
        } catch (Exception e)
        {
            Console.Error.WriteLine($"[modules] could not read {ConfigPath}: {e.Message}");
        }
        return new Dictionary<string, bool>();
    }
}
