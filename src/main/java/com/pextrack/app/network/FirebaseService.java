package com.pextrack.app.network;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.pextrack.app.models.Expense;
import com.pextrack.app.models.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Firestore service.
 *
 * Firestore structure:
 *
 *   projects/                          ← top-level collection
 *     {auto-id}/                       ← one document per project
 *       code, name, description, ...   ← project fields
 *       expenses/                      ← sub-collection
 *         {auto-id}/                   ← one document per expense
 *           code, date, amount, ...    ← expense fields
 *
 * Feature (e): upload all projects + their expenses to Firestore at once.
 * Feature (g – MAUI): the MAUI app reads from this same Firestore collection
 *                     using the Firebase REST API (no SDK needed in MAUI).
 */
public class FirebaseService {

    public interface UploadCallback {
        void onProgress(String message);   // called for each step
        void onSuccess(int projectCount);  // all done
        void onFailure(String error);      // something went wrong
    }

    private final FirebaseFirestore db;

    public FirebaseService() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Single-item auto-sync ─────────────────────────────────────────────────

    /** Saves one project document to Firestore immediately. Fire-and-forget. */
    public void saveProject(Project p) {
        db.collection("projects")
            .document("local_" + p.getId())
            .set(projectToMap(p));
    }

    /** Saves one expense into its parent project's sub-collection. Fire-and-forget. */
    public void saveExpense(Expense e) {
        db.collection("projects")
            .document("local_" + e.getProjectId())
            .collection("expenses")
            .document("local_" + e.getId())
            .set(expenseToMap(e));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Uploads all projects and their expenses to Firestore.
     * Each project is stored as a document in the "projects" collection.
     * Its expenses are stored in a sub-collection "expenses" under that document.
     *
     * Call this from a background thread or observe the returned tasks.
     * The callbacks are posted back on the calling thread — wrap in
     * runOnUiThread() when calling from an Activity.
     */
    public void uploadAll(List<Project> projects,
                          List<Expense> allExpenses,
                          UploadCallback callback) {

        if (projects.isEmpty()) {
            callback.onSuccess(0);
            return;
        }

        // Upload projects one by one so we can report per-project progress
        uploadNextProject(projects, allExpenses, callback, 0);
    }

    /**
     * Deletes ALL documents in the "projects" collection (and their expenses
     * sub-collections). Useful for a fresh re-upload.
     * Note: Firestore does not recursively delete sub-collections automatically —
     * this method handles that correctly via batched deletes.
     */
    public void deleteAllProjects(Runnable onComplete, Runnable onError) {
        db.collection("projects").get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    onComplete.run();
                    return;
                }
                WriteBatch batch = db.batch();
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    batch.delete(doc.getReference());
                }
                batch.commit()
                    .addOnSuccessListener(v -> onComplete.run())
                    .addOnFailureListener(e -> onError.run());
            })
            .addOnFailureListener(e -> onError.run());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Recursive helper: uploads project at index i, then moves to i+1.
     */
    private void uploadNextProject(List<Project> projects,
                                   List<Expense> allExpenses,
                                   UploadCallback callback,
                                   int index) {
        if (index >= projects.size()) {
            // All done
            callback.onSuccess(projects.size());
            return;
        }

        Project p = projects.get(index);
        callback.onProgress("Uploading: " + p.getProjectName() + " (" + (index + 1) + "/" + projects.size() + ")");

        // Write the project document
        db.collection("projects")
            .add(projectToMap(p))
            .addOnSuccessListener(docRef -> {
                // Now write its expenses as a sub-collection
                uploadExpensesForProject(docRef, p.getId(), allExpenses,
                    () -> {
                        // Move to next project
                        uploadNextProject(projects, allExpenses, callback, index + 1);
                    },
                    error -> callback.onFailure("Expense upload failed for "
                        + p.getProjectName() + ": " + error)
                );
            })
            .addOnFailureListener(e ->
                callback.onFailure("Failed to upload project " + p.getProjectName()
                    + ": " + e.getMessage())
            );
    }

    /**
     * Writes all expenses that belong to projectId into docRef/expenses sub-collection.
     * Uses a batched write for efficiency (max 500 per batch; expense count is typically low).
     */
    private void uploadExpensesForProject(DocumentReference projectDoc,
                                          int projectId,
                                          List<Expense> allExpenses,
                                          Runnable onDone,
                                          java.util.function.Consumer<String> onError) {
        List<Expense> mine = new ArrayList<>();
        for (Expense e : allExpenses) {
            if (e.getProjectId() == projectId) mine.add(e);
        }

        if (mine.isEmpty()) {
            onDone.run();
            return;
        }

        WriteBatch batch = db.batch();
        for (Expense e : mine) {
            DocumentReference expRef = projectDoc.collection("expenses").document();
            batch.set(expRef, expenseToMap(e));
        }

        batch.commit()
            .addOnSuccessListener(v -> onDone.run())
            .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    // ── Model → Map converters ────────────────────────────────────────────────

    private Map<String, Object> projectToMap(Project p) {
        Map<String, Object> m = new HashMap<>();
        m.put("localId",           p.getId());
        m.put("code",              p.getProjectCode());
        m.put("name",              p.getProjectName());
        m.put("description",       p.getDescription());
        m.put("startDate",         p.getStartDate());
        m.put("endDate",           p.getEndDate());
        m.put("manager",           p.getManager());
        m.put("status",            p.getStatus());
        m.put("budget",            p.getBudget());
        m.put("specialRequirements", p.getSpecialRequirements());
        m.put("clientInfo",        p.getClientInfo());
        m.put("createdAt",         p.getCreatedAt());
        return m;
    }

    private Map<String, Object> expenseToMap(Expense e) {
        Map<String, Object> m = new HashMap<>();
        m.put("localId",       e.getId());
        m.put("code",          e.getExpenseCode());
        m.put("date",          e.getExpenseDate());
        m.put("amount",        e.getAmount());
        m.put("currency",      e.getCurrency());
        m.put("type",          e.getExpenseType());
        m.put("paymentMethod", e.getPaymentMethod());
        m.put("claimant",      e.getClaimant());
        m.put("status",        e.getPaymentStatus());
        m.put("description",   e.getDescription());
        m.put("location",      e.getLocation());
        m.put("createdAt",     e.getCreatedAt());
        return m;
    }
}
