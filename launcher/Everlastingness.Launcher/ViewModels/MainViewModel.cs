using System;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Everlastingness.Launcher.Core.Clients;
using Everlastingness.Launcher.Core.Minecraft;

namespace Everlastingness.Launcher.ViewModels;

/// <summary>
/// The launcher's primary view model. Wires the Avalonia UI to the Core
/// services: listing Minecraft versions, authenticating a Microsoft account,
/// and building + launching the game (with optional client injection).
///
/// All long-running work runs off the UI thread; progress is marshalled back
/// via <see cref="CommunityToolkit.Mvvm.ComponentModel.ObservableProperty"/>
/// source-generated properties so the view binds update automatically.
/// </summary>
public partial class MainViewModel : ViewModelBase
{
    private readonly MinecraftLauncherService _launcher;
    private readonly MinecraftAuthService _auth;
    private readonly IClientAssetsLocator _clientAssets;
    private readonly LauncherSettings _settings;

    public MainViewModel(
        MinecraftLauncherService launcher,
        MinecraftAuthService auth,
        IClientAssetsLocator clientAssets,
        LauncherSettings settings)
    {
        _launcher = launcher;
        _auth = auth;
        _clientAssets = clientAssets;
        _settings = settings;

        // Surface the client-version matrix in the UI, marking each as
        // ready-to-inject only once its compiled jar is present locally.
        foreach (var version in ClientProfiles.SupportedVersions)
        {
            var profile = ClientProfiles.Find(version)!;
            ClientVersions.Add(new ClientVersionViewModel
            {
                MinecraftVersion = version,
                Era = profile.Era,
                Ready = ClientProfiles.IsArtifactReady(profile, _clientAssets)
            });
        }

        var defaultVersion = ClientVersions.FirstOrDefault(v => v.MinecraftVersion == _settings.LastVersion)
                             ?? ClientVersions.FirstOrDefault();
        if (defaultVersion is not null)
            SelectedClientVersion = defaultVersion;

        JavaPath = _settings.JavaPath;
        MaximumRamMb = _settings.MaximumRamMb;
        InjectClient = _settings.InjectClient;
    }

    /// <summary>Minecraft versions supported by Everlastingness (for injection).</summary>
    public ObservableCollection<ClientVersionViewModel> ClientVersions { get; } = new();

    /// <summary>Full Mojang version list (loaded on demand).</summary>
    public ObservableCollection<MinecraftVersionInfo> MinecraftVersions { get; } = new();

    [ObservableProperty]
    private ClientVersionViewModel? _selectedClientVersion;

    [ObservableProperty]
    private MinecraftVersionInfo? _selectedMinecraftVersion;

    /// <summary>Status text shown in the footer.</summary>
    [ObservableProperty]
    private string _status = "就绪";

    /// <summary>0..1 download progress, or null when idle.</summary>
    [ObservableProperty]
    private double? _progress;

    /// <summary>Whether a long-running operation is in flight.</summary>
    [ObservableProperty]
    [NotifyCanExecuteChangedFor(nameof(RefreshVersionsCommand))]
    [NotifyCanExecuteChangedFor(nameof(LoginCommand))]
    [NotifyCanExecuteChangedFor(nameof(LaunchCommand))]
    private bool _isBusy;

    /// <summary>Path to the Java executable.</summary>
    [ObservableProperty]
    private string _javaPath = "";

    /// <summary>Maximum heap (-Xmx) in MB.</summary>
    [ObservableProperty]
    private int _maximumRamMb = 4096;

    /// <summary>Inject the Everlastingness client on launch.</summary>
    [ObservableProperty]
    private bool _injectClient = true;

    /// <summary>Offline (cracked) username when not signed in.</summary>
    [ObservableProperty]
    private string _offlineUsername = "Player";

    /// <summary>Currently signed-in account, or null.</summary>
    [ObservableProperty]
    private AccountInfo? _account;

    /// <summary>Whether a Microsoft account is signed in.</summary>
    [ObservableProperty]
    private bool _isSignedIn;

    /// <summary>Game process log lines (tail).</summary>
    public ObservableCollection<string> GameLog { get; } = new();

    // --- Navigation ---
    public string[] NavItems { get; } = { "首页", "模块", "设置", "账号", "日志" };

    [ObservableProperty]
    private int _navIndex;

    /// <summary>Module catalogue page model (persists to modules.json).</summary>
    public ModuleCatalogViewModel Modules { get; } = new();

    // --- Staged launch progress (Home hero) ---
    public static readonly string[] LaunchStages = { "下载资源", "校验文件", "解压依赖", "启动游戏" };

    [ObservableProperty]
    private int _launchStage = -1;

    partial void OnLaunchStageChanged(int value) => Status = value < 0
        ? "就绪"
        : $"{LaunchStages[value]}… ({value + 1}/{LaunchStages.Length})";

    /// <summary>Load the Mojang version manifest.</summary>
    [RelayCommand(CanExecute = nameof(CanRunOperation))]
    private async Task RefreshVersionsAsync()
    {
        IsBusy = true;
        Status = "正在获取版本列表…";
        try
        {
            MinecraftVersions.Clear();
            var versions = await _launcher.GetVersionsAsync();
            foreach (var v in versions)
                MinecraftVersions.Add(v);

            // Default-select the Mojang version matching the chosen client profile.
            var match = MinecraftVersions.FirstOrDefault(v => v.Name == SelectedClientVersion?.MinecraftVersion)
                        ?? MinecraftVersions.FirstOrDefault(v => v.Type == "release");
            if (match is not null)
                SelectedMinecraftVersion = match;

            Status = $"已加载 {MinecraftVersions.Count} 个版本";
        }
        catch (Exception ex)
        {
            Status = $"获取版本失败:{ex.Message}";
        }
        finally
        {
            IsBusy = false;
        }
    }

    /// <summary>Sign in with a Microsoft account via the system browser.</summary>
    [RelayCommand(CanExecute = nameof(CanRunOperation))]
    private async Task LoginAsync()
    {
        IsBusy = true;
        Status = "请在打开的浏览器中完成微软账号登录…";
        try
        {
            var session = await _auth.LoginNewAccountAsync();
            Account = _auth.GetDefaultAccount();
            IsSignedIn = true;
            Status = $"已登录:{Account?.Username}";
        }
        catch (Exception ex)
        {
            Status = $"登录失败:{ex.Message}";
        }
        finally
        {
            IsBusy = false;
        }
    }

    /// <summary>Download (if needed) and launch the game.</summary>
    [RelayCommand(CanExecute = nameof(CanRunOperation))]
    private async Task LaunchAsync()
    {
        if (SelectedClientVersion is null)
        {
            Status = "请先选择一个 Minecraft 版本";
            return;
        }
        if (string.IsNullOrWhiteSpace(JavaPath))
        {
            var auto = await _launcher.GetDefaultJavaPathAsync();
            if (auto is null)
            {
                Status = "未找到 Java,请在设置中指定 Java 路径";
                return;
            }
            JavaPath = auto;
        }

        IsBusy = true;
        Progress = 0;
        LaunchStage = 0;
        GameLog.Clear();
        var version = SelectedClientVersion.MinecraftVersion;
        var ct = CancellationToken.None;

        try
        {
            // 1. Download vanilla files.
            Status = $"正在下载原版 {version}…";
            var progress = new Progress<InstallProgress>(p =>
            {
                Progress = p.Fraction;
                Status = string.IsNullOrEmpty(p.CurrentFile)
                    ? $"下载中 {p.ProgressedTasks}/{p.TotalTasks}"
                    : $"下载中:{p.CurrentFile}";
            });
            await _launcher.InstallAsync(version, progress, ct);
            LaunchStage = 2;

            // 2. Resolve session (signed-in account, or offline fallback).
            var session = IsSignedIn && Account is not null
                ? await _auth.LoginSilentlyAsync(Account.Identifier, ct)
                : MinecraftAuthService.CreateOfflineSession(OfflineUsername);

            // 3. Build the launch process.
            var profile = InjectClient ? ClientProfiles.Find(version) : null;
            var options = new LaunchOptions
            {
                JavaPath = JavaPath,
                MaximumRamMb = MaximumRamMb,
                InjectClient = InjectClient,
                Session = session
            };
            var process = await _launcher.BuildProcessAsync(version, options, profile, _clientAssets, ct);

            // 4. Start the game and stream its output.
            process.StartInfo.RedirectStandardOutput = true;
            process.StartInfo.RedirectStandardError = true;
            process.EnableRaisingEvents = true;

            process.OutputDataReceived += (_, e) =>
            {
                if (e.Data is not null)
                    AppLog(e.Data);
            };
            process.ErrorDataReceived += (_, e) =>
            {
                if (e.Data is not null)
                    AppLog("[stderr] " + e.Data);
            };
            process.Exited += (_, _) =>
            {
                AppLog($"[进程退出 代码 {process.ExitCode}]");
            };

            process.Start();
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();

            LaunchStage = 3;
            Status = InjectClient ? $"已启动 {version}(注入客户端)" : $"已启动 {version}(原版)";

            // Persist user choices for next launch.
            _settings.LastVersion = version;
            _settings.JavaPath = JavaPath;
            _settings.MaximumRamMb = MaximumRamMb;
            _settings.InjectClient = InjectClient;
        }
        catch (Exception ex)
        {
            Status = $"启动失败:{ex.Message}";
            AppLog("[错误] " + ex);
        }
        finally
        {
            IsBusy = false;
            Progress = null;
            LaunchStage = -1;
        }
    }

    /// <summary>True only when no operation is running.</summary>
    private bool CanRunOperation() => !IsBusy;

    /// <summary>Append a line to the game log (thread-safe via UI marshal).</summary>
    private void AppLog(string line)
    {
        // Avalonia marshals collection cross-thread changes automatically.
        GameLog.Add(line);
    }
}
