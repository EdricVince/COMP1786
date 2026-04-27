package com.pextrack.app.utils;

/**
 * App-wide constants: Intent keys, spinner arrays, preferences keys.
 */
public final class Constants {

    private Constants() {}

    // ── Intent extra keys ─────────────────────────────────────────────────────
    public static final String EXTRA_PROJECT    = "extra_project";
    public static final String EXTRA_PROJECT_ID = "extra_project_id";
    public static final String EXTRA_EXPENSE    = "extra_expense";
    public static final String EXTRA_EDIT_MODE  = "extra_edit_mode";

    // ── SharedPreferences ─────────────────────────────────────────────────────
    public static final String PREF_FILE        = "expense_tracker_prefs";
    public static final String PREF_API_URL     = "api_url";

    // ── Project status options ────────────────────────────────────────────────
    public static final String[] PROJECT_STATUSES = {
        "Active", "Completed", "On Hold"
    };

    // ── Expense type options ──────────────────────────────────────────────────
    public static final String[] EXPENSE_TYPES = {
        "Travel", "Food", "Shopping", "Equipment", "Materials", "Services",
        "Software/Licenses", "Labour Costs", "Utilities", "Miscellaneous"
    };

    // ── Payment method options ────────────────────────────────────────────────
    public static final String[] PAYMENT_METHODS = {
        "Cash", "Credit Card", "Bank Transfer", "Cheque"
    };

    // ── Payment status options ────────────────────────────────────────────────
    public static final String[] PAYMENT_STATUSES = {
        "Paid", "Pending", "Reimbursed"
    };

    // ── Currency options ──────────────────────────────────────────────────────
    public static final String[] CURRENCIES = {
        "USD", "EUR", "GBP", "VND", "JPY", "AUD", "CAD", "SGD", "THB", "CNY"
    };

    // ── Search filter options ─────────────────────────────────────────────────
    public static final String[] STATUS_FILTER = {
        "All", "Active", "Completed", "On Hold"
    };
}
