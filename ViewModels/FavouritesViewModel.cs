using System.Collections.ObjectModel;
using ProjectExpenseTracker.Models;
using ProjectExpenseTracker.Services;
using ProjectExpenseTracker.ViewModels.Base;

namespace ProjectExpenseTracker.ViewModels;

/// <summary>
/// ViewModel for FavouritesView.
/// Loads only the projects the user has starred.
/// Feature (h) — favourite projects.
/// </summary>
public class FavouritesViewModel : BaseViewModel
{
    private readonly FirebaseService   _firebase;
    private readonly FavouritesService _favs;

    public ObservableCollection<Project> Favourites { get; } = new();

    public FavouritesViewModel(FirebaseService firebase, FavouritesService favs)
    {
        _firebase = firebase;
        _favs     = favs;
        Title     = "Favourites";
    }

    public async Task LoadAsync()
    {
        IsLoading = true;
        try
        {
            var all    = await _firebase.GetProjectsAsync();
            var favIds = _favs.GetAll();
            Favourites.Clear();
            foreach (var p in all.Where(p => favIds.Contains(p.FirestoreId)))
                Favourites.Add(p);
            IsEmpty = !Favourites.Any();
        }
        finally { IsLoading = false; }
    }

    public void Remove(string firestoreId)
    {
        _favs.Toggle(firestoreId);
        var item = Favourites.FirstOrDefault(p => p.FirestoreId == firestoreId);
        if (item != null) Favourites.Remove(item);
        IsEmpty = !Favourites.Any();
    }
}
