package com.example.generalExpanseTracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.generalExpanseTracker.api.ApiService;
import com.example.generalExpanseTracker.api.ApiClient;

public class LoginActivity extends AppCompatActivity {
    private EditText edtMobile;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtMobile = findViewById(R.id.edtMobile);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            String mobile = edtMobile.getText().toString().trim();
            String mobileNumberRegex = "^[6-9]\\d{9}$";

            if (!mobile.matches(mobileNumberRegex)) {
                Toast.makeText(LoginActivity.this, "Please enter valid mobile number!", Toast.LENGTH_SHORT).show();
            } else {
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Map<String, String> body = new HashMap<>();
                body.put("mobile", mobile);
                Call<Map<String, Object>> call = apiService.loginUser(body);
                call.enqueue(new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Object> res = response.body();
                            Boolean status = (Boolean) res.get("status");
                            if (status != null && status) {
                                Map<String, Object> data = (Map<String, Object>) res.get("data");
                                if (data != null && data.get("username") != null) {
                                    String username = data.get("username").toString();
                                    List<Map<String, Object>> accounts = (List<Map<String, Object>>) data
                                            .get("accounts");

                                    if (accounts != null && !accounts.isEmpty()) {
                                        Map<String, Object> acc = accounts.get(0);
                                        String bankName = (String) acc.get("bankName");
                                        String number = (String) acc.get("number");
                                        Double balance = (Double) acc.get("balance");

                                        getSharedPreferences("app", MODE_PRIVATE)
                                                .edit()
                                                .putString("username", username)
                                                .putString("bankName", bankName)
                                                .putString("accountNumber", number)
                                                .putFloat("balance", balance != null ? balance.floatValue() : 0f)
                                                .apply();
                                    }

                                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.putExtra("mobile", edtMobile.getText().toString());
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Username missing", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Toast.makeText(LoginActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}