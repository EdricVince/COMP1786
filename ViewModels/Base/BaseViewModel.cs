using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace ProjectExpenseTracker.ViewModels.Base;

/// <summary>
/// Base class for all ViewModels.
/// Provides INotifyPropertyChanged and common state properties (IsLoading, IsEmpty).
/// </summary>
public abstract class BaseViewModel : INotifyPropertyChanged
{
    private bool   _isLoading;
    private bool   _isEmpty;
    private string _title = string.Empty;

    public bool IsLoading
    {
        get => _isLoading;
        set => SetProperty(ref _isLoading, value);
    }

    public bool IsEmpty
    {
        get => _isEmpty;
        set => SetProperty(ref _isEmpty, value);
    }

    public string Title
    {
        get => _title;
        set => SetProperty(ref _title, value);
    }

    // ── INotifyPropertyChanged ────────────────────────────────────────────────

    public event PropertyChangedEventHandler? PropertyChanged;

    protected void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));

    protected bool SetProperty<T>(ref T field, T value,
        [CallerMemberName] string? name = null)
    {
        if (EqualityComparer<T>.Default.Equals(field, value)) return false;
        field = value;
        OnPropertyChanged(name);
        return true;
    }
}
