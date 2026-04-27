using ProjectExpenseTracker.Models;
using ProjectExpenseTracker.Services;
using ProjectExpenseTracker.ViewModels;

namespace ProjectExpenseTracker.Views;

public partial class FavouritesView : ContentPage
{
    private static readonly FirebaseService _firebase = new FirebaseService();
    private readonly FavouritesViewModel _vm;

    public FavouritesView()
    {
        InitializeComponent();
        _vm = new FavouritesViewModel(_firebase, FavouritesService.Shared);
        BindingContext = _vm;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await _vm.LoadAsync();
        Refresh();
    }

    private void Refresh()
    {
        cvFavourites.ItemsSource = null;
        cvFavourites.ItemsSource = _vm.Favourites;
        lblFavCount.Text = $"{_vm.Favourites.Count} project(s)";
    }

    private void OnRemoveFavourite(object sender, EventArgs e)
    {
        if (sender is Button btn && btn.CommandParameter is Project p)
        {
            _vm.Remove(p.FirestoreId);
            Refresh();
        }
    }
}
