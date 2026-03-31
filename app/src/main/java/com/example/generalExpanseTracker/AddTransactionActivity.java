package com.example.generalExpanseTracker;

import android.os.Bundle;
import android.widget.*;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Calendar;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;
import com.example.generalExpanseTracker.Model.PaymentRequest;
import com.example.generalExpanseTracker.BaseActivity;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionActivity extends BaseActivity {

    EditText edtAmount, edtDesc, edtBank;
    Spinner spinnerType;
    Spinner spinnerCategory;
    Button btnSubmit;

    double totalBudget = 0;
    double totalSpent = 0;

    boolean isBudgetLoaded = false;
    boolean isTransactionsLoaded = false;

    private int lastNotifiedLevel = 0;

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

        enableBackButton();

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

                            // finish();
                            getBudgetAndNotify();

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

    private void getBudgetAndNotify() {

        String username = getSharedPreferences("app", MODE_PRIVATE)
                .getString("username", "");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // -------- GET BUDGET --------
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);

        isBudgetLoaded = false;
        isTransactionsLoaded = false;

        apiService.getBudget(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                totalBudget = 0;

                if (response.body() != null) {
                    Object dataObj = response.body().get("data");

                    if (dataObj instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        totalBudget = parseDoubleSafe(data.get("amount"));
                    } else if (dataObj instanceof List) {
                        List<?> list = (List<?>) dataObj;
                        if (!list.isEmpty() && list.get(0) instanceof Map) {
                            Map<String, Object> data = (Map<String, Object>) list.get(0);
                            totalBudget = parseDoubleSafe(data.get("amount"));
                        }
                    }
                }

                isBudgetLoaded = true;
                tryNotify();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("BUDGET_ERROR", t.getMessage());
            }
        });

        // -------- GET TRANSACTIONS --------
        Map<String, String> txnBody = new HashMap<>();
        txnBody.put("username", username);

        apiService.getTransactions(txnBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                totalSpent = 0;

                if (response.body() != null) {
                    Object dataObj = response.body().get("data");

                    if (dataObj instanceof List) {
                        List<?> list = (List<?>) dataObj;

                        Calendar now = Calendar.getInstance();

                        for (Object obj : list) {
                            if (!(obj instanceof Map))
                                continue;

                            Map<?, ?> txn = (Map<?, ?>) obj;

                            if (!"debit".equals(String.valueOf(txn.get("type"))))
                                continue;

                            long time = parseLongSafe(txn.get("time"));

                            Calendar txnCal = Calendar.getInstance();
                            txnCal.setTimeInMillis(time);

                            if (txnCal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                    txnCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {

                                double amount = parseDoubleSafe(txn.get("transactionAmount"));
                                totalSpent += amount;
                            }
                        }
                    }
                }

                isTransactionsLoaded = true;
                tryNotify();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("TXN_ERROR", t.getMessage());
            }
        });
    }

    private void tryNotify() {

        if (!isBudgetLoaded || !isTransactionsLoaded)
            return;

        if (totalBudget <= 0)
            return;

        int progress = (int) ((totalSpent / totalBudget) * 100);

        String message = "Used ₹" + totalSpent +
                " of ₹" + totalBudget +
                " (" + progress + "%)";

        NotificationHelper.showNotification(
                this,
                "Transaction Added 💸",
                message);

        checkThreshold(progress, message);

        finish(); // ✅ close screen AFTER notification
    }

    private void checkThreshold(int progress, String message) {

        if (progress >= 100 && lastNotifiedLevel < 100) {
            notifyUser("Budget Exhausted 🚨", message);
            lastNotifiedLevel = 100;

        } else if (progress >= 75 && lastNotifiedLevel < 75) {
            notifyUser("Warning ⚠️ (75%)", message);
            lastNotifiedLevel = 75;

        } else if (progress >= 50 && lastNotifiedLevel < 50) {
            notifyUser("Half Budget Used (50%)", message);
            lastNotifiedLevel = 50;

        } else if (progress >= 25 && lastNotifiedLevel < 25) {
            notifyUser("25% Budget Used", message);
            lastNotifiedLevel = 25;
        }
    }

    private void notifyUser(String title, String message) {
        NotificationHelper.showNotification(this, title, message);
    }

    private double parseDoubleSafe(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLongSafe(Object value) {
        try {
            return (long) Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }
}