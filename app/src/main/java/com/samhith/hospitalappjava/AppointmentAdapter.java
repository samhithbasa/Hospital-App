package com.samhith.hospitalappjava;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {
    private List<Appointment> appointmentList;
    private Context context;
    private OnAppointmentClickListener listener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public AppointmentAdapter(List<Appointment> appointmentList, Context context, OnAppointmentClickListener listener) {
        this.appointmentList = appointmentList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPatientName, tvDoctorName, tvDate, tvTime, tvPurpose, tvStatus;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        public void bind(Appointment appointment) {
            tvPatientName.setText("Patient: " + appointment.getPatientName());
            tvDoctorName.setText("Doctor: " + appointment.getDoctorName());
            tvDate.setText("Date: " + appointment.getDate());
            tvTime.setText("Time: " + appointment.getTime());
            tvPurpose.setText("Purpose: " + appointment.getPurpose());

            // Build status text
            String statusText = "Status: " + appointment.getStatus();
            if (("completed".equalsIgnoreCase(appointment.getStatus()) ||
                    "canceled".equalsIgnoreCase(appointment.getStatus())) &&
                    appointment.getStatusUpdateTime() != null &&
                    !appointment.getStatusUpdateTime().isEmpty()) {
                statusText += " (" + appointment.getStatusUpdateTime() + ")";
            } else if ("scheduled".equalsIgnoreCase(appointment.getStatus())) {
                statusText += " (" + appointment.getTime() + ")";
            }
            tvStatus.setText(statusText);

            // Set color based on status
            if ("completed".equalsIgnoreCase(appointment.getStatus())) {
                tvStatus.setTextColor(context.getResources().getColor(R.color.green));
            } else if ("canceled".equalsIgnoreCase(appointment.getStatus())) {
                tvStatus.setTextColor(context.getResources().getColor(R.color.red));
            } else if ("scheduled".equalsIgnoreCase(appointment.getStatus())) {
                tvStatus.setTextColor(context.getResources().getColor(R.color.orange));
            } else {
                tvStatus.setTextColor(context.getResources().getColor(android.R.color.black));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppointmentClick(appointment);
                }
            });
        }
    }
}