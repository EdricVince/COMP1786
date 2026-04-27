package com.pextrack.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pextrack.app.models.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Project CRUD operations.
 */
public class ProjectDAO {

    private final DatabaseHelper dbHelper;

    public ProjectDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    // ── INSERT ────────────────────────────────────────────────────────────────

    /**
     * Inserts a new project. Returns the row ID, or -1 on failure.
     */
    public long insertProject(Project p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv  = buildContentValues(p);
        long id = db.insert(DatabaseHelper.TABLE_PROJECTS, null, cv);
        return id;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Updates an existing project by its ID. Returns number of rows affected.
     */
    public int updateProject(Project p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv  = buildContentValues(p);
        return db.update(
            DatabaseHelper.TABLE_PROJECTS,
            cv,
            DatabaseHelper.COL_P_ID + " = ?",
            new String[]{ String.valueOf(p.getId()) }
        );
    }

    /**
     * Marks a project as uploaded to the cloud.
     */
    public void markUploaded(int projectId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put(DatabaseHelper.COL_P_IS_UPLOADED, 1);
        db.update(
            DatabaseHelper.TABLE_PROJECTS, cv,
            DatabaseHelper.COL_P_ID + " = ?",
            new String[]{ String.valueOf(projectId) }
        );
    }

    /**
     * Updates only the status of a project.
     */
    public int updateProjectStatus(int id, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put(DatabaseHelper.COL_P_STATUS, status);
        return db.update(
            DatabaseHelper.TABLE_PROJECTS,
            cv,
            DatabaseHelper.COL_P_ID + " = ?",
            new String[]{ String.valueOf(id) }
        );
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Deletes a project and all its expenses (cascade via FK).
     */
    public int deleteProject(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
            DatabaseHelper.TABLE_PROJECTS,
            DatabaseHelper.COL_P_ID + " = ?",
            new String[]{ String.valueOf(id) }
        );
    }

    /**
     * Deletes ALL projects and expenses (reset database).
     */
    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_EXPENSES, null, null);
        db.delete(DatabaseHelper.TABLE_PROJECTS, null, null);
    }

    // ── QUERY ─────────────────────────────────────────────────────────────────

    /** Returns all projects, newest first. */
    public List<Project> getAllProjects() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Project> list = new ArrayList<>();
        Cursor c = db.query(
            DatabaseHelper.TABLE_PROJECTS,
            null, null, null, null, null,
            DatabaseHelper.COL_P_ID + " DESC"
        );
        if (c.moveToFirst()) {
            do { list.add(cursorToProject(c)); } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    /** Returns a single project by ID, or null if not found. */
    public Project getProjectById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(
            DatabaseHelper.TABLE_PROJECTS,
            null,
            DatabaseHelper.COL_P_ID + " = ?",
            new String[]{ String.valueOf(id) },
            null, null, null
        );
        Project p = null;
        if (c.moveToFirst()) p = cursorToProject(c);
        c.close();
        return p;
    }

    /**
     * Full-text search: matches name or description (case-insensitive).
     * Advanced: also filter by status or manager if provided (null = ignore).
     */
    public List<Project> searchProjects(String query, String status, String manager, String date) {
        SQLiteDatabase db   = dbHelper.getReadableDatabase();
        List<Project>  list = new ArrayList<>();

        StringBuilder where = new StringBuilder();
        List<String>  args  = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            where.append("(")
                 .append(DatabaseHelper.COL_P_NAME).append(" LIKE ? OR ")
                 .append(DatabaseHelper.COL_P_DESCRIPTION).append(" LIKE ?")
                 .append(")");
            String like = "%" + query.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isEmpty() && !status.equals("All")) {
            if (where.length() > 0) where.append(" AND ");
            where.append(DatabaseHelper.COL_P_STATUS).append(" = ?");
            args.add(status);
        }
        if (manager != null && !manager.trim().isEmpty()) {
            if (where.length() > 0) where.append(" AND ");
            where.append(DatabaseHelper.COL_P_MANAGER).append(" LIKE ?");
            args.add("%" + manager.trim() + "%");
        }
        if (date != null && !date.trim().isEmpty()) {
            if (where.length() > 0) where.append(" AND ");
            where.append("(").append(DatabaseHelper.COL_P_START_DATE).append(" LIKE ? OR ")
                 .append(DatabaseHelper.COL_P_END_DATE).append(" LIKE ?)");
            args.add("%" + date.trim() + "%");
            args.add("%" + date.trim() + "%");
        }

        Cursor c = db.query(
            DatabaseHelper.TABLE_PROJECTS,
            null,
            where.length() > 0 ? where.toString() : null,
            args.isEmpty() ? null : args.toArray(new String[0]),
            null, null,
            DatabaseHelper.COL_P_NAME + " ASC"
        );
        if (c.moveToFirst()) {
            do { list.add(cursorToProject(c)); } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    /** Returns total number of projects. */
    public int getProjectCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_PROJECTS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ContentValues buildContentValues(Project p) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_P_CODE,        p.getProjectCode());
        cv.put(DatabaseHelper.COL_P_NAME,        p.getProjectName());
        cv.put(DatabaseHelper.COL_P_DESCRIPTION, p.getDescription());
        cv.put(DatabaseHelper.COL_P_START_DATE,  p.getStartDate());
        cv.put(DatabaseHelper.COL_P_END_DATE,    p.getEndDate());
        cv.put(DatabaseHelper.COL_P_MANAGER,     p.getManager());
        cv.put(DatabaseHelper.COL_P_STATUS,      p.getStatus());
        cv.put(DatabaseHelper.COL_P_BUDGET,      p.getBudget());
        cv.put(DatabaseHelper.COL_P_SPECIAL_REQ, p.getSpecialRequirements());
        cv.put(DatabaseHelper.COL_P_CLIENT_INFO, p.getClientInfo());
        cv.put(DatabaseHelper.COL_P_CREATED_AT,  p.getCreatedAt());
        cv.put(DatabaseHelper.COL_P_IS_UPLOADED, p.isUploaded() ? 1 : 0);
        return cv;
    }

    private Project cursorToProject(Cursor c) {
        Project p = new Project();
        p.setId(             c.getInt(   c.getColumnIndexOrThrow(DatabaseHelper.COL_P_ID)));
        p.setProjectCode(    c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_CODE)));
        p.setProjectName(    c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_NAME)));
        p.setDescription(    c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_DESCRIPTION)));
        p.setStartDate(      c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_START_DATE)));
        p.setEndDate(        c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_END_DATE)));
        p.setManager(        c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_MANAGER)));
        p.setStatus(         c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_STATUS)));
        p.setBudget(         c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_BUDGET)));
        p.setSpecialRequirements(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_SPECIAL_REQ)));
        p.setClientInfo(     c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_CLIENT_INFO)));
        p.setCreatedAt(      c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_P_CREATED_AT)));
        p.setUploaded(       c.getInt(   c.getColumnIndexOrThrow(DatabaseHelper.COL_P_IS_UPLOADED)) == 1);
        return p;
    }
}
