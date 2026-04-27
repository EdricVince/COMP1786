namespace ProjectExpenseTracker.Models;

/// <summary>
/// Project fetched from Firebase Firestore.
/// Computed properties (TotalSpent, Remaining, UsagePercent) are derived
/// from the Expenses list which is loaded separately on the detail screen.
/// </summary>
public class Project
{
    public string FirestoreId           { get; set; } = string.Empty;
    public string Code                  { get; set; } = string.Empty;
    public string Name                  { get; set; } = string.Empty;
    public string Description           { get; set; } = string.Empty;
    public string StartDate             { get; set; } = string.Empty;
    public string EndDate               { get; set; } = string.Empty;
    public string Manager               { get; set; } = string.Empty;
    public string Status                { get; set; } = string.Empty;
    public double Budget                { get; set; }
    public string SpecialRequirements   { get; set; } = string.Empty;
    public string ClientInfo            { get; set; } = string.Empty;
    public string CreatedAt             { get; set; } = string.Empty;

    public List<Expense> Expenses { get; set; } = new();

    // ── Computed ──────────────────────────────────────────────────────────────
    public double TotalSpent    => Expenses.Sum(e => e.Amount);
    public double Remaining     => Budget - TotalSpent;
    public int    UsagePercent  => Budget > 0
        ? (int)Math.Min(TotalSpent / Budget * 100, 100) : 0;

    // ── Status UI helpers ─────────────────────────────────────────────────────
    public string StatusColor => Status switch
    {
        "Active"    => "#1A9E4A",
        "Completed" => "#2563EB",
        _           => "#D97706"
    };
    public string StatusBg => Status switch
    {
        "Active"    => "#E8F9EE",
        "Completed" => "#E8F0FF",
        _           => "#FFF4E5"
    };
}
