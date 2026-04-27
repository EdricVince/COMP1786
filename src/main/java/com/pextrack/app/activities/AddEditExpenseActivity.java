package com.pextrack.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.pextrack.app.R;
import com.pextrack.app.database.ExpenseDAO;
import com.pextrack.app.database.ProjectDAO;
import com.pextrack.app.databinding.ActivityAddEditExpenseBinding;
import com.pextrack.app.models.Expense;
import com.pextrack.app.models.Project;
import com.pextrack.app.network.FirebaseService;
import com.pextrack.app.network.NetworkUtils;
import com.pextrack.app.utils.Constants;
import com.pextrack.app.utils.DateUtils;
import com.pextrack.app.utils.ValidationUtils;

import java.util.Calendar;
import java.util.UUID;

/**
 * Add or edit an expense linked to a project.
 */
public class AddEditExpenseActivity extends AppCompatActivity {

    private ActivityAddEditExpenseBinding binding;
    private ExpenseDAO                    expenseDAO;
    private ProjectDAO                    projectDAO;
    private Expense                       editExpense;
    private boolean                       isEditMode;
    private int                           projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityAddEditExpenseBinding.inflate(getLayoutInflater());
        expenseDAO = new ExpenseDAO(this);
        projectDAO = new ProjectDAO(this);
        setContentView(binding.getRoot());

        isEditMode = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false);
        projectId  = getIntent().getIntExtra(Constants.EXTRA_PROJECT_ID, -1);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Expense" : "New Expense");
        }

        setupDropdowns();
        setupDatePicker();
        setupSaveButton();
        setupCodeField();

        if (isEditMode) {
            editExpense = (Expense) getIntent().getSerializableExtra(Constants.EXTRA_EXPENSE);
            if (editExpense != null) populateFields(editExpense);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void setupCodeField() {
        if (!isEditMode) {
            binding.tilExpenseCode.setVisibility(android.view.View.GONE);
        } else {
            binding.tilExpenseCode.getEditText().setEnabled(false);
        }
    }

    private String generateExpenseCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "EXP-" + uuid.substring(0, 6);
    }

    private void setupDropdowns() {
        setDropdown(binding.tilExpenseType,     Constants.EXPENSE_TYPES,     0);
        setDropdown(binding.tilCurrency,        Constants.CURRENCIES,        0);
        setDropdown(binding.tilPaymentMethod,   Constants.PAYMENT_METHODS,   0);
        setDropdown(binding.tilPaymentStatus,   Constants.PAYMENT_STATUSES,  0);
    }

    private void setDropdown(com.google.android.material.textfield.TextInputLayout til,
                             String[] options, int defaultIndex) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, options);
        AutoCompleteTextView acv = (AutoCompleteTextView) til.getEditText();
        if (acv != null) {
            acv.setAdapter(adapter);
            acv.setText(options[defaultIndex], false);
        }
    }

    private void setupDatePicker() {
        binding.tilExpenseDate.getEditText().setFocusable(false);
        binding.tilExpenseDate.getEditText().setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = String.format("%02d/%02d/%d", day, month + 1, year);
                    binding.tilExpenseDate.getEditText().setText(date);
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> {
            if (validateForm()) saveExpense();
        });
    }

    private boolean validateForm() {
        String req = getString(R.string.err_required);
        boolean valid = true;
        if (isEditMode) valid &= ValidationUtils.notEmpty(binding.tilExpenseCode, req);
        valid &= ValidationUtils.notEmpty(binding.tilExpenseDate,   req);
        valid &= ValidationUtils.isPositiveNumber(binding.tilAmount, getString(R.string.err_invalid_amount));
        valid &= ValidationUtils.notEmpty(binding.tilClaimant,       req);
        return valid;
    }

    private void saveExpense() {
        Expense e = isEditMode && editExpense != null ? editExpense : new Expense();

        e.setExpenseCode(   isEditMode ? getText(binding.tilExpenseCode) : generateExpenseCode());
        e.setProjectId(     projectId);
        e.setExpenseDate(   getText(binding.tilExpenseDate));
        e.setAmount(        Double.parseDouble(getText(binding.tilAmount)));
        e.setCurrency(      getText(binding.tilCurrency));
        e.setExpenseType(   getText(binding.tilExpenseType));
        e.setPaymentMethod( getText(binding.tilPaymentMethod));
        e.setClaimant(      getText(binding.tilClaimant));
        e.setPaymentStatus( getText(binding.tilPaymentStatus));
        e.setDescription(   getText(binding.tilDescription));
        e.setLocation(      getText(binding.tilLocation));

        if (!isEditMode) {
            e.setCreatedAt(DateUtils.now());
            long id = expenseDAO.insertExpense(e);
            if (id > 0) {
                e.setId((int) id);
                if (NetworkUtils.isConnected(this)) new FirebaseService().saveExpense(e);
                checkAndUpdateProjectStatus();
                finish();
            }
        } else {
            int rows = expenseDAO.updateExpense(e);
            if (rows > 0) {
                if (NetworkUtils.isConnected(this)) new FirebaseService().saveExpense(e);
                checkAndUpdateProjectStatus();
                finish();
            }
        }
    }

    /**
     * Tự động chuyển trạng thái dự án sang "Completed" nếu chi tiêu đạt ngân sách.
     */
    private void checkAndUpdateProjectStatus() {
        Project p = projectDAO.getProjectById(projectId);
        if (p == null) return;

        double totalSpent = expenseDAO.getTotalSpentByProject(projectId);
        // Nếu đã tiêu hết tiền ngân sách, buộc trạng thái là Completed
        if (totalSpent >= p.getBudget()) {
            if (!"Completed".equals(p.getStatus())) {
                projectDAO.updateProjectStatus(projectId, "Completed");
            }
        } else if ("Completed".equals(p.getStatus())) {
            // Nếu ngân sách vẫn còn (sau khi sửa chi phí giảm xuống), đưa về Active
            projectDAO.updateProjectStatus(projectId, "Active");
        }
    }

    private void populateFields(Expense e) {
        binding.tilExpenseCode.getEditText().setText(e.getExpenseCode());
        binding.tilExpenseDate.getEditText().setText(e.getExpenseDate());
        binding.tilAmount.getEditText().setText(String.valueOf(e.getAmount()));
        ((AutoCompleteTextView)binding.tilCurrency.getEditText()).setText(e.getCurrency(), false);
        ((AutoCompleteTextView)binding.tilExpenseType.getEditText()).setText(e.getExpenseType(), false);
        ((AutoCompleteTextView)binding.tilPaymentMethod.getEditText()).setText(e.getPaymentMethod(), false);
        binding.tilClaimant.getEditText().setText(e.getClaimant());
        ((AutoCompleteTextView)binding.tilPaymentStatus.getEditText()).setText(e.getPaymentStatus(), false);
        if (e.getDescription() != null) binding.tilDescription.getEditText().setText(e.getDescription());
        if (e.getLocation() != null) binding.tilLocation.getEditText().setText(e.getLocation());
    }

    private String getText(com.google.android.material.textfield.TextInputLayout til) {
        return til.getEditText() != null ? til.getEditText().getText().toString().trim() : "";
    }
}
