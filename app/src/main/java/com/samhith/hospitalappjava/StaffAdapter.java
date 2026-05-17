package com.samhith.hospitalappjava;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class StaffAdapter extends ArrayAdapter<Staff> {

    public StaffAdapter(Context context, List<Staff> staffList) {
        super(context, R.layout.list_item_staff, staffList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Staff staff = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_item_staff, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.staffImage);
        TextView nameTv = convertView.findViewById(R.id.staffName);
        TextView roleTv = convertView.findViewById(R.id.staffRole);
        TextView deptTv = convertView.findViewById(R.id.staffDepartment);

        nameTv.setText(staff.getName());
        roleTv.setText(staff.getRole());
        deptTv.setText(staff.getDepartment());

        if (staff.getPhotoPath() != null && !staff.getPhotoPath().isEmpty()) {
            String photoPath = staff.getPhotoPath();
            if (photoPath.startsWith(ImageUtils.BASE64_PREFIX)) {
                android.graphics.Bitmap bitmap = ImageUtils.decodeBase64ToBitmap(photoPath);
                if (bitmap != null) {
                    Glide.with(getContext())
                            .load(bitmap)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(imageView);
                } else {
                    imageView.setImageResource(R.drawable.ic_person);
                }
            } else {
                Glide.with(getContext())
                        .load(Uri.parse(photoPath))
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(imageView);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_person);
        }


        return convertView;
    }
}
