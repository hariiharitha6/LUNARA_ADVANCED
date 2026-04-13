package com.example.lunara;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderVH> {

    public interface OnReminderActionListener {
        void onDelete(int position);
        void onToggle(int position, boolean enabled);
    }

    private final List<ReminderModel> reminders;
    private final OnReminderActionListener listener;

    public ReminderAdapter(List<ReminderModel> reminders, OnReminderActionListener listener) {
        this.reminders = reminders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder_card, parent, false);
        return new ReminderVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderVH holder, int position) {
        ReminderModel rem = reminders.get(position);

        holder.title.setText(rem.title);

        SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Date d = new Date(rem.triggerTime);

        holder.date.setText("📅 " + dateFmt.format(d));
        holder.time.setText("⏰ " + timeFmt.format(d));

        if (rem.isSilent) {
            holder.type.setText("🔕 Silent");
            holder.type.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
        } else {
            holder.type.setText("🔔 Alarm + Sound");
            holder.type.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary));
        }

        // Avoid triggering listener during bind
        holder.toggle.setOnCheckedChangeListener(null);
        holder.toggle.setChecked(rem.enabled);
        holder.toggle.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) {
                listener.onToggle(holder.getAdapterPosition(), isChecked);
            }
        });

        // Grey out if disabled
        float alpha = rem.enabled ? 1.0f : 0.45f;
        holder.title.setAlpha(alpha);
        holder.date.setAlpha(alpha);
        holder.time.setAlpha(alpha);
        holder.type.setAlpha(alpha);

        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    static class ReminderVH extends RecyclerView.ViewHolder {
        TextView title, date, time, type;
        Switch toggle;
        Button deleteBtn;

        ReminderVH(View v) {
            super(v);
            title = v.findViewById(R.id.reminderItemTitle);
            date = v.findViewById(R.id.reminderItemDate);
            time = v.findViewById(R.id.reminderItemTime);
            type = v.findViewById(R.id.reminderItemType);
            toggle = v.findViewById(R.id.reminderItemSwitch);
            deleteBtn = v.findViewById(R.id.reminderItemDeleteBtn);
        }
    }
}
