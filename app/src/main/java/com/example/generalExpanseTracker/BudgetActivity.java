
package com.example.generalExpanseTracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;

public class BudgetActivity extends BaseActivity {

    private static final String TAG = "BudgetActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;

    private EditText edtBudget;
    private Button btnSave;
    private TextView txtBudget;
    private TextView txtSpent;
    private TextView txtRemaining;
    private ProgressBar progressBar;

    private ApiService apiService;
    private String username;

    private double totalBudget = 0;
    private double totalSpent = 0;

    private boolean isBudgetLoaded = false;
    private boolean isTransactionsLoaded = false;

    private int lastNotifiedLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        enableBackButton();

        initializeViews();
        initializeServices();

        NotificationHelper.createChannel(this);
        requestNotificationPermission();

        getBudget();
        getTransactions();

        btnSave.setOnClickListener(v -> saveOrUpdateBudget());
    }

    private void initializeViews() {
        edtBudget = findViewById(R.id.edtBudget);
        btnSave = findViewById(R.id.btnSave);

        txtBudget = findViewById(R.id.txtBudget);
        txtSpent = findViewById(R.id.txtSpent);
        txtRemaining = findViewById(R.id.txtRemaining);

        progressBar = findViewById(R.id.progressBar);

        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    private void initializeServices() {
        apiService = ApiClient.getClient().create(ApiService.class);

        username = getSharedPreferences("app", MODE_PRIVATE)
                .getString("username", "");

        Log.d(TAG, "Budget screen initialized");
    }

    // --------------------------------------------------
    // SAVE OR UPDATE BUDGET
    // --------------------------------------------------

    private void saveOrUpdateBudget() {
        String input = edtBudget.getText().toString().trim();

        if (input.isEmpty()) {
            edtBudget.setError("Enter a budget amount");
            edtBudget.requestFocus();
            return;
        }

        final double amount;

        try {
            amount = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            edtBudget.setError("Enter a valid amount");
            edtBudget.requestFocus();
            return;
        }

        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0) {

            edtBudget.setError("Budget must be greater than zero");
            edtBudget.requestFocus();
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Unable to identify the user. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        edtBudget.setError(null);
        btnSave.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("amount", amount);

        Map<String, Object> checkBody = new HashMap<>();
        checkBody.put("username", username);

        apiService.getBudget(checkBody).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

                        if (!response.isSuccessful()) {
                            btnSave.setEnabled(true);

                            Log.e(TAG, "Budget lookup failed: "
                                    + response.code());

                            Toast.makeText(
                                    BudgetActivity.this,
                                    "Unable to check existing budget",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        boolean hasBudget = false;

                        if (response.body() != null) {
                            Object data = response.body().get("data");

                            if (data instanceof Map) {
                                hasBudget = true;
                            } else if (data instanceof List) {
                                hasBudget = !((List<?>) data).isEmpty();
                            }
                        }

                        if (hasBudget) {
                            callUpdate(body);
                        } else {
                            callAdd(body);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        btnSave.setEnabled(true);

                        Log.e(TAG, "Budget lookup error", t);

                        Toast.makeText(
                                BudgetActivity.this,
                                "Unable to check existing budget",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // --------------------------------------------------
    // FETCH BUDGET
    // --------------------------------------------------

    private void getBudget() {
        isBudgetLoaded = false;

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);

        apiService.getBudget(body).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            Log.e(TAG, "Failed to load budget: "
                                    + response.code());

                            return;
                        }

                        Object dataObj = response.body().get("data");
                        totalBudget = 0;

                        if (dataObj instanceof Map) {
                            Map<?, ?> data = (Map<?, ?>) dataObj;

                            totalBudget = parseDoubleSafe(
                                    data.get("amount")
                            );

                        } else if (dataObj instanceof List) {
                            List<?> list = (List<?>) dataObj;

                            if (!list.isEmpty()
                                    && list.get(0) instanceof Map) {

                                Map<?, ?> data =
                                        (Map<?, ?>) list.get(0);

                                totalBudget = parseDoubleSafe(
                                        data.get("amount")
                                );
                            }
                        }

                        isBudgetLoaded = true;
                        updateUI();
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        Log.e(TAG, "Budget API error", t);
                    }
                }
        );
    }

    // --------------------------------------------------
    // FETCH MONTHLY TRANSACTIONS
    // --------------------------------------------------

    private void getTransactions() {
        isTransactionsLoaded = false;

        Map<String, String> body = new HashMap<>();
        body.put("username", username);

        apiService.getTransactions(body).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            Log.e(TAG, "Failed to load transactions: "
                                    + response.code());

                            return;
                        }

                        Object dataObj = response.body().get("data");
                        totalSpent = 0;

                        if (dataObj instanceof List) {
                            List<?> transactions = (List<?>) dataObj;

                            Calendar now = Calendar.getInstance();

                            int currentMonth =
                                    now.get(Calendar.MONTH);

                            int currentYear =
                                    now.get(Calendar.YEAR);

                            for (Object obj : transactions) {
                                if (!(obj instanceof Map)) {
                                    continue;
                                }

                                Map<?, ?> txn = (Map<?, ?>) obj;

                                String type = String.valueOf(
                                        txn.get("type")
                                );

                                // Count debit transactions only.
                                if (!"debit".equalsIgnoreCase(type)) {
                                    continue;
                                }

                                long time = parseLongSafe(
                                        txn.get("time")
                                );

                                Calendar txnCal =
                                        Calendar.getInstance();

                                txnCal.setTimeInMillis(time);

                                boolean sameMonth =
                                        txnCal.get(Calendar.MONTH)
                                                == currentMonth
                                        && txnCal.get(Calendar.YEAR)
                                                == currentYear;

                                if (!sameMonth) {
                                    continue;
                                }

                                double amount = parseDoubleSafe(
                                        txn.get("transactionAmount")
                                );

                                if (amount > 0
                                        && !Double.isInfinite(amount)) {
                                    totalSpent += amount;
                                }
                            }
                        }

                        isTransactionsLoaded = true;
                        updateUI();
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        Log.e(TAG, "Transactions API error", t);
                    }
                }
        );
    }

    // --------------------------------------------------
    // UPDATE BUDGET UI
    // --------------------------------------------------

    private void updateUI() {
        if (!isBudgetLoaded || !isTransactionsLoaded) {
            return;
        }

        double remaining = totalBudget - totalSpent;

        txtBudget.setText(
                "Budget: " + formatCurrency(totalBudget)
        );

        txtSpent.setText(
                "Spent: " + formatCurrency(totalSpent)
        );

        txtRemaining.setText(
                "Remaining: " + formatCurrency(remaining)
        );

        int progress = 0;

        if (totalBudget > 0) {
            progress = (int) Math.min(
                    100,
                    Math.max(
                            0,
                            (totalSpent / totalBudget) * 100
                    )
            );
        }

        progressBar.setMax(100);
        progressBar.setProgress(progress);

        Log.d(
                TAG,
                "Budget=" + totalBudget
                        + ", Spent=" + totalSpent
                        + ", Remaining=" + remaining
                        + ", Progress=" + progress
        );

        if (totalBudget > 0) {
            String message = "Used "
                    + formatCurrency(totalSpent)
                    + " of "
                    + formatCurrency(totalBudget)
                    + " (" + progress + "%)";

            checkThreshold(progress, message);
        } else {
            lastNotifiedLevel = 0;
        }
    }

    private String formatCurrency(double amount) {
        return "₹" + String.format(
                Locale.US,
                "%,.2f",
                amount
        );
    }

    // --------------------------------------------------
    // ADD BUDGET
    // --------------------------------------------------

    private void callAdd(Map<String, Object> body) {
        apiService.addBudget(body).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

                        btnSave.setEnabled(true);

                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Add budget failed: "
                                    + response.code());

                            Toast.makeText(
                                    BudgetActivity.this,
                                    "Failed to add budget",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        Toast.makeText(
                                BudgetActivity.this,
                                "Budget added successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        lastNotifiedLevel = 0;
                        getBudget();
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        btnSave.setEnabled(true);

                        Log.e(TAG, "Add budget error", t);

                        Toast.makeText(
                                BudgetActivity.this,
                                "Failed to add budget",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // --------------------------------------------------
    // UPDATE BUDGET
    // --------------------------------------------------

    private void callUpdate(Map<String, Object> body) {
        apiService.updateBudget(body).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

                        btnSave.setEnabled(true);

                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Update budget failed: "
                                    + response.code());

                            Toast.makeText(
                                    BudgetActivity.this,
                                    "Failed to update budget",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        Toast.makeText(
                                BudgetActivity.this,
                                "Budget updated successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        lastNotifiedLevel = 0;
                        getBudget();
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        btnSave.setEnabled(true);

                        Log.e(TAG, "Update budget error", t);

                        Toast.makeText(
                                BudgetActivity.this,
                                "Failed to update budget",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // --------------------------------------------------
    // SAFE PARSING
    // --------------------------------------------------

    private double parseDoubleSafe(Object value) {
        if (value == null) {
            return 0;
        }

        try {
            double result = Double.parseDouble(
                    String.valueOf(value)
            );

            if (Double.isNaN(result)
                    || Double.isInfinite(result)) {
                return 0;
            }

            return result;

        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid numeric value: " + value);
            return 0;
        }
    }

    private long parseLongSafe(Object value) {
        if (value == null) {
            return 0;
        }

        try {
            return (long) Double.parseDouble(
                    String.valueOf(value)
            );

        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid timestamp: " + value);
            return 0;
        }
    }

    // --------------------------------------------------
    // BUDGET THRESHOLD NOTIFICATIONS
    // --------------------------------------------------

    private void checkThreshold(int progress, String message) {
        int currentLevel;

        if (progress >= 100) {
            currentLevel = 100;
        } else if (progress >= 75) {
            currentLevel = 75;
        } else if (progress >= 50) {
            currentLevel = 50;
        } else if (progress >= 25) {
            currentLevel = 25;
        } else {
            currentLevel = 0;
        }

        // Notify only when crossing into a higher threshold.
        if (currentLevel > lastNotifiedLevel) {
            switch (currentLevel) {
                case 25:
                    notifyUser(
                            "25% Budget Used",
                            message
                    );
                    break;

                case 50:
                    notifyUser(
                            "Half Budget Used (50%)",
                            message
                    );
                    break;

                case 75:
                    notifyUser(
                            "Warning ⚠️ (75%)",
                            message
                    );
                    break;

                case 100:
                    notifyUser(
                            "Budget Exhausted 🚨",
                            message
                    );
                    break;
            }
        }

        // Allow a threshold notification again if spending
        // later falls below that level and rises back up.
        lastNotifiedLevel = currentLevel;
    }

    private void notifyUser(String title, String message) {
        NotificationHelper.showNotification(
                this,
                title,
                message
        );
    }

    // --------------------------------------------------
    // NOTIFICATION PERMISSION
    // --------------------------------------------------

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_REQUEST
                );
            }
        }
    }
}
