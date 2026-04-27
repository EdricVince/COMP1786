package com.pextrack.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite database helper.
 * Manages creation and version upgrades of the local database.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME    = "expense_tracker.db";
    public static final int    DATABASE_VERSION = 1;

    // ── Table: projects ───────────────────────────────────────────────────────
    public static final String TABLE_PROJECTS          = "projects";
    public static final String COL_P_ID               = "id";
    public static final String COL_P_CODE             = "project_code";
    public static final String COL_P_NAME             = "project_name";
    public static final String COL_P_DESCRIPTION      = "description";
    public static final String COL_P_START_DATE       = "start_date";
    public static final String COL_P_END_DATE         = "end_date";
    public static final String COL_P_MANAGER          = "manager";
    public static final String COL_P_STATUS           = "status";
    public static final String COL_P_BUDGET           = "budget";
    public static final String COL_P_SPECIAL_REQ      = "special_requirements";
    public static final String COL_P_CLIENT_INFO      = "client_info";
    public static final String COL_P_CREATED_AT       = "created_at";
    public static final String COL_P_IS_UPLOADED      = "is_uploaded";

    // ── Table: expenses ───────────────────────────────────────────────────────
    public static final String TABLE_EXPENSES         = "expenses";
    public static final String COL_E_ID               = "id";
    public static final String COL_E_CODE             = "expense_code";
    public static final String COL_E_PROJECT_ID       = "project_id";
    public static final String COL_E_DATE             = "expense_date";
    public static final String COL_E_AMOUNT           = "amount";
    public static final String COL_E_CURRENCY         = "currency";
    public static final String COL_E_TYPE             = "expense_type";
    public static final String COL_E_PAYMENT_METHOD   = "payment_method";
    public static final String COL_E_CLAIMANT         = "claimant";
    public static final String COL_E_PAYMENT_STATUS   = "payment_status";
    public static final String COL_E_DESCRIPTION      = "description";
    public static final String COL_E_LOCATION         = "location";
    public static final String COL_E_CREATED_AT       = "created_at";

    private static final String CREATE_TABLE_PROJECTS =
        "CREATE TABLE " + TABLE_PROJECTS + " (" +
        COL_P_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_P_CODE        + " TEXT NOT NULL, " +
        COL_P_NAME        + " TEXT NOT NULL, " +
        COL_P_DESCRIPTION + " TEXT NOT NULL, " +
        COL_P_START_DATE  + " TEXT NOT NULL, " +
        COL_P_END_DATE    + " TEXT NOT NULL, " +
        COL_P_MANAGER     + " TEXT NOT NULL, " +
        COL_P_STATUS      + " TEXT NOT NULL, " +
        COL_P_BUDGET      + " REAL NOT NULL, " +
        COL_P_SPECIAL_REQ + " TEXT, " +
        COL_P_CLIENT_INFO + " TEXT, " +
        COL_P_CREATED_AT  + " TEXT, " +
        COL_P_IS_UPLOADED + " INTEGER DEFAULT 0)";

    private static final String CREATE_TABLE_EXPENSES =
        "CREATE TABLE " + TABLE_EXPENSES + " (" +
        COL_E_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_E_CODE           + " TEXT NOT NULL, " +
        COL_E_PROJECT_ID     + " INTEGER NOT NULL, " +
        COL_E_DATE           + " TEXT NOT NULL, " +
        COL_E_AMOUNT         + " REAL NOT NULL, " +
        COL_E_CURRENCY       + " TEXT NOT NULL, " +
        COL_E_TYPE           + " TEXT NOT NULL, " +
        COL_E_PAYMENT_METHOD + " TEXT NOT NULL, " +
        COL_E_CLAIMANT       + " TEXT NOT NULL, " +
        COL_E_PAYMENT_STATUS + " TEXT NOT NULL, " +
        COL_E_DESCRIPTION    + " TEXT, " +
        COL_E_LOCATION       + " TEXT, " +
        COL_E_CREATED_AT     + " TEXT, " +
        "FOREIGN KEY(" + COL_E_PROJECT_ID + ") REFERENCES " +
        TABLE_PROJECTS + "(" + COL_P_ID + ") ON DELETE CASCADE)";

    // Singleton instance
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON");
        db.execSQL(CREATE_TABLE_PROJECTS);
        db.execSQL(CREATE_TABLE_EXPENSES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For future migrations: drop and recreate (acceptable for coursework)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROJECTS);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys = ON");
    }
}
