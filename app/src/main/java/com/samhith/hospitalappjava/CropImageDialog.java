package com.samhith.hospitalappjava;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.canhub.cropper.CropImageView;
import com.google.android.material.button.MaterialButton;

public class CropImageDialog extends DialogFragment {

    private static final String ARG_IMAGE_URI = "image_uri";

    private Uri imageUri;
    private OnCropImageListener listener;

    private CropImageView cropImageView;
    private FrameLayout progressOverlay;

    public interface OnCropImageListener {
        void onCropSuccess(Uri croppedUri);
        void onCropCancelled();
    }

    public static CropImageDialog newInstance(Uri imageUri) {
        CropImageDialog fragment = new CropImageDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnCropImageListener(OnCropImageListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable(ARG_IMAGE_URI);
        }
        // Set style for a fullscreen overlay dialog
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_crop_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cropImageView = view.findViewById(R.id.dialogCropImageView);
        progressOverlay = view.findViewById(R.id.progressOverlay);
        ImageButton btnCancel = view.findViewById(R.id.btnCancelCrop);
        ImageButton btnSave = view.findViewById(R.id.btnSaveCrop);
        ImageButton btnRotateLeft = view.findViewById(R.id.btnRotateLeft);

        // Configure Crop Options for Circular Avatar Photo
        cropImageView.setGuidelines(CropImageView.Guidelines.ON);
        cropImageView.setFixedAspectRatio(true);
        cropImageView.setAspectRatio(1, 1);
        cropImageView.setCropShape(CropImageView.CropShape.OVAL);

        // Load image
        if (imageUri != null) {
            cropImageView.setImageUriAsync(imageUri);
        } else {
            Toast.makeText(getContext(), "Invalid Image URI", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        // Set Complete Listener
        cropImageView.setOnCropImageCompleteListener((v, result) -> {
            progressOverlay.setVisibility(View.GONE);
            if (result.isSuccessful()) {
                Uri croppedUri = result.getUriContent();
                if (croppedUri != null && listener != null) {
                    listener.onCropSuccess(croppedUri);
                }
                dismiss();
            } else {
                Exception error = result.getError();
                String errorMsg = error != null ? error.getMessage() : "Unknown cropping error";
                Toast.makeText(getContext(), "Cropping failed: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        // Click listeners
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCropCancelled();
            }
            dismiss();
        });

        btnSave.setOnClickListener(v -> {
            progressOverlay.setVisibility(View.VISIBLE);
            cropImageView.croppedImageAsync(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    90,
                    0,
                    0,
                    CropImageView.RequestSizeOptions.NONE,
                    null
            );
        });

        btnRotateLeft.setOnClickListener(v -> cropImageView.rotateImage(90));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }
}
