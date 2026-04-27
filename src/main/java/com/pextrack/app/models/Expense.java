package com.pextrack.app.models;

import java.io.Serializable;

/**
 * Data model for an expense entry linked to a project.
 */
public class Expense implements Serializable {

    private int    id;
    private String expenseCode;
    private int    projectId;
    private String expenseDate;
    private double amount;
    private String currency;
    private String expenseType;     // Travel | Equipment | Materials | Services | ...
    private String paymentMethod;   // Cash | Credit Card | Bank Transfer | Cheque
    private String claimant;
    private String paymentStatus;   // Paid | Pending | Reimbursed
    private String description;
    private String location;
    private String createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Expense() {}

    public Expense(String expenseCode, int projectId, String expenseDate,
                   double amount, String currency, String expenseType,
                   String paymentMethod, String claimant, String paymentStatus,
                   String description, String location) {
        this.expenseCode   = expenseCode;
        this.projectId     = projectId;
        this.expenseDate   = expenseDate;
        this.amount        = amount;
        this.currency      = currency;
        this.expenseType   = expenseType;
        this.paymentMethod = paymentMethod;
        this.claimant      = claimant;
        this.paymentStatus = paymentStatus;
        this.description   = description;
        this.location      = location;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getExpenseCode() { return expenseCode; }
    public void setExpenseCode(String v) { this.expenseCode = v; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int v) { this.projectId = v; }

    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String v) { this.expenseDate = v; }

    public double getAmount() { return amount; }
    public void setAmount(double v) { this.amount = v; }

    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }

    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String v) { this.expenseType = v; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }

    public String getClaimant() { return claimant; }
    public void setClaimant(String v) { this.claimant = v; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { this.paymentStatus = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
