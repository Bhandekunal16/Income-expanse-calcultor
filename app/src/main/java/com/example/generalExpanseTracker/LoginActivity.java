package com.example.generalExpanseTracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText edtMobile;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionManager.hasStoredSession(this)) {
            SessionManager.lock();
            openAppLock();
        } else {
            showLoginScreen();
        }
    }

    private void showLoginScreen() {
        setContentView(R.layout.activity_login);
        edtMobile = findViewById(R.id.edtMobile);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        tvRegister.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        btnLogin.setOnClickListener(view -> login());
    }

    private void login() {
        String mobile = edtMobile.getText().toString().trim();
        if (!mobile.matches("^[6-9]\\d{9}$")) {
            Toast.makeText(this, "Please enter valid mobile number!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("mobile", mobile);
        apiService.loginUser(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnLogin.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LoginActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> result = response.body();
                Object dataValue = result.get("data");
                if (!Boolean.TRUE.equals(result.get("status")) || !(dataValue instanceof Map)) {
                    Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> account = (Map<String, Object>) dataValue;
                Object usernameValue = account.get("username");
                Object accountMobile = account.get("mobile");
                if (usernameValue == null
                        || String.valueOf(usernameValue).trim().isEmpty()
                        || accountMobile == null
                        || !mobile.equals(String.valueOf(accountMobile))) {
                    Toast.makeText(LoginActivity.this, "Account details could not be verified", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                SessionManager.persistAccountDetails(LoginActivity.this, mobile, account);
                SessionManager.markUnlocked();
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("mobile", mobile);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable throwable) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnLogin.setEnabled(true);
                String message = throwable.getLocalizedMessage();
                Toast.makeText(
                        LoginActivity.this,
                        message == null ? "Unable to contact the server." : "Error: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openAppLock() {
        Intent intent = new Intent(this, AppLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
