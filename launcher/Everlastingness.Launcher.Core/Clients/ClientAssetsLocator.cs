using System;
using System.IO;

namespace Everlastingness.Launcher.Core.Clients;

/// <summary>
/// Resolves where the Everlastingness client artifacts (per-version client
/// jars, mixin, launchwrapper/modlauncher) are stored on disk. All artifacts
/// live under a single root directory separate from the vanilla game files so
/// a clean uninstall never touches the user's libraries/assets.
/// </summary>
public interface IClientAssetsLocator
{
    /// <summary>Root directory holding all Everlastingness client artifacts.</summary>
    string ClientDirectory { get; }

    /// <summary>Full path of a named artifact under <see cref="ClientDirectory"/>.</summary>
    string ResolveArtifact(string name);

    /// <summary>True if the named artifact exists under the client directory.</summary>
    bool ArtifactExists(string name);
}

/// <summary>
/// Default implementation. The client directory defaults to
/// <c>~/.everlastingness/client</c> and can be overridden by an environment
/// variable (<c>EVERLASTINGNESS_HOME</c>) or via constructor argument.
/// </summary>
public sealed class ClientAssetsLocator : IClientAssetsLocator
{
    private readonly string _clientDirectory;

    public ClientAssetsLocator(string? homeDirectory = null)
    {
        var home = homeDirectory
            ?? Environment.GetEnvironmentVariable("EVERLASTINGNESS_HOME")
            ?? DefaultHome();
        _clientDirectory = Path.Combine(home, "client");
        Directory.CreateDirectory(_clientDirectory);
    }

    public string ClientDirectory => _clientDirectory;

    public string ResolveArtifact(string name) =>
        Path.GetFullPath(Path.Combine(_clientDirectory, name));

    public bool ArtifactExists(string name) => File.Exists(ResolveArtifact(name));

    private static string DefaultHome()
    {
        // %USERPROFILE% on Windows, $HOME elsewhere.
        var profile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(profile, ".everlastingness");
    }
}
