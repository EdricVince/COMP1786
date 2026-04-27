using System.Collections.ObjectModel;
using ProjectExpenseTracker.Models;
using ProjectExpenseTracker.Services;
using ProjectExpenseTracker.ViewModels.Base;

namespace ProjectExpenseTracker.ViewModels;

/// <summary>
/// ViewModel for ProjectDetailView.
/// Loads expenses for the selected project and manages the favourite toggle.
/// </summary>
public class ProjectDetailViewModel : BaseViewModel
{
    private readonly FirebaseService   _firebase;
    private readonly FavouritesService _favs;

    public Project? Project { get; private set; }
    public ObservableCollection<Expense> Expenses { get; } = new();

    private bool   _isFavourite;
    private string _favIcon = "♡";

    public bool IsFavourite
    {
        get => _isFavourite;
        set { SetProperty(ref _isFavourite, value); FavIcon = value ? "♥" : "♡"; }
    }
    public string FavIcon
    {
        get => _favIcon;
        set => SetProperty(ref _favIcon, value);
    }

    // ── Computed display strings ──────────────────────────────────────────────
    public string BudgetText    => Project != null ? $"${Project.Budget:N2}"             : "$0.00";
    public string SpentText     => Project != null ? $"Spent: ${Project.TotalSpent:N2}"  : "Spent: $0";
    public string RemainingText => Project != null ? $"Remaining: ${Project.Remaining:N2}" : "Remaining: $0";
    public double UsageFraction => Project?.UsagePercent / 100.0 ?? 0;
    public string PercentText   => $"{Project?.UsagePercent ?? 0}%";
    public string ExpenseCount  => $"{Expenses.Count} expense(s)";

    public ProjectDetailViewModel(FirebaseService firebase, FavouritesService favs)
    {
        _firebase = firebase;
        _favs     = favs;
    }

    public async Task LoadAsync(Project project)
    {
        Project     = project;
        Title       = project.Name;
        IsFavourite = _favs.IsFavourite(project.FirestoreId);
        IsLoading   = true;

        try
        {
            var expenses = await _firebase.GetExpensesAsync(project.FirestoreId);
            Expenses.Clear();
            foreach (var e in expenses) Expenses.Add(e);
        }
        finally
        {
            IsLoading = false;
            // Refresh all computed strings after expenses are loaded
            OnPropertyChanged(nameof(BudgetText));
            OnPropertyChanged(nameof(SpentText));
            OnPropertyChanged(nameof(RemainingText));
            OnPropertyChanged(nameof(UsageFraction));
            OnPropertyChanged(nameof(PercentText));
            OnPropertyChanged(nameof(ExpenseCount));
        }
    }

    public void ToggleFavourite()
    {
        if (Project == null) return;
        _favs.Toggle(Project.FirestoreId);
        IsFavourite = _favs.IsFavourite(Project.FirestoreId);
    }
}
