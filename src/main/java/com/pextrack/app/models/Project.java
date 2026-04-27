package com.pextrack.app.models;

import java.io.Serializable;

/**
 * Data model representing a project in the expense tracker.
 * Implements Serializable for passing between Activities via Intents.
 */
public class Project implements Serializable {

    private int    id;
    private String projectCode;
    private String projectName;
    private String description;
    private String startDate;
    private String endDate;
    private String manager;
    private String status;           // "Active" | "Completed" | "On Hold"
    private double budget;
    private String specialRequirements;
    private String clientInfo;
    private String createdAt;
    private boolean isUploaded;      // true once successfully uploaded to cloud

    // ── Constructors ──────────────────────────────────────────────────────────

    public Project() {}

    public Project(String projectCode, String projectName, String description,
                   String startDate, String endDate, String manager,
                   String status, double budget,
                   String specialRequirements, String clientInfo) {
        this.projectCode         = projectCode;
        this.projectName         = projectName;
        this.description         = description;
        this.startDate           = startDate;
        this.endDate             = endDate;
        this.manager             = manager;
        this.status              = status;
        this.budget              = budget;
        this.specialRequirements = specialRequirements;
        this.clientInfo          = clientInfo;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String v) { this.projectCode = v; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String v) { this.startDate = v; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String v) { this.endDate = v; }

    public String getManager() { return manager; }
    public void setManager(String v) { this.manager = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public double getBudget() { return budget; }
    public void setBudget(double v) { this.budget = v; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String v) { this.specialRequirements = v; }

    public String getClientInfo() { return clientInfo; }
    public void setClientInfo(String v) { this.clientInfo = v; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }

    public boolean isUploaded() { return isUploaded; }
    public void setUploaded(boolean v) { this.isUploaded = v; }

    @Override
    public String toString() {
        return projectName + " (" + projectCode + ")";
    }
}
