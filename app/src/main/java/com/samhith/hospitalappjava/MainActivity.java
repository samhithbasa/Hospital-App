package com.samhith.hospitalappjava;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button loginButton, signUpButton;
    private TextView errorText, signupSuccessText;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUP);
        errorText = findViewById(R.id.errorText);
        signupSuccessText = findViewById(R.id.signup_success);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        dbHelper = new DatabaseHelper(this);

        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());

        // Add admin user if not exists
        if (dbHelper.getUserRole("admin") == null) {
            dbHelper.addUser("admin", "admin123", "admin");
        }

        // Login functionality
        loginButton.setOnClickListener(view -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (dbHelper.checkUser(username, password)) {
                String role = dbHelper.getUserRole(username);
                int userId = dbHelper.getUserId(username);

                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                intent.putExtra("USERNAME", username);
                intent.putExtra("USER_ROLE", role);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            } else {
                errorText.setText(R.string.invalid_credentials);
            }
        });

        // Sign-up functionality with password validation
        signUpButton.setOnClickListener(view -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                signupSuccessText.setText(R.string.fill_all_fields);
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                return;
            }

            // Check: password must not contain username
            if (password.toLowerCase().contains(username.toLowerCase())) {
                signupSuccessText.setText("Password should not contain username.");
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                return;
            }

            // Optional: Check for minimum password length
            if (password.length() < 8) {
                signupSuccessText.setText("Password must be at least 8 characters long.");
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                return;
            }

            long result = dbHelper.addUser(username, password, "staff");

            if (result != -1) {
                signupSuccessText.setText(R.string.signup_success);
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_dark));
                usernameEditText.setText("");
                passwordEditText.setText("");
            } else {
                signupSuccessText.setText(R.string.username_exists);
                signupSuccessText
                        .setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your email");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "Reset email sent!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Error: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
