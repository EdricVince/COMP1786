namespace ProjectExpenseTracker.Models;

/// <summary>
/// Expense fetched from a project's sub-collection in Firestore.
/// </summary>
public class Expense
{
    public string FirestoreId   { get; set; } = string.Empty;
    public string Code          { get; set; } = string.Empty;
    public string Date          { get; set; } = string.Empty;
    public double Amount        { get; set; }
    public string Currency      { get; set; } = "USD";
    public string Type          { get; set; } = string.Empty;
    public string PaymentMethod { get; set; } = string.Empty;
    public string Claimant      { get; set; } = string.Empty;
    public string Status        { get; set; } = string.Empty;
    public string Description   { get; set; } = string.Empty;
    public string Location      { get; set; } = string.Empty;
    public string CreatedAt     { get; set; } = string.Empty;

    // ── Computed ──────────────────────────────────────────────────────────────
    public string FormattedAmount => $"{Currency} {Amount:N2}";

    public string TypeEmoji => Type switch
    {
        "Travel"            => "✈️",
        "Equipment"         => "🖥️",
        "Materials"         => "📦",
        "Services"          => "🔧",
        "Software/Licenses" => "💻",
        "Labour Costs"      => "👷",
        "Utilities"         => "💡",
        "Food"              => "🍔",
        "Shopping"          => "🛍️",
        _                   => "💳"
    };

    // ── Status UI helpers ─────────────────────────────────────────────────────
    public string StatusColor => Status switch
    {
        "Paid"       => "#1A9E4A",
        "Reimbursed" => "#2563EB",
        _            => "#D97706"
    };
    public string StatusBg => Status switch
    {
        "Paid"       => "#E8F9EE",
        "Reimbursed" => "#E8F0FF",
        _            => "#FFF4E5"
    };
}
