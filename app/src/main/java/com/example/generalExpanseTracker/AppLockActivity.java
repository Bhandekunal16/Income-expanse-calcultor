package com.example.generalExpanseTracker;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {
    private TextView tvLockMessage;
    private Button btnUseDeviceCredential;
    private Button btnDeviceSettings;
    private Button btnUnlock;
    private Button btnRetrySession;
    private ProgressBar progressLockValidation;
    private BiometricManager biometricManager;
    private BiometricPrompt biometricPrompt;
    private ActivityResultLauncher<Intent> credentialLauncher;
    private boolean strongBiometricAvailable;
    private boolean deviceCredentialAvailable;
    private boolean sessionValidated;
    private boolean sessionValidationInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.hasStoredSession(this)) {
            SessionManager.clearSession(this);
            openLogin();
            return;
        }

        setContentView(R.layout.activity_app_lock);
        tvLockMessage = findViewById(R.id.tvLockMessage);
        btnUnlock = findViewById(R.id.btnUnlock);
        btnUseDeviceCredential = findViewById(R.id.btnUseDeviceCredential);
        btnDeviceSettings = findViewById(R.id.btnDeviceSettings);
        btnRetrySession = findViewById(R.id.btnRetrySession);
        progressLockValidation = findViewById(R.id.progressLockValidation);
        Button btnLogout = findViewById(R.id.btnLogout);

        biometricManager = BiometricManager.from(this);
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                unlockApp();
            }

            @Override
            public void onAuthenticationFailed() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                tvLockMessage.setText("Authentication was not recognized. Try again or use your device screen lock.");
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && deviceCredentialAvailable) {
                    authenticateWithDeviceCredential();
                    return;
                }
                if (errorCode == BiometricPrompt.ERROR_LOCKOUT
                        || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    tvLockMessage.setText(
                            "Biometric authentication is locked. Use your device screen lock or try again later.");
                } else if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                        || errorCode == BiometricPrompt.ERROR_CANCELED) {
                    tvLockMessage.setText("Authentication cancelled. The app remains locked.");
                } else {
                    tvLockMessage.setText(String.valueOf(errString)
                            + " The app remains locked; try again or use your device screen lock.");
                }
            }
        });

        credentialLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (result.getResultCode() == RESULT_OK) {
                        unlockApp();
                    } else {
                        tvLockMessage.setText(
                                "Device authentication cancelled or unsuccessful. The app remains locked.");
                    }
                });

        btnUnlock.setOnClickListener(view -> authenticate());
        btnUseDeviceCredential.setOnClickListener(view -> authenticateWithDeviceCredential());
        btnRetrySession.setOnClickListener(view -> validateServerSession());
        btnDeviceSettings.setOnClickListener(view -> openDeviceSecuritySettings());
        btnLogout.setOnClickListener(view -> logout());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                tvLockMessage.setText("Authenticate or log out to leave this screen.");
            }
        });
        refreshAuthenticationMethods();
        validateServerSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvLockMessage != null && sessionValidated) {
            refreshAuthenticationMethods();
        }
    }

    private void validateServerSession() {
        if (sessionValidationInProgress || isFinishing()) {
            return;
        }
        sessionValidationInProgress = true;
        sessionValidated = false;
        btnUnlock.setEnabled(false);
        btnUseDeviceCredential.setEnabled(false);
        btnRetrySession.setVisibility(View.GONE);
        progressLockValidation.setVisibility(View.VISIBLE);
        tvLockMessage.setText("Verifying your saved account...");

        SessionManager.validateSavedSession(this, new SessionManager.ValidationCallback() {
            @Override
            public void onValid(java.util.Map<String, Object> account) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                sessionValidationInProgress = false;
                sessionValidated = true;
                progressLockValidation.setVisibility(View.GONE);
                refreshAuthenticationMethods();
            }

            @Override
            public void onInvalid() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                sessionValidationInProgress = false;
                SessionManager.clearSession(AppLockActivity.this);
                openLogin();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                sessionValidationInProgress = false;
                progressLockValidation.setVisibility(View.GONE);
                btnUnlock.setEnabled(false);
                btnUseDeviceCredential.setEnabled(false);
                btnRetrySession.setVisibility(View.VISIBLE);
                tvLockMessage.setText(
                        "Could not verify your saved account: " + message
                                + " The app remains locked. Retry or log out.");
            }
        });
    }

    private void refreshAuthenticationMethods() {
        int biometricStatus = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        strongBiometricAvailable = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS;

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        deviceCredentialAvailable = keyguardManager != null && keyguardManager.isDeviceSecure();
        boolean hasSecureMethod = strongBiometricAvailable || deviceCredentialAvailable;

        btnUseDeviceCredential.setVisibility(
                deviceCredentialAvailable && strongBiometricAvailable ? View.VISIBLE : View.GONE);
        btnDeviceSettings.setVisibility(hasSecureMethod ? View.GONE : View.VISIBLE);
        btnUnlock.setEnabled(sessionValidated);
        btnUseDeviceCredential.setEnabled(sessionValidated && deviceCredentialAvailable);

        if (!sessionValidated) {
            return;
        }

        if (strongBiometricAvailable && deviceCredentialAvailable) {
            tvLockMessage.setText("Use a strong biometric or your device screen lock to continue.");
        } else if (strongBiometricAvailable) {
            tvLockMessage.setText("Use an enrolled strong biometric to continue.");
        } else if (deviceCredentialAvailable) {
            tvLockMessage.setText("Biometrics are unavailable. Use your device screen lock to continue.");
        } else {
            tvLockMessage.setText(
                    "No secure device authentication is available. Set up a screen lock or log out.");
        }
    }

    private void authenticate() {
        if (!sessionValidated) {
            tvLockMessage.setText("Verify your saved account before authenticating. The app remains locked.");
            return;
        }
        refreshAuthenticationMethods();
        if (strongBiometricAvailable) {
            BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Daily Expense Tracker")
                    .setSubtitle("Verify your identity to view your finances");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && deviceCredentialAvailable) {
                promptBuilder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            } else {
                promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG);
                promptBuilder.setNegativeButtonText(
                        deviceCredentialAvailable ? "Use device screen lock" : "Cancel");
            }
            biometricPrompt.authenticate(promptBuilder.build());
        } else if (deviceCredentialAvailable) {
            authenticateWithDeviceCredential();
        } else {
            tvLockMessage.setText(
                    "Set up a secure screen lock in device settings, or log out. The app remains locked.");
        }
    }

    private void authenticateWithDeviceCredential() {
        if (!sessionValidated) {
            tvLockMessage.setText("Verify your saved account before authenticating. The app remains locked.");
            return;
        }
        refreshAuthenticationMethods();
        if (!deviceCredentialAvailable) {
            tvLockMessage.setText(
                    "A secure device PIN, pattern, or password is not set up. The app remains locked.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Daily Expense Tracker")
                    .setSubtitle("Confirm your device screen lock")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
            biometricPrompt.authenticate(promptInfo);
            return;
        }

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        Intent credentialIntent = keyguardManager == null
                ? null
                : keyguardManager.createConfirmDeviceCredentialIntent(
                        "Unlock Daily Expense Tracker",
                        "Confirm your device PIN, pattern, or password");
        if (credentialIntent == null) {
            deviceCredentialAvailable = false;
            refreshAuthenticationMethods();
            tvLockMessage.setText(
                    "The system device-credential screen is unavailable. The app remains locked.");
            return;
        }
        credentialLauncher.launch(credentialIntent);
    }

    private void openDeviceSecuritySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            tvLockMessage.setText(
                    "Set up a device PIN, pattern, or password, then return and unlock the app.");
        } catch (android.content.ActivityNotFoundException exception) {
            tvLockMessage.setText(
                    "Device security settings could not be opened. Log out or configure a screen lock manually.");
        }
    }

    private void unlockApp() {
        if (!sessionValidated || !SessionManager.hasStoredSession(this)) {
            logout();
            return;
        }
        SessionManager.markUnlocked();
        Intent dashboard = new Intent(this, MainActivity.class);
        dashboard.putExtra("mobile", SessionManager.getStoredMobile(this));
        dashboard.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(dashboard);
        finish();
    }

    private void logout() {
        SessionManager.clearSession(this);
        openLogin();
    }

    private void openLogin() {
        Intent login = new Intent(this, LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        finish();
    }
}
