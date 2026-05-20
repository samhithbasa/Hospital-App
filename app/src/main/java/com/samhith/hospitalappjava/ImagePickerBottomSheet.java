package com.samhith.hospitalappjava;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

public class ImagePickerBottomSheet extends BottomSheetDialogFragment {

    private OnImageSelectedListener listener;
    private Uri cameraUri;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    openCropDialog(uri);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraUri != null) {
                    openCropDialog(cameraUri);
                } else {
                    Toast.makeText(getContext(), "Camera capture cancelled or failed", Toast.LENGTH_SHORT).show();
                }
            });

    public interface OnImageSelectedListener {
        void onImageSelected(Uri croppedUri);
    }

    public static ImagePickerBottomSheet newInstance(OnImageSelectedListener listener) {
        ImagePickerBottomSheet fragment = new ImagePickerBottomSheet();
        fragment.setListener(listener);
        return fragment;
    }

    public void setListener(OnImageSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_select_photo_source, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout btnCamera = view.findViewById(R.id.btnCamera);
        LinearLayout btnGallery = view.findViewById(R.id.btnGallery);

        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnCamera.setOnClickListener(v -> {
            try {
                File photoFile = new File(requireContext().getCacheDir(), "temp_profile_photo.jpg");
                if (photoFile.exists()) {
                    photoFile.delete();
                }
                cameraUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", photoFile);
                cameraLauncher.launch(cameraUri);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCropDialog(Uri imageUri) {
        CropImageDialog cropDialog = CropImageDialog.newInstance(imageUri);
        cropDialog.setOnCropImageListener(new CropImageDialog.OnCropImageListener() {
            @Override
            public void onCropSuccess(Uri croppedUri) {
                if (listener != null) {
                    listener.onImageSelected(croppedUri);
                }
                dismiss();
            }

            @Override
            public void onCropCancelled() {
                // Keep the flow intact or do nothing
            }
        });
        cropDialog.show(getParentFragmentManager(), "crop_dialog");
        dismiss(); // Dismiss the bottom sheet after choosing image
    }
}
