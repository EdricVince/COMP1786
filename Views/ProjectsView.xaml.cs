using ProjectExpenseTracker.Models;
using ProjectExpenseTracker.Services;
using ProjectExpenseTracker.ViewModels;

namespace ProjectExpenseTracker.Views;

public partial class ProjectsView : ContentPage
{
    private static readonly FirebaseService _firebase = new FirebaseService();
    private List<Project> _allProjects = new();
    private string _searchQuery = string.Empty;

    public ProjectsView()
    {
        InitializeComponent();
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await LoadAsync();
    }

    private async Task LoadAsync()
    {
        lblResultCount.Text = "Loading...";
        _allProjects = await _firebase.GetProjectsAsync();
        ApplyFilter();
        UpdateStats();
    }

    private void ApplyFilter()
    {
        var filtered = string.IsNullOrWhiteSpace(_searchQuery)
            ? _allProjects
            : _allProjects.Where(p =>
                p.Name.Contains(_searchQuery, StringComparison.OrdinalIgnoreCase) ||
                p.Description.Contains(_searchQuery, StringComparison.OrdinalIgnoreCase) ||
                p.Manager.Contains(_searchQuery, StringComparison.OrdinalIgnoreCase)).ToList();

        cvProjects.ItemsSource = null;
        cvProjects.ItemsSource = filtered;
        lblResultCount.Text = filtered.Count == _allProjects.Count
            ? $"{_allProjects.Count} project(s)"
            : $"{filtered.Count} of {_allProjects.Count} project(s)";
    }

    private void UpdateStats()
    {
        double totalBudget = _allProjects.Sum(p => p.Budget);
        double totalSpent  = _allProjects.Sum(p => p.TotalSpent);
        double remaining   = totalBudget - totalSpent;
        int    usage       = totalBudget > 0 ? (int)(totalSpent / totalBudget * 100) : 0;

        lblTotalBudget.Text  = $"${totalBudget:N2}";
        lblTotalSpent.Text   = $"${totalSpent:N2}";
        lblRemaining.Text    = $"${remaining:N2}";
        lblUsage.Text        = $"{usage}%";
        lblProjectCount.Text = _allProjects.Count.ToString();
    }

    private async void OnRefreshing(object sender, EventArgs e)
    {
        await LoadAsync();
        refreshView.IsRefreshing = false;
    }

    private void OnSearchChanged(object sender, TextChangedEventArgs e)
    {
        _searchQuery = e.NewTextValue ?? string.Empty;
        ApplyFilter();
    }

    private async void OnProjectTapped(object sender, TappedEventArgs e)
    {
        if (e.Parameter is Project project)
        {
            await Shell.Current.GoToAsync(nameof(ProjectDetailView),
                new Dictionary<string, object> { [nameof(ProjectDetailView.Project)] = project });
        }
    }

    private async void OnDeleteAllTapped(object sender, EventArgs e)
    {
        bool confirm = await DisplayAlert(
            "Clear Cloud Data",
            "This will permanently delete ALL projects and expenses from Firebase. This cannot be undone.",
            "Delete All", "Cancel");

        if (!confirm) return;

        bool ok = await _firebase.DeleteAllProjectsAsync();
        if (ok)
        {
            _allProjects.Clear();
            ApplyFilter();
            UpdateStats();
            await DisplayAlert("Done", "All cloud data has been deleted.", "OK");
        }
        else
        {
            await DisplayAlert("Error", "Failed to delete cloud data. Check your connection.", "OK");
        }
    }
}
