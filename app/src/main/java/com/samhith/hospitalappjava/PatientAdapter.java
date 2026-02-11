package com.samhith.hospitalappjava;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class PatientAdapter extends ArrayAdapter<Patient> {

    // ViewHolder pattern for better performance
    private static class ViewHolder {
        TextView nameTextView;
        TextView ageTextView;
        TextView phoneTextView;
    }

    public PatientAdapter(@NonNull Context context, @NonNull List<Patient> patients) {
        super(context, 0, patients);
    }


    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the patient item for this position
        Patient patient = getItem(position);
        if(patient == null){
            return new View(getContext());
        }

        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_patient, parent, false);

            // Lookup view for data population
            viewHolder.nameTextView = convertView.findViewById(R.id.patientName);
            viewHolder.ageTextView = convertView.findViewById(R.id.patientAge);
            viewHolder.phoneTextView = convertView.findViewById(R.id.patientPhone);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Populate the data into the template view using the data object
        if (patient != null) {
            viewHolder.nameTextView.setText(patient.getName());
            viewHolder.ageTextView.setText(String.format("Age: %d", patient.getAge()));
            viewHolder.phoneTextView.setText(String.format("Phone: %s", patient.getPhone()));
        }

        // Return the completed view to render on screen
        return convertView;
    }
}