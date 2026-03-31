package com.example.generalExpanseTracker;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.generalExpanseTracker.api.ApiClient;
import com.example.generalExpanseTracker.api.ApiService;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;

import java.util.*;

import retrofit2.*;

public class VpaActivity extends AppCompatActivity {

    EditText etVpa, etAmount;
    Button btnSave;
    ImageView imgQR;
    ApiService apiService;
    String mobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpa);

        etVpa = findViewById(R.id.etVpa);
        btnSave = findViewById(R.id.btnSaveVpa);
        imgQR = findViewById(R.id.imgQR);
        etAmount = findViewById(R.id.etAmount);

        apiService = ApiClient.getClient().create(ApiService.class);

        mobile = getSharedPreferences("app", MODE_PRIVATE)
                .getString("mobile", "");

        loadAccount();

        btnSave.setOnClickListener(v -> saveVpa());

        // 🔥 Dynamic QR update on amount change
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String vpa = getSharedPreferences("app", MODE_PRIVATE)
                        .getString("vpa", "");

                if (vpa != null && !vpa.isEmpty()) {
                    showQR(vpa);
                }
            }
        });
    }

    private void loadAccount() {

        Map<String, String> body = new HashMap<>();
        Log.d("VPA_DEBUG", mobile);
        body.put("mobile", mobile);

        apiService.loginUser(body).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    showInput();
                    return;
                }

                Map<String, Object> res = response.body();
                Boolean status = (Boolean) res.get("status");

                Log.d("VPA_DEBUG", String.valueOf(res));

                if (status != null && status) {

                    Object dataObj = res.get("data");

                    if (dataObj instanceof Map) {

                        Map<String, Object> data = (Map<String, Object>) dataObj;

                        Object accObj = data.get("accounts");

                        if (accObj instanceof List) {

                            List<?> accounts = (List<?>) accObj;

                            if (!accounts.isEmpty() && accounts.get(0) instanceof Map) {

                                Map<String, Object> acc = (Map<String, Object>) accounts.get(0);

                                Object vpaObj = acc.get("vpa");

                                Log.d("VPA_DEBUG", String.valueOf(vpaObj));

                                if (vpaObj != null) {
                                    String vpa = String.valueOf(vpaObj);

                                    Log.d("VPA_DEBUG", vpa);

                                    if (!vpa.isEmpty() && !"null".equalsIgnoreCase(vpa)) {

                                        // ✅ Save locally
                                        getSharedPreferences("app", MODE_PRIVATE)
                                                .edit()
                                                .putString("vpa", vpa)
                                                .apply();

                                        showQR(vpa);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }

                showInput();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(VpaActivity.this, "Error", Toast.LENGTH_SHORT).show();
                showInput();
            }
        });
    }

    private void saveVpa() {
        String vpa = etVpa.getText().toString().trim();

        if (vpa.isEmpty()) {
            Toast.makeText(this, "Enter VPA", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = getSharedPreferences("app", MODE_PRIVATE)
                .getString("username", "");

        Log.d("VPA_DEBUG_username", username);
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("vpa", vpa);

        apiService.updateUser(body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {

                // ✅ Save locally
                getSharedPreferences("app", MODE_PRIVATE)
                        .edit()
                        .putString("vpa", vpa)
                        .apply();

                showQR(vpa);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Toast.makeText(VpaActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInput() {
        etVpa.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE);
        imgQR.setVisibility(View.GONE);
        etAmount.setVisibility(View.GONE);
    }

    private void showQR(String vpa) {
        etVpa.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
        imgQR.setVisibility(View.VISIBLE);
        etAmount.setVisibility(View.VISIBLE); // ✅ keep amount visible

        String amount = etAmount.getText().toString().trim();

        String upiLink;

        if (!amount.isEmpty()) {
            try {
                double amt = Double.parseDouble(amount);

                if (amt <= 0)
                    return;

                upiLink = "upi://pay?pa=" + vpa + "&am=" + amount + "&cu=INR";

            } catch (Exception e) {
                return; // ignore invalid typing
            }
        } else {
            upiLink = "upi://pay?pa=" + vpa;
        }

        Log.d("VPA_DEBUG", "QR: " + upiLink);

        generateQR(upiLink);
    }

    private void generateQR(String text) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(text, BarcodeFormat.QR_CODE, 500, 500);

            Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565);

            for (int x = 0; x < 500; x++) {
                for (int y = 0; y < 500; y++) {
                    bitmap.setPixel(x, y,
                            matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            imgQR.setImageBitmap(bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}