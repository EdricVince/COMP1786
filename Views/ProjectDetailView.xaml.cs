using ProjectExpenseTracker.Models;
using ProjectExpenseTracker.Services;

namespace ProjectExpenseTracker.Views;

[QueryProperty(nameof(Project), nameof(Project))]
public partial class ProjectDetailView : ContentPage
{
    private static readonly FavouritesService _favs = FavouritesService.Shared;
    private Project? _project;

    public ProjectDetailView()
    {
        InitializeComponent();
    }

    public Project Project
    {
        set
        {
            _project = value;
            LoadProject(value);
        }
    }

    private void LoadProject(Project p)
    {
        // Title bar
        lblTitle.Text = p.Name;

        // Budget card
        lblProjectName.Text  = p.Name;
        lblBudgetAmount.Text = $"Budget: ${p.Budget:N2}";
        lblSpent.Text        = $"${p.TotalSpent:N2}";
        lblRemaining.Text    = $"${p.Remaining:N2}";
        lblUsage.Text        = $"{p.UsagePercent}%";

        // Animate progress bar after layout
        Dispatcher.Dispatch(() =>
        {
            var parent = progressBar.Parent as Grid;
            if (parent != null)
            {
                double parentWidth = parent.Width > 0 ? parent.Width : 300;
                progressBar.WidthRequest = parentWidth * p.UsagePercent / 100.0;
            }
        });

        // Info card
        lblCode.Text        = p.Code;
        lblManager.Text     = p.Manager;
        lblDates.Text       = $"{p.StartDate}  →  {p.EndDate}";
        lblDescription.Text = string.IsNullOrWhiteSpace(p.Description)
                              ? "—" : p.Description;

        // Status chip
        lblStatus.Text                 = p.Status;
        lblStatus.TextColor            = Color.FromArgb(p.StatusColor);
        frmStatus.BackgroundColor      = Color.FromArgb(p.StatusBg);

        // Expenses
        cvExpenses.ItemsSource    = p.Expenses;
        lblExpenseCount.Text      = $"{p.Expenses.Count} expense(s)";

        // Favourite button
        UpdateFavButton();
    }

    private void UpdateFavButton()
    {
        if (_project == null) return;
        bool isFav = _favs.IsFavourite(_project.FirestoreId);
        btnFavShell.Text      = isFav ? "♥" : "♡";
        btnFavShell.TextColor = isFav
            ? Color.FromArgb("#FF7043")
            : Colors.White;
    }

    private void OnFavouriteTapped(object sender, EventArgs e)
    {
        if (_project == null) return;
        _favs.Toggle(_project.FirestoreId);
        UpdateFavButton();
    }

    private async void OnBackTapped(object sender, EventArgs e)
    {
        await Shell.Current.GoToAsync("..");
    }
}
