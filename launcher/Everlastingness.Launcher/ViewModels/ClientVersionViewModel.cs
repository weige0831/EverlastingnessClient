using Everlastingness.Launcher.Core.Clients;

namespace Everlastingness.Launcher.ViewModels;

/// <summary>
/// A single row in the client-version list. Reflects whether the compiled
/// Everlastingness client jar for a Minecraft version is present on disk.
/// </summary>
public sealed class ClientVersionViewModel
{
    public string MinecraftVersion { get; init; } = "";
    public MappingEra Era { get; init; }

    /// <summary>Human-readable injection era label.</summary>
    public string EraLabel => Era switch
    {
        MappingEra.LegacyLaunchWrapper => "Legacy · LaunchWrapper",
        MappingEra.ModernModLauncher => "Modern · ModLauncher",
        _ => Era.ToString()
    };

    /// <summary>True once the client jar exists in the client directory.</summary>
    public bool Ready { get; init; }

    public override string ToString() => $"{MinecraftVersion} ({EraLabel})";
}
