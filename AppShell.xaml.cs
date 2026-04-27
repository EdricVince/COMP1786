namespace ProjectExpenseTracker;

public partial class AppShell : Shell
{
    public AppShell()
    {
        InitializeComponent();
        Routing.RegisterRoute(nameof(Views.ProjectDetailView), typeof(Views.ProjectDetailView));
    }
}
