package com.pextrack.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pextrack.app.models.Expense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Expense CRUD operations.
 */
public class ExpenseDAO {

 private final DatabaseHelper dbHelper;

 public ExpenseDAO(Context context) {
 this.dbHelper = DatabaseHelper.getInstance(context);
 }

 // ── INSERT ────────────────────────────────────────────────────────────────

 public long insertExpense(Expense e) {
 SQLiteDatabase db = dbHelper.getWritableDatabase();
 return db.insert(DatabaseHelper.TABLE_EXPENSES, null, buildContentValues(e));
 }

 // ── UPDATE ────────────────────────────────────────────────────────────────

 public int updateExpense(Expense e) {
 SQLiteDatabase db = dbHelper.getWritableDatabase();
 return db.update(
 DatabaseHelper.TABLE_EXPENSES,
 buildContentValues(e),
 DatabaseHelper.COL_E_ID + " = ?",
 new String[]{ String.valueOf(e.getId()) }
 );
 }

 // ── DELETE ────────────────────────────────────────────────────────────────

 public int deleteExpense(int id) {
 SQLiteDatabase db = dbHelper.getWritableDatabase();
 return db.delete(
 DatabaseHelper.TABLE_EXPENSES,
 DatabaseHelper.COL_E_ID + " = ?",
 new String[]{ String.valueOf(id) }
 );
 }

 public int deleteExpensesByProject(int projectId) {
 SQLiteDatabase db = dbHelper.getWritableDatabase();
 return db.delete(
 DatabaseHelper.TABLE_EXPENSES,
 DatabaseHelper.COL_E_PROJECT_ID + " = ?",
 new String[]{ String.valueOf(projectId) }
 );
 }

 // ── QUERY ─────────────────────────────────────────────────────────────────

 /** Returns all expenses for a given project, newest first. */
 public List<Expense> getExpensesByProject(int projectId) {
 SQLiteDatabase db = dbHelper.getReadableDatabase();
 List<Expense> list = new ArrayList<>();
 Cursor c = db.query(
 DatabaseHelper.TABLE_EXPENSES,
 null,
 DatabaseHelper.COL_E_PROJECT_ID + " = ?",
 new String[]{ String.valueOf(projectId) },
 null, null,
 DatabaseHelper.COL_E_ID + " DESC"
 );
 if (c.moveToFirst()) {
 do { list.add(cursorToExpense(c)); } while (c.moveToNext());
 }
 c.close();
 return list;
 }

 /** Returns all expenses in the database. */
 public List<Expense> getAllExpenses() {
 SQLiteDatabase db = dbHelper.getReadableDatabase();
 List<Expense> list = new ArrayList<>();
 Cursor c = db.query(DatabaseHelper.TABLE_EXPENSES,
 null, null, null, null, null,
 DatabaseHelper.COL_E_ID + " DESC");
 if (c.moveToFirst()) {
 do { list.add(cursorToExpense(c)); } while (c.moveToNext());
 }
 c.close();
 return list;
 }

 /** Returns a single expense by ID, or null. */
 public Expense getExpenseById(int id) {
 SQLiteDatabase db = dbHelper.getReadableDatabase();
 Cursor c = db.query(
 DatabaseHelper.TABLE_EXPENSES,
 null,
 DatabaseHelper.COL_E_ID + " = ?",
 new String[]{ String.valueOf(id) },
 null, null, null
 );
 Expense e = null;
 if (c.moveToFirst()) e = cursorToExpense(c);
 c.close();
 return e;
 }

 /** Returns the sum of all expense amounts for a project. */
 public double getTotalSpentByProject(int projectId) {
 SQLiteDatabase db = dbHelper.getReadableDatabase();
 Cursor c = db.rawQuery(
 "SELECT SUM(" + DatabaseHelper.COL_E_AMOUNT + ") FROM " +
 DatabaseHelper.TABLE_EXPENSES + " WHERE " +
 DatabaseHelper.COL_E_PROJECT_ID + " = ?",
 new String[]{ String.valueOf(projectId) }
 );
 double total =0;
 if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
 c.close();
 return total;
 }

 /** Returns expenses for a project grouped by date. */
 public Map<String, List<Expense>> getExpensesByProjectGroupedByDate(int projectId) {
 List<Expense> expenses = getExpensesByProject(projectId);
 Map<String, List<Expense>> grouped = new LinkedHashMap<>();
 for (Expense e : expenses) {
 String date = e.getExpenseDate() != null ? e.getExpenseDate() : "Unknown";
 if (!grouped.containsKey(date)) {
 grouped.put(date, new ArrayList<>());
 }
 grouped.get(date).add(e);
 }
 return grouped;
 }

 /** Returns total spent by project for a specific date. */
 public double getTotalSpentByProjectAndDate(int projectId, String date) {
 SQLiteDatabase db = dbHelper.getReadableDatabase();
 Cursor c = db.rawQuery(
 "SELECT SUM(" + DatabaseHelper.COL_E_AMOUNT + ") FROM " +
 DatabaseHelper.TABLE_EXPENSES + " WHERE " +
 DatabaseHelper.COL_E_PROJECT_ID + " = ? AND " +
 DatabaseHelper.COL_E_DATE + " = ?",
 new String[]{ String.valueOf(projectId), date }
 );
 double total =0;
 if (c.moveToFirst() && !c.isNull(0)) total = c.getDouble(0);
 c.close();
 return total;
 }

 /** Returns all expenses grouped by date across all projects. */
 public Map<String, List<Expense>> getAllExpensesGroupedByDate() {
 List<Expense> expenses = getAllExpenses();
 Map<String, List<Expense>> grouped = new LinkedHashMap<>();
 // Sort by date descending
 Collections.sort(expenses, (a, b) -> {
 String da = a.getExpenseDate() != null ? a.getExpenseDate() : "";
 String db = b.getExpenseDate() != null ? b.getExpenseDate() : "";
 return db.compareTo(da); // descending
 });
 for (Expense e : expenses) {
 String date = e.getExpenseDate() != null ? e.getExpenseDate() : "Unknown";
 if (!grouped.containsKey(date)) {
 grouped.put(date, new ArrayList<>());
 }
 grouped.get(date).add(e);
 }
 return grouped;
 }

 // ── Helpers ───────────────────────────────────────────────────────────────

 private ContentValues buildContentValues(Expense e) {
 ContentValues cv = new ContentValues();
 cv.put(DatabaseHelper.COL_E_CODE, e.getExpenseCode());
 cv.put(DatabaseHelper.COL_E_PROJECT_ID, e.getProjectId());
 cv.put(DatabaseHelper.COL_E_DATE, e.getExpenseDate());
 cv.put(DatabaseHelper.COL_E_AMOUNT, e.getAmount());
 cv.put(DatabaseHelper.COL_E_CURRENCY, e.getCurrency());
 cv.put(DatabaseHelper.COL_E_TYPE, e.getExpenseType());
 cv.put(DatabaseHelper.COL_E_PAYMENT_METHOD, e.getPaymentMethod());
 cv.put(DatabaseHelper.COL_E_CLAIMANT, e.getClaimant());
 cv.put(DatabaseHelper.COL_E_PAYMENT_STATUS, e.getPaymentStatus());
 cv.put(DatabaseHelper.COL_E_DESCRIPTION, e.getDescription());
 cv.put(DatabaseHelper.COL_E_LOCATION, e.getLocation());
 cv.put(DatabaseHelper.COL_E_CREATED_AT, e.getCreatedAt());
 return cv;
 }

 private Expense cursorToExpense(Cursor c) {
 Expense e = new Expense();
 e.setId( c.getInt( c.getColumnIndexOrThrow(DatabaseHelper.COL_E_ID)));
 e.setExpenseCode( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_CODE)));
 e.setProjectId( c.getInt( c.getColumnIndexOrThrow(DatabaseHelper.COL_E_PROJECT_ID)));
 e.setExpenseDate( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_DATE)));
 e.setAmount( c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_AMOUNT)));
 e.setCurrency( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_CURRENCY)));
 e.setExpenseType( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_TYPE)));
 e.setPaymentMethod( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_PAYMENT_METHOD)));
 e.setClaimant( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_CLAIMANT)));
 e.setPaymentStatus( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_PAYMENT_STATUS)));
 e.setDescription( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_DESCRIPTION)));
 e.setLocation( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_LOCATION)));
 e.setCreatedAt( c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_E_CREATED_AT)));
 return e;
 }
}
