package com.example.generalExpanseTracker;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;
import com.example.generalExpanseTracker.Model.PaymentRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionActivity extends AppCompatActivity {

    EditText edtAmount, edtDesc, edtBank;
    Spinner spinnerType;
    Spinner spinnerCategory;
    Button btnSubmit;

    String[] expenseCategories = {
            "food", "transport", "shopping", "bills", "entertainment",
            "health", "travel", "education", "groceries", "rent",
            "insurance", "investments", "loans", "subscriptions", "misc"
    };

    String[] incomeCategories = {
            "salary", "business", "freelance", "investments",
            "rental", "interest", "gifts", "refunds", "other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        String bankName = getSharedPreferences("app", MODE_PRIVATE)
                .getString("bankName", "");

        edtAmount = findViewById(R.id.edtAmount);
        edtDesc = findViewById(R.id.edtDesc);
        // edtBank = findViewById(R.id.edtBank);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Spinner setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] { "debit", "credit" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        updateCategorySpinner("debit");

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {

                String selectedType = spinnerType.getSelectedItem().toString();
                updateCategorySpinner(selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSubmit.setOnClickListener(v -> {

            String amountStr = edtAmount.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();
            String bank = bankName;
            String type = spinnerType.getSelectedItem().toString();
            String category = spinnerCategory.getSelectedItem().toString();

            if (amountStr.isEmpty() || desc.isEmpty() || bank.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            long time = System.currentTimeMillis();


            String username = getSharedPreferences("app", MODE_PRIVATE)
                    .getString("username", "");

            if (username.isEmpty()) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            PaymentRequest request = new PaymentRequest(
                    bank,
                    username,
                    amount,
                    desc,
                    type,
                    time,
                    category);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            apiService.createPayment(request).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call,
                        Response<Map<String, Object>> response) {

                    if (response.isSuccessful() && response.body() != null) {

                        Map<String, Object> res = response.body();

                        Boolean status = (Boolean) res.get("status");

                        if (status != null && status) {

                            Toast.makeText(AddTransactionActivity.this,
                                    "Transaction Added",
                                    Toast.LENGTH_SHORT).show();

                            finish();

                        } else {
                            Toast.makeText(AddTransactionActivity.this,
                                    "Failed to add",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(AddTransactionActivity.this,
                                "Server error",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(AddTransactionActivity.this,
                            "Error: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

        });
    }

    private void updateCategorySpinner(String type) {

        String[] categories;

        if (type.equals("debit")) {
            categories = expenseCategories;
        } else {
            categories = incomeCategories;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }
}