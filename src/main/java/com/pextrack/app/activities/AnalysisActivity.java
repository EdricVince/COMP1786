package com.pextrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.pextrack.app.R;
import com.pextrack.app.database.ExpenseDAO;
import com.pextrack.app.database.ProjectDAO;
import com.pextrack.app.models.Expense;
import com.pextrack.app.models.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analysis screen — visualises spending statistics from local SQLite data.
 */
public class AnalysisActivity extends AppCompatActivity {

    private ProjectDAO projectDAO;
    private ExpenseDAO expenseDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        projectDAO = new ProjectDAO(this);
        expenseDAO = new ExpenseDAO(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupBottomNav();
        loadStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_analysis);
    }

    // ── Bottom nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_analysis);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_analysis) {
                return true;
            } else if (id == R.id.nav_upload) {
                startActivity(new Intent(this, UploadActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    // ── Load & compute stats ──────────────────────────────────────────────────

    private void loadStats() {
        List<Project> projects    = projectDAO.getAllProjects();
        List<Expense> allExpenses = expenseDAO.getAllExpenses();

        // ── Financial overview ────────────────────────────────────────────────
        double totalBudget = 0, totalSpent = 0;
        int active = 0, completed = 0, onHold = 0;

        for (Project p : projects) {
            totalBudget += p.getBudget();
            totalSpent  += expenseDAO.getTotalSpentByProject(p.getId());
            switch (p.getStatus()) {
                case "Active":    active++;    break;
                case "Completed": completed++; break;
                default:          onHold++;    break;
            }
        }
        double remaining    = totalBudget - totalSpent;
        int    utilization  = totalBudget > 0 ? (int) Math.min((totalSpent / totalBudget) * 100, 100) : 0;

        setText(R.id.tvAnalysisTotalBudget,  String.format("$%,.2f", totalBudget));
        setText(R.id.tvAnalysisTotalSpent,   String.format("$%,.2f", totalSpent));
        setText(R.id.tvAnalysisRemaining,    String.format("$%,.2f", remaining));
        setText(R.id.tvAnalysisUtilization,  utilization + "%");

        // ── Project status bars ───────────────────────────────────────────────
        int total = projects.size();
        setText(R.id.tvActiveCount,    String.valueOf(active));
        setText(R.id.tvCompletedCount, String.valueOf(completed));
        setText(R.id.tvOnHoldCount,    String.valueOf(onHold));

        setBar(R.id.barActive,    active,    total);
        setBar(R.id.barCompleted, completed, total);
        setBar(R.id.barOnHold,    onHold,    total);

        // ── Expenses by type ──────────────────────────────────────────────────
        Map<String, Double> byType = new HashMap<>();
        int paid = 0, pending = 0, reimbursed = 0;

        for (Expense e : allExpenses) {
            String type = e.getExpenseType() != null ? e.getExpenseType() : "Other";
            byType.put(type, byType.getOrDefault(type, 0.0) + e.getAmount());

            if ("Paid".equalsIgnoreCase(e.getPaymentStatus())) paid++;
            else if ("Pending".equalsIgnoreCase(e.getPaymentStatus())) pending++;
            else reimbursed++;
        }

        setText(R.id.tvTotalExpenseCount, allExpenses.size() + " total");
        setText(R.id.tvPaidCount,         String.valueOf(paid));
        setText(R.id.tvPendingCount,      String.valueOf(pending));
        setText(R.id.tvReimbursedCount,   String.valueOf(reimbursed));

        // Build type rows
        LinearLayout layoutTypes = findViewById(R.id.layoutExpenseTypes);
        layoutTypes.removeAllViews();
        final double maxTypeAmount = byType.isEmpty() ? 1 :
            byType.values().stream().mapToDouble(d -> d).max().orElse(1);
        final double finalTotalSpent = totalSpent;

        // Sort by amount descending
        byType.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(entry -> {
                View row = buildTypeRow(entry.getKey(), entry.getValue(), maxTypeAmount, finalTotalSpent);
                layoutTypes.addView(row);
            });

        if (byType.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No expenses recorded yet");
            empty.setTextColor(getColor(R.color.text_hint));
            layoutTypes.addView(empty);
        }

        // ── Top projects by spending ──────────────────────────────────────────
        LinearLayout layoutTop = findViewById(R.id.layoutTopProjects);
        layoutTop.removeAllViews();

        projects.stream()
            .sorted((a, b) -> Double.compare(
                expenseDAO.getTotalSpentByProject(b.getId()),
                expenseDAO.getTotalSpentByProject(a.getId())))
            .limit(5)
            .forEach(p -> {
                double spent = expenseDAO.getTotalSpentByProject(p.getId());
                View row = buildProjectRow(p, spent);
                layoutTop.addView(row);
            });

        if (projects.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No projects yet");
            empty.setTextColor(getColor(R.color.text_hint));
            layoutTop.addView(empty);
        }
    }

    // ── View builders ─────────────────────────────────────────────────────────

    private View buildTypeRow(String label, double amount, double maxAmount, double totalSpent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                          ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        row.setLayoutParams(lp);

        // Label + amount row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(getColor(R.color.text_primary));
        tvLabel.setTextSize(13);
        LinearLayout.LayoutParams labelLp =
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(labelLp);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(String.format("$%,.0f", amount));
        tvAmt.setTextColor(getColor(R.color.text_secondary));
        tvAmt.setTextSize(13);

        int pct = totalSpent > 0 ? (int) ((amount / totalSpent) * 100) : 0;
        TextView tvPct = new TextView(this);
        tvPct.setText("  " + pct + "%");
        tvPct.setTextColor(getColor(R.color.primary));
        tvPct.setTextSize(12);

        header.addView(tvLabel);
        header.addView(tvAmt);
        header.addView(tvPct);

        // Progress bar
        android.widget.FrameLayout track = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams trackLp =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        trackLp.topMargin = dp(4);
        track.setLayoutParams(trackLp);
        track.setBackgroundResource(R.drawable.bg_progress_track);

        View fill = new View(this);
        fill.setBackgroundResource(R.drawable.bg_progress_fill);
        android.widget.FrameLayout.LayoutParams fillLp =
            new android.widget.FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        fill.setLayoutParams(fillLp);
        track.addView(fill);

        // Animate bar after layout
        int barPct = maxAmount > 0 ? (int) ((amount / maxAmount) * 100) : 0;
        track.post(() -> {
            int w = track.getWidth();
            android.widget.FrameLayout.LayoutParams flp =
                (android.widget.FrameLayout.LayoutParams) fill.getLayoutParams();
            flp.width = w * barPct / 100;
            fill.setLayoutParams(flp);
        });

        row.addView(header);
        row.addView(track);
        return row;
    }

    private View buildProjectRow(Project p, double spent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                          ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        row.setLayoutParams(lp);

        TextView tvName = new TextView(this);
        tvName.setText(p.getProjectName());
        tvName.setTextColor(getColor(R.color.text_primary));
        tvName.setTextSize(14);
        tvName.setMaxLines(1);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameLp =
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameLp);

        TextView tvSpent = new TextView(this);
        tvSpent.setText(String.format("$%,.0f", spent));
        tvSpent.setTextColor(getColor(R.color.accent));
        tvSpent.setTextSize(14);
        tvSpent.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        row.addView(tvName);
        row.addView(tvSpent);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void setBar(int barId, int count, int total) {
        View bar = findViewById(barId);
        if (bar == null || total == 0) return;
        bar.post(() -> {
            int parentWidth = ((View) bar.getParent()).getWidth();
            ViewGroup.LayoutParams lp = bar.getLayoutParams();
            lp.width = parentWidth * count / total;
            bar.setLayoutParams(lp);
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
