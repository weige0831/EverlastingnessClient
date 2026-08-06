using System;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.ProcessBuilder;
using Everlastingness.Launcher.Core.Clients;
using Everlastingness.Launcher.Core.Minecraft;

namespace Everlastingness.Launcher.TestHarness;

/// <summary>
/// Headless verification harness: downloads MC 1.7.10, injects the
/// Everlastingness client, launches the game process, and captures the
/// stdout/stderr log to check for [Everlastingness] mixin-applied evidence.
///
/// Does NOT attempt to interact with the GUI — it reads the game's own log
/// output to prove the mixin injected (the bootstrap log lines + any render
/// errors would appear there before the window even shows).
///
/// Run: dotnet run --project Everlastingness.Launcher.TestHarness -- [timeoutSeconds]
/// </summary>
internal static class Program
{
    static async Task Main(string[] args)
    {
        var timeoutSeconds = args.Length > 0 && int.TryParse(args[0], out var t) ? t : 120;
        var mcVersion = "1.7.10";
        var mcDir = System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".everlastingness", "game");
        System.IO.Directory.CreateDirectory(mcDir);

        Console.WriteLine($"[harness] game dir: {mcDir}");
        Console.WriteLine($"[harness] MC version: {mcVersion}");
        Console.WriteLine($"[harness] timeout: {timeoutSeconds}s");

        // 1. Find Java 8 (1.7.10 requires Java 8, not Java 21).
        var javaPath = FindJava8();
        if (javaPath == null)
        {
            Console.Error.WriteLine("[harness] FAIL: Java 8 not found. 1.7.10 requires Java 8.");
            Environment.Exit(1);
            return;
        }
        Console.WriteLine($"[harness] Java 8: {javaPath}");

        // 2. Download MC 1.7.10.
        var svc = new MinecraftLauncherService(mcDir);
        Console.WriteLine("[harness] downloading MC 1.7.10 ...");
        await svc.InstallAsync(mcVersion, null, CancellationToken.None);
        Console.WriteLine("\n[harness] download complete.");

        // 3. Build process with injection.
        var clientAssets = new ClientAssetsLocator();
        var profile = ClientProfiles.Find(mcVersion);
        if (profile == null)
        {
            Console.Error.WriteLine("[harness] FAIL: no client profile for " + mcVersion);
            Environment.Exit(1);
            return;
        }

        Console.WriteLine($"[harness] client jar ready: {ClientProfiles.IsArtifactReady(profile, clientAssets)}");
        if (!ClientProfiles.IsArtifactReady(profile, clientAssets))
        {
            Console.Error.WriteLine("[harness] FAIL: client jar not staged.");
            Environment.Exit(1);
            return;
        }

        var session = MSession.CreateOfflineSession("Tester");

        var launchOptions = new LaunchOptions
        {
            JavaPath = javaPath!,
            MaximumRamMb = 2048,
            MinimumRamMb = 512,
            InjectClient = true,
            Session = session
        };

        Console.WriteLine("[harness] building injected process ...");
        Process process;
        try
        {
            // First build without injection to see vanilla args.
            var vanillaProcess = await svc.BuildProcessAsync(mcVersion, new LaunchOptions
            {
                JavaPath = javaPath!,
                MaximumRamMb = 2048,
                MinimumRamMb = 512,
                InjectClient = false,
                Session = session
            }, null, clientAssets, CancellationToken.None);

            Console.WriteLine("[harness] VANILLA ArgumentList:");
            for (int i = 0; i < vanillaProcess.StartInfo.ArgumentList.Count; i++)
                Console.WriteLine($"  [{i}] {vanillaProcess.StartInfo.ArgumentList[i]}");
            Console.WriteLine($"[harness] VANILLA Arguments (string): '{vanillaProcess.StartInfo.Arguments}'");
            Console.WriteLine($"[harness] VANILLA FileName: {vanillaProcess.StartInfo.FileName}");

            // Now build with injection.
            process = await svc.BuildProcessAsync(mcVersion, launchOptions, profile, clientAssets, CancellationToken.None);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"[harness] FAIL: build process: {ex.Message}");
            Environment.Exit(1);
            return;
        }

        // 4. Launch and capture output.
        Console.WriteLine($"[harness] launch command: {process.StartInfo.FileName} {string.Join(" ", process.StartInfo.ArgumentList)}");
        process.StartInfo.RedirectStandardOutput = true;
        process.StartInfo.RedirectStandardError = true;
        process.StartInfo.UseShellExecute = false;

        var sawEverlastingness = false;
        var sawMixinTweaker = false;
        var sawClientTweaker = false;
        var sawRenderTick = false;
        var sawFatalError = false;

        process.OutputDataReceived += (_, e) =>
        {
            if (e.Data == null) return;
            Console.WriteLine($"[game:out] {e.Data}");
            CheckLine(e.Data);
        };
        process.ErrorDataReceived += (_, e) =>
        {
            if (e.Data == null) return;
            Console.WriteLine($"[game:err] {e.Data}");
            CheckLine(e.Data);
        };

        void CheckLine(string line)
        {
            if (line.Contains("Everlastingness", StringComparison.OrdinalIgnoreCase))
            {
                sawEverlastingness = true;
                Console.WriteLine($"[harness] >>> [Everlastingness] detected: {line.Trim()}");
            }
            if (line.Contains("MixinTweaker", StringComparison.OrdinalIgnoreCase))
                sawMixinTweaker = true;
            if (line.Contains("ClientTweaker", StringComparison.OrdinalIgnoreCase))
                sawClientTweaker = true;
            if (line.Contains("render", StringComparison.OrdinalIgnoreCase) &&
                line.Contains("Everlastingness", StringComparison.OrdinalIgnoreCase))
                sawRenderTick = true;
            if (line.Contains("FATAL", StringComparison.OrdinalIgnoreCase) ||
                line.Contains("Exception", StringComparison.OrdinalIgnoreCase))
                sawFatalError = true;
        }

        Console.WriteLine("[harness] starting game process ...");
        process.Start();
        process.BeginOutputReadLine();
        process.BeginErrorReadLine();

        // 5. Wait up to timeout.
        var exited = process.WaitForExit(timeoutSeconds * 1000);
        if (!exited)
        {
            Console.WriteLine($"[harness] timeout ({timeoutSeconds}s) — killing process.");
            try { process.Kill(true); } catch { }
        }

        Console.WriteLine();
        Console.WriteLine("=== HARNESST RESULT ===");
        Console.WriteLine($"  [Everlastingness] line seen : {sawEverlastingness}");
        Console.WriteLine($"  MixinTweaker loaded         : {sawMixinTweaker}");
        Console.WriteLine($"  ClientTweaker loaded        : {sawClientTweaker}");
        Console.WriteLine($"  render-tick event           : {sawRenderTick}");
        Console.WriteLine($"  FATAL/Exception seen        : {sawFatalError}");
        Console.WriteLine($"  process exited within limit : {exited}");
        Console.WriteLine("=======================");

        if (sawEverlastingness)
        {
            Console.WriteLine("[harness] PASS: Everlastingness client injected into MC process.");
            Environment.Exit(0);
        }
        else
        {
            Console.WriteLine("[harness] PARTIAL: process ran but no [Everlastingness] evidence in log.");
            Environment.Exit(2);
        }
    }

    static string? FindJava8()
    {
        // Common Java 8 locations on Windows.
        var candidates = new[]
        {
            @"C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe",
            @"C:\Program Files\Java\jre1.8.0_491\bin\java.exe",
            @"C:\Program Files\Java\jdk1.8.0_491\bin\java.exe",
        };
        foreach (var path in candidates)
        {
            if (System.IO.File.Exists(path))
                return path;
        }
        // Search for any Java 8 in Program Files.
        var pf = new[]
        {
            @"C:\Program Files\Java",
            @"C:\Program Files (x86)\Java",
        };
        foreach (var dir in pf)
        {
            if (!System.IO.Directory.Exists(dir)) continue;
            foreach (var sub in System.IO.Directory.GetDirectories(dir))
            {
                if (sub.Contains("1.8") || sub.Contains("jre8"))
                {
                    var exe = System.IO.Path.Combine(sub, "bin", "java.exe");
                    if (System.IO.File.Exists(exe)) return exe;
                }
            }
        }
        return null;
    }
}
