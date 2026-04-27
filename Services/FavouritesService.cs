namespace ProjectExpenseTracker.Services;

/// <summary>
/// Persists favourite project IDs locally using MAUI Preferences.
/// Feature (h) — favourite projects.
///
/// Storage key: "fav_ids"
/// Format: comma-separated Firestore document IDs
/// </summary>
public class FavouritesService
{
    // Shared singleton instance across all Views
    public static readonly FavouritesService Shared = new FavouritesService();

    private const string PREF_KEY = "fav_ids";
    private readonly HashSet<string> _cache;

    public FavouritesService()
    {
        _cache = Load();
    }

    /// <summary>Returns true if the project is marked as a favourite.</summary>
    public bool IsFavourite(string firestoreId) => _cache.Contains(firestoreId);

    /// <summary>Adds to favourites if not present; removes if present.</summary>
    public void Toggle(string firestoreId)
    {
        if (!_cache.Add(firestoreId))
            _cache.Remove(firestoreId);
        Save();
    }

    /// <summary>Returns a snapshot of all favourite IDs.</summary>
    public IReadOnlySet<string> GetAll() => _cache;

    // ── Private ───────────────────────────────────────────────────────────────

    private static HashSet<string> Load()
    {
        var raw = Preferences.Default.Get(PREF_KEY, string.Empty);
        return string.IsNullOrEmpty(raw)
            ? new HashSet<string>()
            : new HashSet<string>(raw.Split(',', StringSplitOptions.RemoveEmptyEntries));
    }

    private void Save() =>
        Preferences.Default.Set(PREF_KEY, string.Join(",", _cache));
}
