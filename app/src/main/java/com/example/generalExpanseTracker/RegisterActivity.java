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

    EditText etName, etMobile, etEmail, etUsername, etPassword;
    EditText etBank, etAccNumber, etBalance;
    Button btnRegister;

    ApiService apiService;

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

        if (bank.isEmpty() || number.isEmpty() || balanceStr.isEmpty()) {
            Toast.makeText(this, "Account details required", Toast.LENGTH_SHORT).show();
            return;
        }

        double balance = Double.parseDouble(balanceStr);

        // Default type
        String type = "savings";

        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account(type, number, bank, balance));

        User user = new User(name, mobile, email, username, password, accounts);

        Call<Object> call = apiService.registerUser(user);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Toast.makeText(RegisterActivity.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.putExtra("mobile", mobile);
                String username = etUsername.getText().toString();
                getSharedPreferences("app", MODE_PRIVATE)
                        .edit()
                        .putString("username", username)
                        .putString("bank", bank)
                        .putString("number", number)
                        .putString("balanceStr", balanceStr)
                        .apply();



                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}