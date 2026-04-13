package com.example.lunara;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorVH> {

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    private final List<DoctorModel> doctors;
    private final OnDeleteListener deleteListener;

    public DoctorAdapter(List<DoctorModel> doctors, OnDeleteListener deleteListener) {
        this.doctors = doctors;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public DoctorVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_card, parent, false);
        return new DoctorVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorVH holder, int position) {
        DoctorModel doc = doctors.get(position);
        holder.nameText.setText(doc.name);
        holder.phoneText.setText(doc.phone);

        holder.callBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + doc.phone));
                v.getContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Cannot make call", Toast.LENGTH_SHORT).show();
            }
        });

        holder.whatsappBtn.setOnClickListener(v -> {
            try {
                String url = "https://wa.me/91" + doc.phone;
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "WhatsApp not available", Toast.LENGTH_SHORT).show();
            }
        });

        holder.deleteBtn.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    static class DoctorVH extends RecyclerView.ViewHolder {
        TextView nameText, phoneText;
        Button callBtn, whatsappBtn, deleteBtn;

        DoctorVH(View v) {
            super(v);
            nameText = v.findViewById(R.id.doctorItemName);
            phoneText = v.findViewById(R.id.doctorItemPhone);
            callBtn = v.findViewById(R.id.doctorItemCallBtn);
            whatsappBtn = v.findViewById(R.id.doctorItemWhatsappBtn);
            deleteBtn = v.findViewById(R.id.doctorItemDeleteBtn);
        }
    }
}
