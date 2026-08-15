using System;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.ProcessBuilder;
using Everlastingness.Launcher.Core.Clients;
using Everlastingness.Launcher.Core.Minecraft;

namespace Everlastingness.Launcher.TestHarness;

internal static class Win32
{
    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    public static extern IntPtr FindWindow(string lpClassName, string lpWindowTitle);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool SetCursorPos(int x, int y);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

    public const int SW_RESTORE = 9;
    public const uint INPUT_MOUSE = 0;
    public const uint INPUT_KEYBOARD = 1;
    public const uint MOUSEEVENTF_MOVE = 0x0001;
    public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    public const uint MOUSEEVENTF_LEFTUP = 0x0004;
    public const uint MOUSEEVENTF_ABSOLUTE = 0x8000;
    public const uint KEYEVENTF_KEYDOWN = 0x0000;
    public const uint KEYEVENTF_KEYUP = 0x0002;
    public const ushort VK_F1 = 0x70;
    public const ushort VK_F2 = 0x71;
    public const ushort VK_ESCAPE = 0x1B;

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }

    [StructLayout(LayoutKind.Sequential)]
    public struct MOUSEINPUT
    {
        public int dx;
        public int dy;
        public uint mouseData;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Explicit)]
    public struct INPUTUNION
    {
        [FieldOffset(0)] public MOUSEINPUT mi;
        [FieldOffset(0)] public KEYBDINPUT ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT
    {
        public uint type;
        public INPUTUNION u;
    }

    /// <summary>
    /// Send a hardware-level key press via SendInput. This goes through the
    /// real input queue, so LWJGL's DirectInput-based Mouse/Keyboard pick it
    /// up (unlike PostMessage, which LWJGL ignores).
    /// </summary>
    public static void SendKey(ushort vk, bool down)
    {
        var input = new INPUT
        {
            type = INPUT_KEYBOARD,
            u = new INPUTUNION
            {
                ki = new KEYBDINPUT
                {
                    wVk = vk,
                    wScan = 0,
                    dwFlags = down ? KEYEVENTF_KEYDOWN : KEYEVENTF_KEYUP,
                    time = 0,
                    dwExtraInfo = IntPtr.Zero
                }
            }
        };
        SendInput(1, new[] { input }, System.Runtime.InteropServices.Marshal.SizeOf<INPUT>());
    }

    /// <summary>
    /// Send a hardware-level left-click at the given screen-space pixel
    /// coordinates via SendInput. LWJGL's DirectInput Mouse receives this.
    /// </summary>
    public static void SendClick(int x, int y)
    {
        SetCursorPos(x, y);
        System.Threading.Thread.Sleep(60);
        var down = new INPUT
        {
            type = INPUT_MOUSE,
            u = new INPUTUNION { mi = new MOUSEINPUT { dx = 0, dy = 0, mouseData = 0,
                dwFlags = MOUSEEVENTF_LEFTDOWN, time = 0, dwExtraInfo = IntPtr.Zero } }
        };
        var up = new INPUT
        {
            type = INPUT_MOUSE,
            u = new INPUTUNION { mi = new MOUSEINPUT { dx = 0, dy = 0, mouseData = 0,
                dwFlags = MOUSEEVENTF_LEFTUP, time = 0, dwExtraInfo = IntPtr.Zero } }
        };
        SendInput(1, new[] { down }, System.Runtime.InteropServices.Marshal.SizeOf<INPUT>());
        System.Threading.Thread.Sleep(60);
        SendInput(1, new[] { up }, System.Runtime.InteropServices.Marshal.SizeOf<INPUT>());
    }

    /// <summary>Click at a fractional position (0..1) within the given window.</summary>
    public static void ClickInWindow(IntPtr hwnd, double fx, double fy)
    {
        GetWindowRect(hwnd, out var r);
        int cx = r.Left + (int)((r.Right - r.Left) * fx);
        int cy = r.Top + (int)((r.Bottom - r.Top) * fy);
        SendClick(cx, cy);
    }
}

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
        var mcVersion = args.Length > 1 ? args[1] : "1.7.10";
        var mcDir = System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".everlastingness", "game");
        System.IO.Directory.CreateDirectory(mcDir);

        Console.WriteLine($"[harness] game dir: {mcDir}");
        Console.WriteLine($"[harness] MC version: {mcVersion}");
        Console.WriteLine($"[harness] timeout: {timeoutSeconds}s");

        // Only 1.7.10 (LaunchWrapper, own v1_7_10 tweaker set) needs Java 8;
        // 1.8.9–1.12.2 inject via the agent on JDK 21 like the modern versions.
        var javaPath = mcVersion == "1.7.10" ? FindJava8() : @"C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe";
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

        // Persist full stdout/stderr to a file for post-mortem APPLIED analysis.
        var logPath = System.IO.Path.Combine(mcDir, "everlastingness-harness.log");
        var logFile = new System.IO.StreamWriter(logPath) { AutoFlush = true };
        Console.WriteLine($"[harness] full log -> {logPath}");

        var sawEverlastingness = false;
        var sawMixinTweaker = false;
        var sawClientTweaker = false;
        var sawRenderTick = false;
        var sawFatalError = false;
        var mixinAppliedCount = 0;
        var mixinPrepareCount = 0;
        var modulesRegistered = 0;
        var sawMainMenu = false;
        var inWorld = false;

        process.OutputDataReceived += (_, e) =>
        {
            if (e.Data == null) return;
            var line = e.Data;
            // Unwrap log4j CDATA if present, for grepping.
            Console.WriteLine($"[game:out] {line}");
            logFile.WriteLine(line);
            CheckLine(line);
        };
        process.ErrorDataReceived += (_, e) =>
        {
            if (e.Data == null) return;
            var line = e.Data;
            Console.WriteLine($"[game:err] {line}");
            logFile.WriteLine("[stderr] " + line);
            CheckLine(line);
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
            // Mixin lifecycle markers (verbose): "Preparing" + "Successfully loaded mixin config" + APPLIED.
            if (line.Contains("Successfully loaded mixin config", StringComparison.OrdinalIgnoreCase) ||
                line.Contains("Mixin config ", StringComparison.OrdinalIgnoreCase) && line.Contains("successfully", StringComparison.OrdinalIgnoreCase))
            {
                mixinPrepareCount++;
            }
            // The reliable applied marker: "Mixing <mixin> from <config> into <target>"
            // (verbose mode emits this per successfully-applied mixin). We count
            // unique mixin names to avoid double-counting duplicate log lines.
            if (line.Contains("Mixing ", StringComparison.OrdinalIgnoreCase) &&
                line.Contains(" from ", StringComparison.OrdinalIgnoreCase) &&
                line.Contains(" into ", StringComparison.OrdinalIgnoreCase))
            {
                mixinAppliedCount++;
                Console.WriteLine($"[harness] >>> mixin applied #{mixinAppliedCount}: {line.Trim()}");
            }
            if (line.Contains("APPLIED", StringComparison.OrdinalIgnoreCase) && mixinAppliedCount == 0)
            {
                // Fallback for non-verbose logs.
                mixinAppliedCount++;
            }
            // Module registration counters from ClientTweaker stdout.
            if (line.Contains("modules registered", StringComparison.OrdinalIgnoreCase))
            {
                // Try to parse the count if present, else just mark seen.
                modulesRegistered = Math.Max(modulesRegistered, 1);
            }
            if (line.Contains("render", StringComparison.OrdinalIgnoreCase) &&
                line.Contains("Everlastingness", StringComparison.OrdinalIgnoreCase))
                sawRenderTick = true;
            if (line.Contains("FATAL", StringComparison.OrdinalIgnoreCase))
                sawFatalError = true;
            // Main menu / world detection markers.
            if (line.Contains("Sound engine started", StringComparison.OrdinalIgnoreCase))
                sawMainMenu = true;
            if (line.Contains("AutoWorld", StringComparison.OrdinalIgnoreCase) &&
                line.Contains("world launched", StringComparison.OrdinalIgnoreCase))
            {
                inWorld = true;
                Console.WriteLine($"[harness] >>> AutoWorld launched: {line.Trim()}");
            }
            if (line.Contains("Loading world", StringComparison.OrdinalIgnoreCase) ||
                line.Contains("Preparing start region", StringComparison.OrdinalIgnoreCase) ||
                line.Contains("Preparing spawn", StringComparison.OrdinalIgnoreCase) ||
                (line.Contains("Loaded ", StringComparison.OrdinalIgnoreCase) && line.Contains("spawn", StringComparison.OrdinalIgnoreCase)) ||
                line.Contains("Connected to ", StringComparison.OrdinalIgnoreCase) ||
                line.Contains("Saving and pausing", StringComparison.OrdinalIgnoreCase) ||
                line.Contains("Saving chunks", StringComparison.OrdinalIgnoreCase))
            {
                inWorld = true;
                Console.WriteLine($"[harness] >>> in-world marker: {line.Trim()}");
            }
        }
        Console.WriteLine("[harness] launch args: " + process.StartInfo.Arguments);

        Console.WriteLine("[harness] starting game process ...");
        process.Start();
        process.BeginOutputReadLine();
        process.BeginErrorReadLine();

        // 5. Auto-drive into a singleplayer world via SendInput (hardware-level
        // input that LWJGL's DirectInput Mouse/Keyboard receive, unlike
        // PostMessage which LWJGL ignores), then capture per-stage screenshots.
        //
        // MC 1.7.10 GUI button layout (default 854x480, scales proportionally
        // with the resizable LWJGL window):
        //   Main menu "Singleplayer" button : ~50% width, ~38% height
        //   "Create New World" button       : centered bottom ~78% height
        //   "Create New World" (confirm)    : left-bottom ~78% height (after
        //                                      entering a world name, which the
        //                                      menu pre-fills as "New World").
        System.Threading.Tasks.Task.Run(async () =>
        {
            // Wait for main menu (Sound engine started).
            for (int i = 0; i < 80 && !sawMainMenu; i++) await Task.Delay(500);
            await Task.Delay(3500); // extra time for menu render + GL settle

            try
            {
                IntPtr hwnd = Win32.FindWindow("LWJGL", null);
                if (hwnd == IntPtr.Zero) hwnd = Win32.FindWindow(null, "Minecraft 1.7.10");
                if (hwnd == IntPtr.Zero)
                {
                    Console.WriteLine("[harness] MC window not found by FindWindow");
                    return;
                }
                // Restore + focus so SendInput targets it.
                Win32.ShowWindow(hwnd, Win32.SW_RESTORE);
                Win32.SetForegroundWindow(hwnd);
                await Task.Delay(500);
                Console.WriteLine($"[harness] found MC window (hwnd={hwnd}), AutoWorld will drive world creation");

                // --- Stage A: main menu screenshot ---
                await CaptureScreenshot(hwnd, "main menu");

                // AutoWorld mixin handles world creation (no clicks needed).
                // --- Stage D: wait for world generation ---
                Console.WriteLine("[harness] waiting for AutoWorld to generate world...");
                for (int i = 0; i < 90 && !inWorld; i++)
                {
                    await Task.Delay(1000);
                }
                if (inWorld)
                {
                    Console.WriteLine("[harness] >>> IN WORLD — capturing per-feature screenshots");
                    await Task.Delay(5000); // let chunks render + HUD settle

                    // 1. HUD (XYZ/FPS/CPS/Clock) — default state
                    await CaptureScreenshot(hwnd, "hud-default");

                    // 2. Fullbright — already active (gamma override), capture
                    await CaptureScreenshot(hwnd, "fullbright");

                    // 3. Custom crosshair — active, capture
                    await CaptureScreenshot(hwnd, "crosshair");

                    // 4. Block outline — look at a block (default crosshair)
                    await CaptureScreenshot(hwnd, "block-outline");

                    // 5. CPS — left-click a few times to register clicks
                    Win32.ClickInWindow(hwnd, 0.50, 0.50);
                    await Task.Delay(150);
                    Win32.ClickInWindow(hwnd, 0.50, 0.50);
                    await Task.Delay(150);
                    Win32.ClickInWindow(hwnd, 0.50, 0.50);
                    await Task.Delay(400);
                    await CaptureScreenshot(hwnd, "cps");

                    // 6. Zoom — press and hold C to test FOV (ZoomModule hold keybind)
                    Win32.SendKey(0x43 /*C*/, true); // key down
                    await Task.Delay(1500);
                    await CaptureScreenshot(hwnd, "zoom-fov");
                    Win32.SendKey(0x43, false); // key up

                    // 7. Perspective — press G to cycle to 3rd-person view
                    Win32.SendKey(0x47 /*G*/, true);
                    await Task.Delay(150);
                    Win32.SendKey(0x47, false);
                    await Task.Delay(1000);
                    await CaptureScreenshot(hwnd, "perspective");
                }
                else
                {
                    Console.WriteLine("[harness] WARNING: did not detect in-world; capturing whatever is on screen");
                    await CaptureScreenshot(hwnd, "post-autoworld");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("[harness] world-drive attempt: " + ex.Message);
            }
        });

        // 6. Wait up to timeout.
        var exited = process.WaitForExit(timeoutSeconds * 1000);
        if (!exited)
        {
            Console.WriteLine($"[harness] timeout ({timeoutSeconds}s) — killing process.");
            try { process.Kill(true); } catch { }
        }
        logFile.Close();

        Console.WriteLine();
        Console.WriteLine("=== HARNESST RESULT ===");
        Console.WriteLine($"  [Everlastingness] line seen : {sawEverlastingness}");
        Console.WriteLine($"  MixinTweaker loaded         : {sawMixinTweaker}");
        Console.WriteLine($"  ClientTweaker loaded        : {sawClientTweaker}");
        Console.WriteLine($"  mixin config prepared        : {mixinPrepareCount}");
        Console.WriteLine($"  mixin APPLIED count         : {mixinAppliedCount} (expect >= 11)");
        Console.WriteLine($"  render-tick event           : {sawRenderTick}");
        Console.WriteLine($"  FATAL seen                  : {sawFatalError}");
        Console.WriteLine($"  reached main menu           : {sawMainMenu}");
        Console.WriteLine($"  reached in-world            : {inWorld}");
        Console.WriteLine($"  process exited within limit : {exited}");
        Console.WriteLine($"  screenshots dir             : {System.IO.Path.Combine(mcDir, "screenshots")}");
        Console.WriteLine("=======================");

        // PASS requires: tweaker chain ran + mixins applied + no FATAL.
        var pass = sawEverlastingness && sawClientTweaker && mixinAppliedCount >= 11 && !sawFatalError;
        if (pass)
        {
            Console.WriteLine($"[harness] PASS: {mixinAppliedCount} mixins applied, client bootstrapped.");
            Environment.Exit(0);
        }
        else
        {
            Console.WriteLine($"[harness] PARTIAL: applied={mixinAppliedCount}, fatal={sawFatalError}. See {logPath}");
            Environment.Exit(2);
        }
    }

    /// <summary>
    /// Send F2 to the focused MC window to capture a screenshot, tagging the
    /// stage name in the harness log so the resulting PNGs can be correlated.
    /// MC saves screenshots to &lt;gameDir&gt;/screenshots/ with a timestamp name.
    ///
    /// <p>Uses PostMessage (not SendInput) for F2 because MC 1.7.10's LWJGL
    /// Keyboard reads from the Windows message queue, where PostMessage lands
    /// reliably. (Mouse clicks, by contrast, go through DirectInput and need
    /// SendInput — see ClickInWindow.)</p>
    /// </summary>
    static async System.Threading.Tasks.Task CaptureScreenshot(IntPtr hwnd, string stage)
    {
        // Focus the window so the keyboard event lands.
        Win32.SetForegroundWindow(hwnd);
        await System.Threading.Tasks.Task.Delay(120);
        // PostMessage F2 down+up with a clear gap so MC's Keyboard.next() sees
        // a distinct rising edge each time (avoids the "only first screenshot
        // saves" problem of back-to-back events).
        const uint WM_KEYDOWN = 0x0100;
        const uint WM_KEYUP = 0x0101;
        Win32.PostMessage(hwnd, WM_KEYDOWN, (IntPtr)Win32.VK_F2, (IntPtr)0x003C0001);
        await System.Threading.Tasks.Task.Delay(220);
        Win32.PostMessage(hwnd, WM_KEYUP, (IntPtr)Win32.VK_F2, (IntPtr)0xC03C0001);
        await System.Threading.Tasks.Task.Delay(700); // MC writes the PNG
        Console.WriteLine($"[harness] screenshot captured for stage: {stage}");
    }

    static string? FindJava8()    {
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
