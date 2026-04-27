package com.pextrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.pextrack.app.R;
import com.pextrack.app.adapters.ExpenseAdapter;
import com.pextrack.app.database.ExpenseDAO;
import com.pextrack.app.database.ProjectDAO;
import com.pextrack.app.databinding.ActivityProjectDetailBinding;
import com.pextrack.app.models.Expense;
import com.pextrack.app.models.Project;
import com.pextrack.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectDetailActivity extends AppCompatActivity
        implements ExpenseAdapter.OnExpenseClickListener {

    private ActivityProjectDetailBinding binding;
    private ProjectDAO                   projectDAO;
    private ExpenseDAO                   expenseDAO;
    private Project                      project;
    private ExpenseAdapter               adapter;
    private List<Object>                 groupedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityProjectDetailBinding.inflate(getLayoutInflater());
        projectDAO = new ProjectDAO(this);
        expenseDAO = new ExpenseDAO(this);
        setContentView(binding.getRoot());

        int projectId = getIntent().getIntExtra(Constants.EXTRA_PROJECT_ID, -1);
        if (projectId == -1) { finish(); return; }
        project = projectDAO.getProjectById(projectId);
        if (project == null) { finish(); return; }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(project.getProjectName());
        }

        setupProjectInfo();
        setupExpenseRecyclerView();
        setupFab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProject();
        loadExpenses();
    }

    private void refreshProject() {
        if (project != null) {
            project = projectDAO.getProjectById(project.getId());
            setupProjectInfo();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { finish(); return true; }
        if (id == R.id.action_edit) {
            Intent i = new Intent(this, AddEditProjectActivity.class);
            i.putExtra(Constants.EXTRA_EDIT_MODE, true);
            i.putExtra(Constants.EXTRA_PROJECT, project);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupProjectInfo() {
        binding.tvDetailCode.setText(project.getProjectCode());
        binding.tvDetailName.setText(project.getProjectName());
        binding.tvDetailDescription.setText(project.getDescription());
        binding.tvDetailDates.setText(project.getStartDate() + "  →  " + project.getEndDate());
        binding.tvDetailManager.setText(project.getManager());
        binding.tvDetailBudget.setText(String.format("$%,.2f", project.getBudget()));

        // Status chip colour
        binding.tvDetailStatus.setText(project.getStatus());
        switch (project.getStatus()) {
            case "Active":
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_chip_active);
                binding.tvDetailStatus.setTextColor(getColor(R.color.status_active_text));
                break;
            case "Completed":
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_chip_completed);
                binding.tvDetailStatus.setTextColor(getColor(R.color.status_completed_text));
                break;
            default:
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_chip_on_hold);
                binding.tvDetailStatus.setTextColor(getColor(R.color.status_on_hold_text));
        }

        if (project.getSpecialRequirements() != null && !project.getSpecialRequirements().isEmpty()) {
            binding.tvDetailSpecialReq.setText(project.getSpecialRequirements());
            binding.rowSpecialReq.setVisibility(View.VISIBLE);
        }
        if (project.getClientInfo() != null && !project.getClientInfo().isEmpty()) {
            binding.tvDetailClientInfo.setText(project.getClientInfo());
            binding.rowClientInfo.setVisibility(View.VISIBLE);
        }
    }

    private void setupExpenseRecyclerView() {
        binding.recyclerExpenses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter(this, groupedItems, this);
        binding.recyclerExpenses.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAddExpense.setOnClickListener(v -> {
            Intent i = new Intent(this, AddEditExpenseActivity.class);
            i.putExtra(Constants.EXTRA_PROJECT_ID, project.getId());
            i.putExtra(Constants.EXTRA_EDIT_MODE, false);
            startActivity(i);
        });
    }

    private void loadExpenses() {
        Map<String, List<Expense>> grouped = expenseDAO.getExpensesByProjectGroupedByDate(project.getId());
        groupedItems.clear();

        for (Map.Entry<String, List<Expense>> entry : grouped.entrySet()) {
            groupedItems.add(entry.getKey()); // The Date string as header
            groupedItems.addAll(entry.getValue()); // All expenses for that date
        }

        adapter.updateItems(groupedItems);
        boolean empty = groupedItems.isEmpty();
        binding.tvEmptyExpenses.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerExpenses.setVisibility(empty ? View.GONE : View.VISIBLE);
        updateBudgetSummary();
    }

    private void updateBudgetSummary() {
        double budget    = project.getBudget();
        double spent     = expenseDAO.getTotalSpentByProject(project.getId());
        double remaining = budget - spent;
        int    pct       = budget > 0 ? (int) Math.min((spent / budget) * 100, 100) : 0;

        binding.tvDetailSpent.setText(String.format("Spent: $%,.2f", spent));
        binding.tvDetailRemaining.setText(String.format("Remaining: $%,.2f", remaining));
        binding.tvDetailPercent.setText(pct + "%");
        
        // Count only Expense objects, not headers
        long count = groupedItems.stream().filter(o -> o instanceof Expense).count();
        binding.tvExpenseCount.setText(count + " expense(s)");

        // Animate progress bar
        binding.viewBudgetProgress.post(() -> {
            int parentWidth = ((View) binding.viewBudgetProgress.getParent()).getWidth();
            ViewGroup.LayoutParams lp = binding.viewBudgetProgress.getLayoutParams();
            lp.width = (int) (parentWidth * pct / 100.0);
            binding.viewBudgetProgress.setLayoutParams(lp);
        });
    }

    // ── ExpenseAdapter callbacks ──────────────────────────────────────────────

    @Override
    public void onExpenseClick(Expense expense) {
        Intent i = new Intent(this, AddEditExpenseActivity.class);
        i.putExtra(Constants.EXTRA_EXPENSE, expense);
        i.putExtra(Constants.EXTRA_PROJECT_ID, project.getId());
        i.putExtra(Constants.EXTRA_EDIT_MODE, true);
        startActivity(i);
    }

    @Override
    public void onExpenseLongClick(Expense expense) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage(getString(R.string.msg_confirm_delete_expense))
            .setPositiveButton("Delete", (d, w) -> {
                expenseDAO.deleteExpense(expense.getId());
                checkAndUpdateProjectStatus();
                loadExpenses();
                refreshProject();
                Snackbar.make(binding.getRoot(),
                    getString(R.string.msg_expense_deleted),
                    Snackbar.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Auto-update status when an expense is deleted.
     */
    private void checkAndUpdateProjectStatus() {
        double totalSpent = expenseDAO.getTotalSpentByProject(project.getId());
        if (totalSpent < project.getBudget()) {
            if ("Completed".equals(project.getStatus())) {
                projectDAO.updateProjectStatus(project.getId(), "Active");
            }
        }
    }
}
