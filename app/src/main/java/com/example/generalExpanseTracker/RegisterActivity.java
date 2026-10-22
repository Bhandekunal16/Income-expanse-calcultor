package com.example.generalExpanseTracker;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.ArrayList;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;
import com.example.generalExpanseTracker.Model.User;
import com.example.generalExpanseTracker.Model.Account;
import com.example.generalExpanseTracker.MainActivity;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etMobile, etEmail, etUsername, etPassword, etBank, etAccNumber, etBalance;
    private Button btnRegister;
    private ApiService apiService;
    private static final String NAME_REGEX = "^[A-Za-z ]{2,50}$";
    private static final String MOBILE_REGEX = "^[6-9]\\d{9}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{4,20}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@#$%^&+=!]{6,}$";
    private static final String BANK_REGEX = "^[A-Za-z ]{2,50}$";
    private static final String ACCOUNT_REGEX = "^\\d{9,18}$";
    private static final String BALANCE_REGEX = "^\\d+(\\.\\d{1,2})?$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        etBank = findViewById(R.id.etBankName);
        etAccNumber = findViewById(R.id.etAccountNumber);
        etBalance = findViewById(R.id.etBalance);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etName.getText().toString();
        String mobile = etMobile.getText().toString();
        String email = etEmail.getText().toString();
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();
        String bank = etBank.getText().toString().trim();
        String number = etAccNumber.getText().toString().trim();
        String balanceStr = etBalance.getText().toString().trim();

        boolean isValid = true;

        isValid &= validate(etName, name, NAME_REGEX, getStringByKey("invalidName"));
        isValid &= validate(etMobile, mobile, MOBILE_REGEX, getStringByKey("invalidMobile"));
        isValid &= validate(etEmail, email, EMAIL_REGEX, getStringByKey("invalidEmail"));
        isValid &= validate(etUsername, username, USERNAME_REGEX, getStringByKey("invalidUsername"));
        isValid &= validate(etPassword, password, PASSWORD_REGEX, getStringByKey("weakPassword"));
        isValid &= validate(etBank, bank, BANK_REGEX, getStringByKey("invalidBank"));
        isValid &= validate(etAccNumber, number, ACCOUNT_REGEX, getStringByKey("invalidAccount"));
        isValid &= validate(etBalance, balanceStr, BALANCE_REGEX, getStringByKey("invalidBalance"));

        if (!isValid) {
            notification(getStringByKey("fix_errors"));
            return;
        }

        if (bank.isEmpty() || number.isEmpty() || balanceStr.isEmpty()) {
            notification(getStringByKey("account_required"));
            return;
        }

        double balance = Double.parseDouble(balanceStr);
        String type = "savings";
        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account(type, number, bank, balance));
        User user = new User(name, mobile, email, username, password, accounts);
        Call<Object> call = apiService.registerUser(user);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!response.isSuccessful()) {
                    notification("Registration failed. Please try again.");
                    return;
                }
                Object registrationResult = response.body();
                if (registrationResult instanceof java.util.Map
                        && Boolean.FALSE.equals(
                                ((java.util.Map<?, ?>) registrationResult).get("status"))) {
                    notification("Registration failed. Please check the details and try again.");
                    return;
                }

                SessionManager.saveSession(RegisterActivity.this, mobile, username);
                SessionManager.validateSavedSession(
                        RegisterActivity.this,
                        new SessionManager.ValidationCallback() {
                            @Override
                            public void onValid(java.util.Map<String, Object> account) {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }
                                SessionManager.markUnlocked();
                                getSharedPreferences("app", MODE_PRIVATE)
                                        .edit()
                                        .putString("username", username)
                                        .putString("bank", bank)
                                        .putString("bankName", bank)
                                        .putString("number", number)
                                        .putString("accountNumber", number)
                                        .putString("balanceStr", balanceStr)
                                        .apply();
                                notification(getStringByKey("register_success"));
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.putExtra("mobile", mobile);
                                intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }

                            @Override
                            public void onInvalid() {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }
                                SessionManager.clearSession(RegisterActivity.this);
                                notification("Account created, but could not verify it. Please sign in.");
                            }

                            @Override
                            public void onError(String message) {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }
                                SessionManager.clearSession(RegisterActivity.this);
                                notification("Account created, but could not verify it: " + message);
                                Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                                loginIntent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginIntent);
                            }
                        });
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                notification("Error: " + t.getMessage());
            }
        });
    }

    private boolean validate(EditText et, String value, String regex, String msg) {
        if (value.isEmpty() || !value.matches(regex)) {
            et.setError(msg);
            return false;
        }
        return true;
    }

    private void notification(String text) {
        Toast.makeText(RegisterActivity.this, text, Toast.LENGTH_LONG).show();
    }

    private String getStringByKey(String key) {
        int resId = getResources().getIdentifier(key, "string", getPackageName());
        return resId != 0 ? getString(resId) : key;
    }
}