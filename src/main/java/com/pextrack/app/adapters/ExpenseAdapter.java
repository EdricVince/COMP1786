package com.pextrack.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pextrack.app.R;
import com.pextrack.app.models.Expense;

import java.util.ArrayList;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
        void onExpenseLongClick(Expense expense);
    }

    private final Context                 context;
    private       List<Object>            items = new ArrayList<>();
    private final OnExpenseClickListener  listener;

    public ExpenseAdapter(Context ctx, List<Object> items, OnExpenseClickListener l) {
        this.context  = ctx;
        this.items    = items;
        this.listener = l;
    }

    public void updateItems(List<Object> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
            return new ItemViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) item);
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder h = (ItemViewHolder) holder;
            Expense e = (Expense) item;

            h.tvIcon.setText(typeToEmoji(e.getExpenseType()));
            h.tvType.setText(e.getExpenseType());
            h.tvClaimant.setText(e.getClaimant());
            h.tvDate.setText(e.getExpenseDate());
            h.tvPaymentMethod.setText(e.getPaymentMethod());
            h.tvAmount.setText(String.format("%s %,.2f", e.getCurrency(), e.getAmount()));
            h.tvStatus.setText(e.getPaymentStatus());
            h.tvCode.setText(e.getExpenseCode());

            switch (e.getPaymentStatus()) {
                case "Paid":
                    h.tvStatus.setBackgroundResource(R.drawable.bg_status_chip_paid);
                    h.tvStatus.setTextColor(context.getColor(R.color.paid_text));
                    break;
                case "Reimbursed":
                    h.tvStatus.setBackgroundResource(R.drawable.bg_status_chip_reimbursed);
                    h.tvStatus.setTextColor(context.getColor(R.color.reimbursed_text));
                    break;
                default:
                    h.tvStatus.setBackgroundResource(R.drawable.bg_status_chip_pending);
                    h.tvStatus.setTextColor(context.getColor(R.color.pending_text));
                    break;
            }

            h.itemView.setOnClickListener(v -> listener.onExpenseClick(e));
            h.itemView.setOnLongClickListener(v -> { listener.onExpenseLongClick(e); return true; });
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String typeToEmoji(String type) {
        if (type == null) return "💳";
        switch (type) {
            case "Travel":            return "✈️";
            case "Equipment":         return "🖥️";
            case "Materials":         return "📦";
            case "Services":          return "🔧";
            case "Software/Licenses": return "💻";
            case "Labour Costs":      return "👷";
            case "Utilities":         return "💡";
            default:                  return "💳";
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(View v) {
            super(v);
            tvHeader = v.findViewById(R.id.tvHeaderDate);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvType, tvClaimant, tvDate, tvPaymentMethod, tvAmount, tvStatus, tvCode;
        ItemViewHolder(View v) {
            super(v);
            tvIcon          = v.findViewById(R.id.tvExpenseIcon);
            tvType          = v.findViewById(R.id.tvExpenseType);
            tvClaimant      = v.findViewById(R.id.tvExpenseClaimant);
            tvDate          = v.findViewById(R.id.tvExpenseDate);
            tvPaymentMethod = v.findViewById(R.id.tvExpensePaymentMethod);
            tvAmount        = v.findViewById(R.id.tvExpenseAmount);
            tvStatus        = v.findViewById(R.id.tvExpenseStatus);
            tvCode          = v.findViewById(R.id.tvExpenseCode);
        }
    }
}
