using System.Collections.ObjectModel;
using ProjectExpenseTracker.Models;
using ProjectExpenseTracker.Services;
using ProjectExpenseTracker.ViewModels.Base;

namespace ProjectExpenseTracker.ViewModels;

/// <summary>
/// ViewModel for ProjectsView.
/// Fetches all projects from Firebase and handles search + status filtering.
/// </summary>
public class ProjectsViewModel : BaseViewModel
{
    private readonly FirebaseService   _firebase;
    private readonly FavouritesService _favs;

    private List<Project> _allProjects = new();
    public  ObservableCollection<Project> Projects { get; } = new();

    // ── Filter state ──────────────────────────────────────────────────────────
    private string _searchQuery  = string.Empty;
    private string _statusFilter = "All";

    public string SearchQuery
    {
        get => _searchQuery;
        set { SetProperty(ref _searchQuery, value); ApplyFilter(); }
    }
    public string StatusFilter
    {
        get => _statusFilter;
        set { SetProperty(ref _statusFilter, value); ApplyFilter(); }
    }

    // ── Summary stats ─────────────────────────────────────────────────────────
    private string _totalBudget  = "$0.00";
    private string _totalSpent   = "$0.00";
    private string _projectCount = "0";

    public string TotalBudget  { get => _totalBudget;  set => SetProperty(ref _totalBudget,  value); }
    public string TotalSpent   { get => _totalSpent;   set => SetProperty(ref _totalSpent,   value); }
    public string ProjectCount { get => _projectCount; set => SetProperty(ref _projectCount, value); }

    public string[] StatusOptions { get; } = { "All", "Active", "Completed", "On Hold" };

    public ProjectsViewModel(FirebaseService firebase, FavouritesService favs)
    {
        _firebase = firebase;
        _favs     = favs;
        Title     = "Projects";
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    public async Task LoadAsync()
    {
        IsLoading = true;
        IsEmpty   = false;
        try
        {
            _allProjects = await _firebase.GetProjectsAsync();
            ApplyFilter();
            UpdateStats();
        }
        finally { IsLoading = false; }
    }

    // ── Favourites ────────────────────────────────────────────────────────────

    public bool IsFavourite(string id)    => _favs.IsFavourite(id);
    public void ToggleFavourite(string id) => _favs.Toggle(id);

    // ── Private helpers ───────────────────────────────────────────────────────

    private void ApplyFilter()
    {
        var filtered = _allProjects.AsEnumerable();

        if (!string.IsNullOrWhiteSpace(SearchQuery))
        {
            var q = SearchQuery.Trim().ToLower();
            filtered = filtered.Where(p =>
                p.Name.ToLower().Contains(q)        ||
                p.Description.ToLower().Contains(q) ||
                p.StartDate.Contains(q)             ||
                p.EndDate.Contains(q));
        }

        if (StatusFilter != "All")
            filtered = filtered.Where(p => p.Status == StatusFilter);

        Projects.Clear();
        foreach (var p in filtered) Projects.Add(p);
        IsEmpty = !Projects.Any();
    }

    private void UpdateStats()
    {
        TotalBudget  = $"${_allProjects.Sum(p => p.Budget):N2}";
        TotalSpent   = $"${_allProjects.Sum(p => p.TotalSpent):N2}";
        ProjectCount = _allProjects.Count.ToString();
    }
}
