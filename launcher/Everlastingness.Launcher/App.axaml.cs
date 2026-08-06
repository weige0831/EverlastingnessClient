using System;
using System.IO;
using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Markup.Xaml;
using Everlastingness.Launcher.Core.Clients;
using Everlastingness.Launcher.Core.Minecraft;
using Everlastingness.Launcher.ViewModels;
using Everlastingness.Launcher.Views;

namespace Everlastingness.Launcher;

public partial class App : Application
{
    public override void Initialize()
    {
        AvaloniaXamlLoader.Load(this);
    }

    public override void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            desktop.MainWindow = new MainWindow
            {
                DataContext = BuildViewModel(),
            };
        }

        base.OnFrameworkInitializationCompleted();
    }

    /// <summary>
    /// Compose the Core services and hand them to the view model. Settings are
    /// persisted next to the launcher; the Azure client id is read from there
    /// (configure it once, in settings.json).
    /// </summary>
    private static MainViewModel BuildViewModel()
    {
        var settingsPath = SettingsFilePath();
        var settings = LauncherSettings.Load(settingsPath);

        // The Azure public-client app id for MSAL login. It is read from the
        // LAUNCHER_AZURE_CLIENT_ID env var first, then settings.json, so a
        // developer can run without editing code. It is NOT a secret.
        var clientId = Environment.GetEnvironmentVariable("LAUNCHER_AZURE_CLIENT_ID")
                       ?? settings.AzureClientId;

        var gameDir = DefaultGameDirectory();
        var clientAssets = new ClientAssetsLocator();
        var launcherService = new MinecraftLauncherService(gameDir);
        var authService = string.IsNullOrEmpty(clientId)
            ? null  // auth unavailable until configured; UI degrades to offline
            : new MinecraftAuthService(clientId, Path.Combine(gameDir, "everlastingness_accounts.json"));

        // If auth is unavailable, create an offline-only view model by passing a
        // null auth — handled via an overload below.
        return authService is null
            ? new MainViewModel(launcherService, OfflineAuth(), clientAssets, settings)
            : new MainViewModel(launcherService, authService, clientAssets, settings);
    }

    /// <summary>An offline-only auth stub used when no Azure client id is configured.</summary>
    private static MinecraftAuthService OfflineAuth() =>
        // Passing a sentinel id keeps the type constructible; LoginNewAccount
        // will surface a clear error until a real id is configured. The launcher
        // still works for offline/cracked play in this state.
        new MinecraftAuthService("00000000-0000-0000-0000-000000000000",
            Path.Combine(DefaultGameDirectory(), "everlastingness_accounts.json"));

    private static string DefaultGameDirectory()
    {
        // Use a dedicated game dir under the user profile so we never touch the
        // official launcher's .minecraft by default.
        var profile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(profile, ".everlastingness", "game");
    }

    private static string SettingsFilePath() =>
        Path.Combine(DefaultGameDirectory(), "settings.json");
}
