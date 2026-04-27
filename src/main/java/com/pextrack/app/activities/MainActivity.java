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
import com.pextrack.app.adapters.ProjectAdapter;
import com.pextrack.app.database.ExpenseDAO;
import com.pextrack.app.database.ProjectDAO;
import com.pextrack.app.databinding.ActivityMainBinding;
import com.pextrack.app.models.Project;
import com.pextrack.app.utils.Constants;

import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements ProjectAdapter.OnProjectClickListener {

    private ActivityMainBinding binding;
    private ProjectDAO           projectDAO;
    private ExpenseDAO           expenseDAO;
    private ProjectAdapter       adapter;
    private List<Project>        projectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityMainBinding.inflate(getLayoutInflater());
        projectDAO = new ProjectDAO(this);
        expenseDAO = new ExpenseDAO(this);
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Overview");
        }

        setupRecyclerView();
        setupFab();
        setupActionButtons();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
        if (adapter.isSelectionMode()) {
            exitSelectionMode();
        }
    }

    private void setupRecyclerView() {
        binding.recyclerProjects.setLayoutManager(new LinearLayoutManager(this));
        projectList = projectDAO.getAllProjects();
        adapter     = new ProjectAdapter(this, projectList, this);
        adapter.setSpentProvider(pid -> expenseDAO.getTotalSpentByProject(pid));
        binding.recyclerProjects.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAddProject.setOnClickListener(v -> {
            Intent i = new Intent(this, AddEditProjectActivity.class);
            i.putExtra(Constants.EXTRA_EDIT_MODE, false);
            startActivity(i);
        });
    }

    private void setupActionButtons() {
        // Change Reset to "Select to Delete" or just "Delete" when in selection mode
        binding.btnResetDb.setText("Delete Multiple");
        binding.btnResetDb.setTextColor(getColor(R.color.primary));
        
        binding.btnResetDb.setOnClickListener(v -> {
            if (!adapter.isSelectionMode()) {
                adapter.setSelectionMode(true);
                binding.btnResetDb.setText("Cancel");
                binding.btnResetDb.setTextColor(getColor(R.color.text_secondary));
            } else {
                exitSelectionMode();
            }
        });
    }

    private void exitSelectionMode() {
        adapter.setSelectionMode(false);
        binding.btnResetDb.setText("Delete Multiple");
        binding.btnResetDb.setTextColor(getColor(R.color.primary));
        binding.fabAddProject.setImageResource(android.R.drawable.ic_input_add);
        binding.fabAddProject.setOnClickListener(v -> {
            Intent i = new Intent(this, AddEditProjectActivity.class);
            i.putExtra(Constants.EXTRA_EDIT_MODE, false);
            startActivity(i);
        });
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            }
            if (id == R.id.nav_analysis) {
                startActivity(new Intent(this, AnalysisActivity.class));
                return true;
            }
            if (id == R.id.nav_upload) {
                startActivity(new Intent(this, UploadActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadProjects() {
        projectList = projectDAO.getAllProjects();
        adapter.updateList(projectList);

        boolean empty = projectList.isEmpty();
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerProjects.setVisibility(empty ? View.GONE : View.VISIBLE);

        double totalBudget = 0, totalSpent = 0;
        int active = 0, completed = 0, onHold = 0;

        for (Project p : projectList) {
            totalBudget += p.getBudget();
            totalSpent  += expenseDAO.getTotalSpentByProject(p.getId());
            switch (p.getStatus()) {
                case "Active":    active++;    break;
                case "Completed": completed++; break;
                default:          onHold++;    break;
            }
        }

        double remaining = totalBudget - totalSpent;
        binding.tvTotalBudget.setText(String.format("$%,.2f", totalBudget));
        binding.tvTotalSpent.setText(String.format("$%,.2f", totalSpent));
        binding.tvTotalRemaining.setText(String.format("$%,.2f", remaining));
        binding.tvProjectCount.setText(String.valueOf(projectList.size()));
        binding.tvActiveCount.setText(String.valueOf(active));
        binding.tvCompletedCount.setText(String.valueOf(completed));
        binding.tvOnHoldCount.setText(String.valueOf(onHold));
    }

    @Override
    public void onProjectClick(Project project) {
        Intent i = new Intent(this, ProjectDetailActivity.class);
        i.putExtra(Constants.EXTRA_PROJECT_ID, project.getId());
        startActivity(i);
    }

    @Override
    public void onProjectLongClick(Project project) {
        if (!adapter.isSelectionMode()) {
            adapter.setSelectionMode(true);
            binding.btnResetDb.setText("Cancel");
            binding.btnResetDb.setTextColor(getColor(R.color.text_secondary));
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count > 0) {
            binding.btnResetDb.setText("Delete (" + count + ")");
            binding.btnResetDb.setTextColor(getColor(R.color.error));
            
            // Re-bind FAB to Delete action
            binding.fabAddProject.setImageResource(android.R.drawable.ic_menu_delete);
            binding.fabAddProject.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Delete Selected")
                    .setMessage("Are you sure you want to delete " + count + " selected budgets?")
                    .setPositiveButton("Delete", (d, w) -> {
                        Set<Integer> ids = adapter.getSelectedProjectIds();
                        for (Integer id : ids) {
                            projectDAO.deleteProject(id);
                        }
                        exitSelectionMode();
                        loadProjects();
                        Snackbar.make(binding.getRoot(), count + " budgets deleted", Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        } else {
            binding.btnResetDb.setText("Cancel");
            binding.btnResetDb.setTextColor(getColor(R.color.text_secondary));
            binding.fabAddProject.setImageResource(android.R.drawable.ic_input_add);
            setupFab(); // Restore original FAB behavior
        }
    }
}
