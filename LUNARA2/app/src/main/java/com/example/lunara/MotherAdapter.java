package com.example.lunara;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MotherAdapter extends RecyclerView.Adapter<MotherAdapter.MotherViewHolder> {

    private final List<MotherModel> motherList;

    public MotherAdapter(List<MotherModel> motherList) {
        this.motherList = motherList;
    }

    @NonNull
    @Override
    public MotherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mother_card, parent, false);
        return new MotherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MotherViewHolder holder, int position) {
        MotherModel mother = motherList.get(position);

        holder.nameText.setText("👩‍⚕️ " + mother.name);
        holder.areaText.setText("📍 " + mother.area);
        holder.mobileText.setText("📱 " + mother.mobile);
        holder.trimesterText.setText("🤰 " + mother.trimester);

        // Risk badge — color-coded
        int score = mother.riskScore;
        if (score >= 60) {
            holder.riskBadge.setText("🔴 HIGH RISK (" + score + "%)");
            holder.riskBadge.setTextColor(Color.parseColor("#DC2626"));
            holder.riskBadge.setBackgroundColor(Color.parseColor("#FEE2E2"));
        } else if (score >= 30) {
            holder.riskBadge.setText("🟠 MODERATE (" + score + "%)");
            holder.riskBadge.setTextColor(Color.parseColor("#92400E"));
            holder.riskBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
        } else {
            holder.riskBadge.setText("🟢 LOW RISK (" + score + "%)");
            holder.riskBadge.setTextColor(Color.parseColor("#166534"));
            holder.riskBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
        }
    }

    @Override
    public int getItemCount() {
        return motherList.size();
    }

    static class MotherViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, areaText, mobileText, trimesterText, riskBadge;

        MotherViewHolder(View itemView) {
            super(itemView);
            nameText     = itemView.findViewById(R.id.motherName);
            areaText     = itemView.findViewById(R.id.motherArea);
            mobileText   = itemView.findViewById(R.id.motherMobile);
            trimesterText = itemView.findViewById(R.id.motherTrimester);
            riskBadge    = itemView.findViewById(R.id.riskBadge);
        }
    }
}
