package com.pextrack.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.pextrack.app.R;
import com.pextrack.app.database.ProjectDAO;
import com.pextrack.app.databinding.ActivityAddEditProjectBinding;
import com.pextrack.app.models.Project;
import com.pextrack.app.network.FirebaseService;
import com.pextrack.app.network.NetworkUtils;
import com.pextrack.app.utils.Constants;
import com.pextrack.app.utils.DateUtils;
import com.pextrack.app.utils.ValidationUtils;

import java.util.Calendar;
import java.util.UUID;

/**
 * Add or edit a project.
 * Feature (a) — enter and validate project details.
 */
public class AddEditProjectActivity extends AppCompatActivity {

    private ActivityAddEditProjectBinding binding;
    private ProjectDAO                    projectDAO;
    private Project                       editProject;   // non-null when editing
    private boolean                       isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityAddEditProjectBinding.inflate(getLayoutInflater());
        projectDAO = new ProjectDAO(this);
        setContentView(binding.getRoot());

        isEditMode = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false);
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Budget" : "New Budget");
        }

        setupStatusDropdown();
        setupDatePickers();
        setupSaveButton();
        setupCodeField();

        if (isEditMode) {
            editProject = (Project) getIntent().getSerializableExtra(Constants.EXTRA_PROJECT);
            if (editProject != null) populateFields(editProject);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ── UI Setup ──────────────────────────────────────────────────────────────

    private void setupCodeField() {
        if (!isEditMode) {
            // Hide field — ID will be auto-generated on save
            binding.tilCode.setVisibility(android.view.View.GONE);
        } else {
            // Show as read-only when editing
            binding.tilCode.getEditText().setEnabled(false);
            binding.tilCode.setHint("Budget ID (auto-generated)");
        }
    }

    private String generateBudgetCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "BDG-" + uuid.substring(0, 6);
    }

    private void setupStatusDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, Constants.PROJECT_STATUSES);
        ((AutoCompleteTextView) binding.tilStatus.getEditText()).setAdapter(adapter);
        // Default selection
        ((AutoCompleteTextView) binding.tilStatus.getEditText()).setText(
            Constants.PROJECT_STATUSES[0], false);
    }

    private void setupDatePickers() {
        binding.tilStartDate.getEditText().setFocusable(false);
        binding.tilEndDate.getEditText().setFocusable(false);

        binding.tilStartDate.getEditText().setOnClickListener(v ->
            showDatePicker(binding.tilStartDate.getEditText())
        );
        binding.tilEndDate.getEditText().setOnClickListener(v ->
            showDatePicker(binding.tilEndDate.getEditText())
        );
    }

    private void showDatePicker(android.widget.EditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this,
            (view, year, month, day) -> {
                String date = String.format("%02d/%02d/%d", day, month + 1, year);
                target.setText(date);
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> {
            if (validateForm()) saveProject();
        });
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateForm() {
        String required = getString(R.string.err_required);
        boolean valid = true;

        if (isEditMode) valid &= ValidationUtils.notEmpty(binding.tilCode, required);
        valid &= ValidationUtils.notEmpty(binding.tilName,        required);
        // Description is now optional
        valid &= ValidationUtils.notEmpty(binding.tilStartDate,   required);
        valid &= ValidationUtils.notEmpty(binding.tilEndDate,     required);
        valid &= ValidationUtils.notEmpty(binding.tilManager,     required);
        valid &= ValidationUtils.notEmpty(binding.tilStatus,      required);
        valid &= ValidationUtils.isPositiveNumber(binding.tilBudget,
                    getString(R.string.err_invalid_budget));

        if (valid) {
            // Check end date is after start date
            String start = binding.tilStartDate.getEditText().getText().toString();
            String end   = binding.tilEndDate.getEditText().getText().toString();
            if (!DateUtils.isEndAfterStart(start, end)) {
                binding.tilEndDate.setError(getString(R.string.err_end_before_start));
                valid = false;
            }
        }
        return valid;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveProject() {
        Project p = isEditMode && editProject != null ? editProject : new Project();

        p.setProjectCode(   isEditMode ? getText(binding.tilCode) : generateBudgetCode());
        p.setProjectName(   getText(binding.tilName));
        p.setDescription(   getText(binding.tilDescription));
        p.setStartDate(     getText(binding.tilStartDate));
        p.setEndDate(       getText(binding.tilEndDate));
        p.setManager(       getText(binding.tilManager));
        p.setStatus(        getText(binding.tilStatus));
        p.setBudget(        Double.parseDouble(getText(binding.tilBudget)));
        p.setSpecialRequirements(getText(binding.tilSpecialReq));
        p.setClientInfo(    getText(binding.tilClientInfo));

        if (!isEditMode) {
            p.setCreatedAt(DateUtils.now());
            long id = projectDAO.insertProject(p);
            if (id > 0) {
                p.setId((int) id);
                if (NetworkUtils.isConnected(this)) new FirebaseService().saveProject(p);
                Snackbar.make(binding.getRoot(),
                    getString(R.string.msg_project_saved),
                    Snackbar.LENGTH_SHORT).show();
                finish();
            }
        } else {
            int rows = projectDAO.updateProject(p);
            if (rows > 0) {
                if (NetworkUtils.isConnected(this)) new FirebaseService().saveProject(p);
                Snackbar.make(binding.getRoot(),
                    getString(R.string.msg_project_saved),
                    Snackbar.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void populateFields(Project p) {
        binding.tilCode.getEditText().setText(p.getProjectCode());
        binding.tilName.getEditText().setText(p.getProjectName());
        binding.tilDescription.getEditText().setText(p.getDescription());
        binding.tilStartDate.getEditText().setText(p.getStartDate());
        binding.tilEndDate.getEditText().setText(p.getEndDate());
        binding.tilManager.getEditText().setText(p.getManager());
        ((AutoCompleteTextView) binding.tilStatus.getEditText()).setText(p.getStatus(), false);
        binding.tilBudget.getEditText().setText(String.valueOf(p.getBudget()));
        if (p.getSpecialRequirements() != null)
            binding.tilSpecialReq.getEditText().setText(p.getSpecialRequirements());
        if (p.getClientInfo() != null)
            binding.tilClientInfo.getEditText().setText(p.getClientInfo());
    }

    private String getText(com.google.android.material.textfield.TextInputLayout til) {
        return til.getEditText() != null
               ? til.getEditText().getText().toString().trim()
               : "";
    }
}
