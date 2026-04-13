package com.example.lunara;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CheckupAdapter extends RecyclerView.Adapter<CheckupAdapter.CheckupViewHolder> {

    private final List<CheckupEntry> entryList;

    public CheckupAdapter(List<CheckupEntry> entryList) {
        this.entryList = entryList;
    }

    @NonNull
    @Override
    public CheckupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_checkup_card, parent, false);
        return new CheckupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckupViewHolder h, int position) {
        CheckupEntry e = entryList.get(position);

        h.nameText.setText("👩‍⚕️ " + e.name);
        h.areaText.setText("📍 " + e.area);
        h.dateText.setText("📅 " + CheckupScheduler.formatDate(e.nextVisitDate));

        // Risk badge colors
        switch (e.riskLevel) {
            case CheckupScheduler.LEVEL_HIGH:
                h.riskBadge.setText("🔴 HIGH RISK (" + e.riskScore + "%)");
                h.riskBadge.setTextColor(Color.parseColor("#DC2626"));
                h.riskBadge.setBackgroundColor(Color.parseColor("#FEE2E2"));
                break;
            case CheckupScheduler.LEVEL_MODERATE:
                h.riskBadge.setText("🟠 MODERATE (" + e.riskScore + "%)");
                h.riskBadge.setTextColor(Color.parseColor("#92400E"));
                h.riskBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
                break;
            default:
                h.riskBadge.setText("🟢 LOW RISK (" + e.riskScore + "%)");
                h.riskBadge.setTextColor(Color.parseColor("#166534"));
                h.riskBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
        }

        // Countdown / overdue
        long days = e.daysRemaining;
        if (CheckupScheduler.STATUS_OVERDUE.equals(e.status) || days < 0) {
            h.countdownText.setText("⚠ OVERDUE by " + Math.abs(days) + " days");
            h.countdownText.setTextColor(Color.parseColor("#DC2626"));
            h.statusBadge.setText("OVERDUE");
            h.statusBadge.setTextColor(Color.WHITE);
            h.statusBadge.setBackgroundColor(Color.parseColor("#DC2626"));
            h.itemView.setBackgroundColor(Color.parseColor("#FFF5F5"));
        } else if (days <= 2) {
            h.countdownText.setText("⏰ In " + days + " day(s) — Visit Soon!");
            h.countdownText.setTextColor(Color.parseColor("#B45309"));
            h.statusBadge.setText("DUE SOON");
            h.statusBadge.setTextColor(Color.parseColor("#92400E"));
            h.statusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
            h.itemView.setBackgroundColor(Color.parseColor("#FFFBEB"));
        } else {
            h.countdownText.setText("🗓 In " + days + " days");
            h.countdownText.setTextColor(Color.parseColor("#0F766E"));
            h.statusBadge.setText("UPCOMING");
            h.statusBadge.setTextColor(Color.parseColor("#166534"));
            h.statusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
            h.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() { return entryList.size(); }

    static class CheckupViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, areaText, dateText, riskBadge, countdownText, statusBadge;

        CheckupViewHolder(View v) {
            super(v);
            nameText     = v.findViewById(R.id.checkupName);
            areaText     = v.findViewById(R.id.checkupArea);
            dateText     = v.findViewById(R.id.checkupDate);
            riskBadge    = v.findViewById(R.id.checkupRiskBadge);
            countdownText = v.findViewById(R.id.checkupCountdown);
            statusBadge  = v.findViewById(R.id.checkupStatusBadge);
        }
    }
}
