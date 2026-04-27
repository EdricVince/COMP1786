package com.pextrack.app.utils;

import com.google.android.material.textfield.TextInputLayout;

/**
 * Input validation helpers for form fields.
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    /**
     * Checks that a TextInputLayout is non-empty.
     * Shows an error and returns false if empty.
     */
    public static boolean notEmpty(TextInputLayout layout, String errorMessage) {
        String text = layout.getEditText() != null
                      ? layout.getEditText().getText().toString().trim()
                      : "";
        if (text.isEmpty()) {
            layout.setError(errorMessage);
            return false;
        }
        layout.setError(null);
        return true;
    }

    /**
     * Validates that the value in a TextInputLayout is a positive number.
     */
    public static boolean isPositiveNumber(TextInputLayout layout, String errorMessage) {
        if (layout.getEditText() == null) return false;
        String text = layout.getEditText().getText().toString().trim();
        try {
            double v = Double.parseDouble(text);
            if (v <= 0) throw new NumberFormatException();
            layout.setError(null);
            return true;
        } catch (NumberFormatException e) {
            layout.setError(errorMessage);
            return false;
        }
    }

    /** Clears the error on a TextInputLayout. */
    public static void clearError(TextInputLayout layout) {
        layout.setError(null);
    }
}
