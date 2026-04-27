package com.pextrack.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pextrack.app.R;
import com.pextrack.app.models.Project;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
        void onProjectLongClick(Project project);
        void onSelectionChanged(int count);
    }

    private final Context                context;
    private       List<Project>          projects;
    private final OnProjectClickListener listener;
    private       boolean                isSelectionMode = false;
    private final Set<Integer>           selectedProjectIds = new HashSet<>();

    public interface SpentProvider {
        double getSpent(int projectId);
    }
    private SpentProvider spentProvider;

    public void setSpentProvider(SpentProvider p) { this.spentProvider = p; }

    public ProjectAdapter(Context ctx, List<Project> projects, OnProjectClickListener listener) {
        this.context  = ctx;
        this.projects = projects;
        this.listener = listener;
    }

    public void updateList(List<Project> newList) {
        this.projects = newList;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean active) {
        this.isSelectionMode = active;
        if (!active) selectedProjectIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() { return isSelectionMode; }
    public Set<Integer> getSelectedProjectIds() { return selectedProjectIds; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Project p = projects.get(position);

        h.tvCode.setText(p.getProjectCode());
        h.tvName.setText(p.getProjectName());
        h.tvManager.setText("👤  " + p.getManager());
        h.tvDates.setText("📅  " + p.getStartDate() + "  →  " + p.getEndDate());
        h.tvUploaded.setVisibility(p.isUploaded() ? View.VISIBLE : View.GONE);

        // Status chip
        h.tvStatus.setText(p.getStatus());
        switch (p.getStatus()) {
            case "Active":
                h.tvStatus.setBackgroundResource(R.drawable.bg_status_chip_active);
                h.tvStatus.setTextColor(context.getColor(R.color.status_active_text));
                break;
            case "Completed":
                h.tvStatus.setBackgroundResource(R.drawable.bg_status_chip_completed);
                h.tvStatus.setTextColor(context.getColor(R.color.status_completed_text));
                break;
            default:
                h.tvStatus.setBackgroundResource(R.drawable.bg_status_chip_on_hold);
                h.tvStatus.setTextColor(context.getColor(R.color.status_on_hold_text));
                break;
        }

        // Selection logic
        h.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        h.checkBox.setChecked(selectedProjectIds.contains(p.getId()));
        h.checkBox.setOnClickListener(v -> {
            if (selectedProjectIds.contains(p.getId())) {
                selectedProjectIds.remove(p.getId());
            } else {
                selectedProjectIds.add(p.getId());
            }
            listener.onSelectionChanged(selectedProjectIds.size());
        });

        // Budget + progress bar
        double budget = p.getBudget();
        double spent  = spentProvider != null ? spentProvider.getSpent(p.getId()) : 0;
        int pct = budget > 0 ? (int) Math.min((spent / budget) * 100, 100) : 0;

        h.tvBudget.setText(String.format("$%,.0f  budget", budget));
        h.tvPercent.setText(pct + "% used");

        h.viewProgress.post(() -> {
            int parentWidth = ((View) h.viewProgress.getParent()).getWidth();
            ViewGroup.LayoutParams lp = h.viewProgress.getLayoutParams();
            lp.width = (int) (parentWidth * pct / 100.0);
            h.viewProgress.setLayoutParams(lp);
        });

        h.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                h.checkBox.performClick();
            } else {
                listener.onProjectClick(p);
            }
        });
        h.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
                selectedProjectIds.add(p.getId());
                listener.onSelectionChanged(selectedProjectIds.size());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() { return projects.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvName, tvManager, tvDates, tvBudget, tvPercent, tvStatus, tvUploaded;
        View     viewProgress;
        CheckBox checkBox;
        ViewHolder(View v) {
            super(v);
            tvCode        = v.findViewById(R.id.tvProjectCode);
            tvName        = v.findViewById(R.id.tvProjectName);
            tvManager     = v.findViewById(R.id.tvProjectManager);
            tvDates       = v.findViewById(R.id.tvProjectDates);
            tvBudget      = v.findViewById(R.id.tvProjectBudget);
            tvPercent     = v.findViewById(R.id.tvProjectPercent);
            tvStatus      = v.findViewById(R.id.tvProjectStatus);
            tvUploaded    = v.findViewById(R.id.tvProjectUploaded);
            viewProgress  = v.findViewById(R.id.viewProgress);
            checkBox      = v.findViewById(R.id.cbProjectDelete);
        }
    }
}
