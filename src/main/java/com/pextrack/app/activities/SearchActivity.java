package com.pextrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pextrack.app.R;
import com.pextrack.app.adapters.ProjectAdapter;
import com.pextrack.app.database.ExpenseDAO;
import com.pextrack.app.database.ProjectDAO;
import com.pextrack.app.databinding.ActivitySearchBinding;
import com.pextrack.app.models.Project;
import com.pextrack.app.utils.Constants;

import java.util.List;

/**
 * Search projects by name, description, status, owner, or date.
 * Feature (d) — search.
 */
public class SearchActivity extends AppCompatActivity
        implements ProjectAdapter.OnProjectClickListener {

    private ActivitySearchBinding binding;
    private ProjectDAO             projectDAO;
    private ExpenseDAO             expenseDAO;
    private ProjectAdapter         adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivitySearchBinding.inflate(getLayoutInflater());
        projectDAO = new ProjectDAO(this);
        expenseDAO = new ExpenseDAO(this);
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Budgets");
        }

        setupRecyclerView();
        setupStatusFilter();
        setupSearchButton();
        // Show all projects by default
        performSearch();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectAdapter(this, List.of(), this);
        adapter.setSpentProvider(pid -> expenseDAO.getTotalSpentByProject(pid));
        binding.recyclerSearchResults.setAdapter(adapter);
    }

    private void setupStatusFilter() {
        ArrayAdapter<String> a = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, Constants.STATUS_FILTER);
        ((AutoCompleteTextView) binding.tilStatusFilter.getEditText()).setAdapter(a);
        ((AutoCompleteTextView) binding.tilStatusFilter.getEditText()).setText("All", false);
    }

    private void setupSearchButton() {
        binding.btnSearch.setOnClickListener(v -> performSearch());
        // Also trigger on Enter in the search field
        binding.etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    private void performSearch() {
        String query   = binding.etSearchQuery.getText().toString().trim();
        String status  = binding.tilStatusFilter.getEditText() != null
                         ? binding.tilStatusFilter.getEditText().getText().toString() : "All";
        String manager = binding.etFilterManager.getText().toString().trim();
        String date    = binding.etFilterDate.getText().toString().trim();

        List<Project> results = projectDAO.searchProjects(query, status, manager, date);
        adapter.updateList(results);

        binding.tvResultCount.setText(results.size() + " result(s) found");
        binding.recyclerSearchResults.setVisibility(
            results.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.tvEmptySearch.setVisibility(
            results.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @Override
    public void onProjectClick(Project project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PROJECT_ID, project.getId());
        startActivity(intent);
    }

    @Override
    public void onProjectLongClick(Project project) { /* no-op in search */ }

    @Override
    public void onSelectionChanged(int count) { /* no-op in search */ }
}
