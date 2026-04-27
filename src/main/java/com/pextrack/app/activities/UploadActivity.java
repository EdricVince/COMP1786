package com.pextrack.app.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.pextrack.app.R;
import com.pextrack.app.database.ExpenseDAO;
import com.pextrack.app.database.ProjectDAO;
import com.pextrack.app.databinding.ActivityUploadBinding;
import com.pextrack.app.models.Expense;
import com.pextrack.app.models.Project;
import com.pextrack.app.network.FirebaseService;
import com.pextrack.app.network.NetworkUtils;
import com.pextrack.app.utils.DateUtils;

import java.util.List;

/**
 * Uploads all local project/expense data to Firebase Cloud Firestore.
 * Feature (e) – cloud upload.
 *
 * Firestore path used:
 *   projects/{auto-id}            ← project document
 *   projects/{auto-id}/expenses/  ← sub-collection of expenses
 *
 * The MAUI hybrid app (Feature g) reads from this same Firestore collection
 * via Firebase REST API: GET https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents/projects
 */
public class UploadActivity extends AppCompatActivity {

    private ActivityUploadBinding binding;
    private ProjectDAO            projectDAO;
    private ExpenseDAO            expenseDAO;
    private FirebaseService       firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding        = ActivityUploadBinding.inflate(getLayoutInflater());
        projectDAO     = new ProjectDAO(this);
        expenseDAO     = new ExpenseDAO(this);
        firebaseService = new FirebaseService();
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Upload to Firebase");
        }

        updateNetworkStatus();
        setupButtons();
        appendLog("Ready. Press 'Upload to Firebase' to begin.");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void updateNetworkStatus() {
        boolean connected = NetworkUtils.isConnected(this);
        binding.tvNetworkStatus.setText(connected ? "✓ Connected" : "✗ No Internet");
        binding.tvNetworkStatus.setTextColor(getColor(
            connected ? R.color.status_active_text : R.color.error));
    }

    private void setupButtons() {
        // Main upload button
        binding.btnUpload.setOnClickListener(v -> startUpload());

        // Delete-all button (re-upload fresh)
        binding.btnDeleteCloud.setOnClickListener(v -> deleteAll());
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    private void startUpload() {
        if (!NetworkUtils.isConnected(this)) {
            Snackbar.make(binding.getRoot(),
                getString(R.string.msg_no_network), Snackbar.LENGTH_SHORT).show();
            return;
        }

        List<Project> projects    = projectDAO.getAllProjects();
        List<Expense> allExpenses = expenseDAO.getAllExpenses();

        if (projects.isEmpty()) {
            Snackbar.make(binding.getRoot(),
                "No projects to upload.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        appendLog("─── Upload started: " + DateUtils.now() + " ───");
        appendLog("Projects found: " + projects.size());
        appendLog("Expenses found: " + allExpenses.size());

        firebaseService.uploadAll(projects, allExpenses, new FirebaseService.UploadCallback() {

            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> appendLog("  • " + message));
            }

            @Override
            public void onSuccess(int projectCount) {
                // Mark all projects as uploaded in local DB
                for (Project p : projects) projectDAO.markUploaded(p.getId());

                runOnUiThread(() -> {
                    setLoading(false);
                    appendLog("✓ Upload complete! " + projectCount + " project(s) synced.");
                    appendLog("─────────────────────────────────");
                    Snackbar.make(binding.getRoot(),
                        getString(R.string.msg_upload_success),
                        Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    appendLog("✗ Error: " + error);
                    Snackbar.make(binding.getRoot(),
                        getString(R.string.msg_upload_failed),
                        Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    // ── Delete all from Firestore ─────────────────────────────────────────────

    private void deleteAll() {
        if (!NetworkUtils.isConnected(this)) {
            Snackbar.make(binding.getRoot(),
                getString(R.string.msg_no_network), Snackbar.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        appendLog("Deleting all Firestore data…");

        firebaseService.deleteAllProjects(
            () -> runOnUiThread(() -> {
                setLoading(false);
                appendLog("✓ Firestore data cleared.");
                Snackbar.make(binding.getRoot(),
                    "Cloud data deleted.", Snackbar.LENGTH_SHORT).show();
            }),
            () -> runOnUiThread(() -> {
                setLoading(false);
                appendLog("✗ Delete failed.");
            })
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendLog(String message) {
        String current = binding.tvUploadLog.getText().toString();
        binding.tvUploadLog.setText(current + "\n" + message);
        binding.scrollLog.post(() ->
            binding.scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    private void setLoading(boolean loading) {
        binding.progressUpload.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnUpload.setEnabled(!loading);
        binding.btnDeleteCloud.setEnabled(!loading);
        binding.btnUpload.setText(loading ? "Uploading…" : "Upload to Firebase");
    }
}
