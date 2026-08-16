using System;
using System.Globalization;
using Avalonia.Data.Converters;
using Avalonia.Media;

namespace Everlastingness.Launcher.ViewModels;

/// <summary>NavIndex == N → visible (page switcher).</summary>
public class NavIndexConverter : IValueConverter
{
    public static readonly NavIndexConverter EqualTo0 = new() { Index = 0 };
    public static readonly NavIndexConverter EqualTo1 = new() { Index = 1 };
    public static readonly NavIndexConverter EqualTo2 = new() { Index = 2 };
    public static readonly NavIndexConverter EqualTo3 = new() { Index = 3 };
    public static readonly NavIndexConverter EqualTo4 = new() { Index = 4 };

    public int Index { get; init; }

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
        => value is int i && i == Index;

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => throw new NotSupportedException();
}

/// <summary>Launch stage index → step dot color (done=green, current=accent, future=gray).</summary>
public class StageBrushConverter : IMultiValueConverter
{
    public static readonly StageBrushConverter Default = new();

    public object Convert(System.Collections.Generic.IList<object?> values, Type targetType,
                          object? parameter, CultureInfo culture)
    {
        int stage = values.Count > 0 && values[0] is int s ? s : -1;
        int index = values.Count > 1 && values[1] is int i ? i : -1;
        if (index < 0) return new SolidColorBrush(Colors.Transparent);
        if (index < stage) return new SolidColorBrush(Color.Parse("#4CC38A"));   // done
        if (index == stage) return new SolidColorBrush(Color.Parse("#4CC2FF"));  // active
        return new SolidColorBrush(Color.Parse("#5A6272"));                       // pending
    }
}

/// <summary>Module-ready → dot color.</summary>
public class ReadyDotConverter : IMultiValueConverter
{
    public static readonly ReadyDotConverter Default = new();

    public object Convert(System.Collections.Generic.IList<object?> values, Type targetType,
                          object? parameter, CultureInfo culture)
    {
        bool ready = values.Count > 0 && values[0] is bool b && b;
        return new SolidColorBrush(ready ? Color.Parse("#4CC38A") : Color.Parse("#C9A227"));
    }
}
