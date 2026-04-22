package czb.budgethelper;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Locale;

import czb.budgethelper.db.AppDatabase;
import czb.budgethelper.db.BudgetDao;
import czb.budgethelper.db.SessionEntity;
import czb.budgethelper.db.SessionItemEntity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.widget.ImageView;
import android.content.Intent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText budgetEditText;
    private TextInputEditText zipEditText;
    private TextInputEditText itemNameEditText;
    private TextInputEditText itemPriceEditText;

    private TextView taxLabelText;
    private TextView subtotalTextView;
    private TextView taxTextView;
    private TextView totalTextView;
    private TextView remainingTextView;
    private TextView emptyStateText;
    private MaterialCardView remainingCard;

    private RecyclerView recyclerView;

    private ArrayList<BudgetItem> itemList;
    private BudgetItemAdapter adapter;

    private double subtotal = 0.0;
    private double taxRate = 0.0;

    private BudgetDao dao;
    private boolean sessionSavedThisStop = false;

    private ImageView pigImage;
    private ImageView hammerImage;
    private ImageView coinImage;

    // ── Spending Meter (Thinh Le's animation) ──────────────────────────────
    private ImageView gaugeNeedle;
    private TextView gaugeStatusText;
    private com.google.android.material.card.MaterialCardView gaugeCard;
    private float currentNeedleRotation = 90f; // starts at F (full, +90°)
    private int lastGaugeZone = 0; // 0=green, 1=yellow, 2=red

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        getCurrentZipFromLocation();
                    } else {
                        Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        dao = AppDatabase.getDatabase(this).budgetDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == R.id.action_reports) {
                startActivity(new Intent(this, ReportActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.action_graph) {
                saveSessionIfNeeded();

                new android.os.Handler(getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(this, MonthlyGraphActivity.class));
                }, 500); // wait 0.5 seconds

                return true;
            }

            if (item.getItemId() == R.id.action_new_session) {
                confirmNewSession();
                return true;
            }

            // ✅ ADD THIS
            if (item.getItemId() == R.id.action_about) {
                showAboutDialog();
                return true;
            }

            return false;
        });

        pigImage = findViewById(R.id.pigImage);
        hammerImage = findViewById(R.id.hammerImage);
        coinImage = findViewById(R.id.coinImage);

        gaugeNeedle = findViewById(R.id.gaugeNeedle);
        gaugeStatusText = findViewById(R.id.gaugeStatusText);
        gaugeCard = findViewById(R.id.gaugeCard);

        // Set needle pivot to the gauge centre (100/200 wide, 100/120 tall in viewport)
        gaugeNeedle.post(() -> {
            gaugeNeedle.setPivotX(gaugeNeedle.getWidth() / 2f);
            gaugeNeedle.setPivotY(gaugeNeedle.getHeight() * (100f / 120f));
            gaugeNeedle.setRotation(90f); // start at F
        });

        budgetEditText = findViewById(R.id.budgetEditText);
        zipEditText = findViewById(R.id.zipEditText);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemPriceEditText = findViewById(R.id.itemPriceEditText);

        taxLabelText = findViewById(R.id.taxLabelText);
        subtotalTextView = findViewById(R.id.subtotalTextView);
        taxTextView = findViewById(R.id.taxTextView);
        totalTextView = findViewById(R.id.totalTextView);
        remainingTextView = findViewById(R.id.remainingTextView);
        emptyStateText = findViewById(R.id.emptyStateText);
        remainingCard = findViewById(R.id.remainingCard);

        recyclerView = findViewById(R.id.recyclerView);

        itemList = new ArrayList<>();
        adapter = new BudgetItemAdapter(itemList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // ZIP TextWatcher — update tax rate when 5 digits entered
        zipEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String zip = s.toString().trim();
                if (zip.length() == 5) {
                    taxRate = TaxRateHelper.getTaxRate(zip);
                    String state = TaxRateHelper.getState(zip);
                    if (taxRate > 0) {
                        taxLabelText.setText(String.format(Locale.US, "Tax  (%.2f%%%s)",
                                taxRate * 100, state.isEmpty() ? "" : " · " + state));
                    } else {
                        taxLabelText.setText(state.isEmpty()
                                ? "Tax  (0.00% — no sales tax)"
                                : String.format("Tax  (0.00%% · %s)", state));
                    }
                } else {
                    taxRate = 0.0;
                    taxLabelText.setText("Tax");
                }
                recalculateWithCurrentBudget();
            }
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        BudgetItem deletedItem = adapter.getItemAt(position);

                        subtotal -= deletedItem.getPrice();
                        adapter.removeItem(position);

                        updateEmptyState();
                        recalculateWithCurrentBudget();

                        String budgetText = budgetEditText.getText() != null
                                ? budgetEditText.getText().toString().trim() : "";
                        double budget = 0.0;
                        try {
                            budget = Double.parseDouble(budgetText);
                        } catch (NumberFormatException ignored) {}

                        double taxAmount = subtotal * taxRate;
                        double total = subtotal + taxAmount;

                        if (budget > 0 && total <= budget) {
                            playCoinDropAnimation();
                        }

                        Toast.makeText(MainActivity.this,
                                deletedItem.getName() + " removed",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        itemTouchHelper.attachToRecyclerView(recyclerView);

        findViewById(R.id.addButton).setOnClickListener(v -> addItem());

        findViewById(R.id.useLocationButton).setOnClickListener(v -> checkLocationPermissionAndFetchZip());
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSessionIfNeeded();
    }

    private void saveSessionIfNeeded() {
        if (itemList.isEmpty()) return;

        String budgetText = budgetEditText.getText() != null
                ? budgetEditText.getText().toString().trim() : "";
        double budget = 0.0;
        try { budget = Double.parseDouble(budgetText); } catch (NumberFormatException ignored) {}

        String zip = zipEditText.getText() != null
                ? zipEditText.getText().toString().trim() : "";

        double taxAmount = subtotal * taxRate;
        double total = subtotal + taxAmount;

        final SessionEntity session = new SessionEntity(
                System.currentTimeMillis(), budget, zip, taxRate, subtotal, taxAmount, total);
        final ArrayList<BudgetItem> snapshot = new ArrayList<>(itemList);

        new Thread(() -> {
            long sessionId = dao.insertSession(session);
            for (BudgetItem item : snapshot) {
                dao.insertItem(new SessionItemEntity(sessionId, item.getName(), item.getPrice()));
            }
        }).start();
    }

    private void confirmNewSession() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.new_session_title))
                .setMessage(getString(R.string.new_session_message))
                .setPositiveButton(getString(R.string.new_session_confirm), (dialog, which) -> resetSession())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void resetSession() {
        itemList.clear();
        adapter.notifyDataSetChanged();
        subtotal = 0.0;
        taxRate = 0.0;
        taxLabelText.setText("Tax");
        budgetEditText.setText("");
        zipEditText.setText("");
        updateTotals(0.0);
        updateEmptyState();
        pigImage.setImageResource(R.drawable.nocrack);

        // Reset gauge to F (full)
        currentNeedleRotation = 90f;
        lastGaugeZone = 0;
        if (gaugeNeedle != null) {
            ObjectAnimator.ofFloat(gaugeNeedle, "rotation", gaugeNeedle.getRotation(), 90f)
                    .setDuration(600).start();
        }
        if (gaugeStatusText != null) {
            gaugeStatusText.setText(getString(R.string.gauge_status_ready));
            gaugeStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
        if (gaugeCard != null) {
            gaugeCard.setCardBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface)));
        }
    }

    private void addItem() {
        String budgetText = budgetEditText.getText() != null
                ? budgetEditText.getText().toString().trim() : "";
        String itemName = itemNameEditText.getText() != null
                ? itemNameEditText.getText().toString().trim() : "";
        String itemPriceText = itemPriceEditText.getText() != null
                ? itemPriceEditText.getText().toString().trim() : "";

        if (TextUtils.isEmpty(budgetText)) {
            Toast.makeText(this, getString(R.string.error_budget), Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(itemName)) {
            Toast.makeText(this, getString(R.string.error_item_name), Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(itemPriceText)) {
            Toast.makeText(this, getString(R.string.error_item_price), Toast.LENGTH_SHORT).show();
            return;
        }

        double budget;
        double itemPrice;
        try {
            budget = Double.parseDouble(budgetText);
            itemPrice = Double.parseDouble(itemPriceText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.error_numbers), Toast.LENGTH_SHORT).show();
            return;
        }

        itemList.add(new BudgetItem(itemName, itemPrice));
        adapter.notifyItemInserted(itemList.size() - 1);

        subtotal += itemPrice;
        updateTotals(budget);
        updateEmptyState();

        playHammerHitAnimation();

        itemNameEditText.setText("");
        itemPriceEditText.setText("");
        itemNameEditText.requestFocus();
    }
    private void recalculateWithCurrentBudget() {
        String budgetText = budgetEditText.getText() != null
                ? budgetEditText.getText().toString().trim() : "";
        double budget = 0.0;
        try { budget = Double.parseDouble(budgetText); } catch (NumberFormatException ignored) {}
        updateTotals(budget);
    }

    private void updateTotals(double budget) {
        double taxAmount = subtotal * taxRate;
        double total = subtotal + taxAmount;
        double remaining = budget - total;

        subtotalTextView.setText(String.format(Locale.US, "$%.2f", subtotal));
        taxTextView.setText(String.format(Locale.US, "$%.2f", taxAmount));
        totalTextView.setText(String.format(Locale.US, "$%.2f", total));
        remainingTextView.setText(String.format(Locale.US, "$%.2f", remaining));

        int cardColor = ContextCompat.getColor(this,
                remaining < 0 ? R.color.remaining_negative : R.color.remaining_positive);
        remainingCard.setCardBackgroundColor(ColorStateList.valueOf(cardColor));

        updatePigState(budget, total);
        updateGauge(budget, total);
    }

    // ── Spending Meter animation (Thinh Le) ────────────────────────────────

    /**
     * Updates the fuel-gauge needle to reflect how much of the budget has been spent.
     *
     * Story:
     *  • 0 % spent  → needle rests at F (full, +90° rotation)
     *  • 50% spent  → needle points straight up (0°)
     *  • 100%+ spent→ needle swings to E (empty, −90° or beyond)
     *
     * Zone crossing triggers extra animations:
     *  • Entering yellow → warning wobble
     *  • Entering red    → alarm shake + card flash
     *  • Returning green → celebration bounce
     */
    private void updateGauge(double budget, double total) {
        if (gaugeNeedle == null || budget <= 0) {
            if (gaugeStatusText != null) {
                gaugeStatusText.setText(getString(R.string.gauge_status_ready));
                gaugeStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
            return;
        }

        double percentSpent = total / budget;
        // rotation: +90° = F (0% spent), 0° = 50%, −90° = E (100% spent)
        float targetRotation = (float) (90.0 - percentSpent * 180.0);
        targetRotation = Math.max(-105f, Math.min(90f, targetRotation)); // clamp

        // Determine zone (0=green, 1=yellow, 2=red)
        int zone = percentSpent < 0.6 ? 0 : percentSpent < 0.8 ? 1 : 2;

        animateNeedle(targetRotation, zone);

        // Status text + colour
        String statusMsg;
        int statusColor;
        if (percentSpent <= 0) {
            statusMsg = getString(R.string.gauge_status_ready);
            statusColor = ContextCompat.getColor(this, R.color.text_secondary);
        } else if (zone == 0) {
            statusMsg = getString(R.string.gauge_status_ok);
            statusColor = ContextCompat.getColor(this, R.color.remaining_positive);
        } else if (zone == 1) {
            statusMsg = getString(R.string.gauge_status_low);
            statusColor = 0xFFFFB300; // amber
        } else {
            statusMsg = getString(R.string.gauge_status_over);
            statusColor = ContextCompat.getColor(this, R.color.remaining_negative);
        }
        gaugeStatusText.setText(statusMsg);
        gaugeStatusText.setTextColor(statusColor);

        // Zone-crossing animations
        if (zone != lastGaugeZone) {
            if (zone == 1) {
                playNeedleWobble();         // entering yellow — gentle warning
            } else if (zone == 2) {
                playNeedleAlarm();          // entering red — dramatic shake
                playGaugeCardFlash();
            } else if (lastGaugeZone > 0) {
                playNeedleCelebration();    // returning to green — bounce
            }
            lastGaugeZone = zone;
        }
    }

    /** Smoothly rotates the needle to the target angle. */
    private void animateNeedle(float targetRotation, int zone) {
        android.animation.TimeInterpolator interpolator = zone == 2
                ? new android.view.animation.OvershootInterpolator(1.5f)
                : new android.view.animation.DecelerateInterpolator();

        ObjectAnimator anim = ObjectAnimator.ofFloat(
                gaugeNeedle, "rotation", currentNeedleRotation, targetRotation);
        anim.setDuration(450);
        anim.setInterpolator(interpolator);
        anim.start();
        currentNeedleRotation = targetRotation;
    }

    /** Gentle left-right wobble when entering the yellow (caution) zone. */
    private void playNeedleWobble() {
        float base = currentNeedleRotation;
        ObjectAnimator wobble = ObjectAnimator.ofFloat(
                gaugeNeedle, "rotation",
                base, base - 8f, base + 8f, base - 5f, base + 5f, base);
        wobble.setDuration(500);
        wobble.setInterpolator(new android.view.animation.LinearInterpolator());
        wobble.start();
    }

    /** Rapid alarm shake when entering the red (over-budget) zone. */
    private void playNeedleAlarm() {
        float base = currentNeedleRotation;
        ObjectAnimator alarm = ObjectAnimator.ofFloat(
                gaugeNeedle, "rotation",
                base, base - 15f, base + 15f, base - 12f, base + 12f,
                base - 8f, base + 8f, base);
        alarm.setDuration(700);
        alarm.setInterpolator(new android.view.animation.LinearInterpolator());
        alarm.start();
    }

    /**
     * Brief card background flash to red when going over budget.
     * Uses a colour change sequence on the gauge card itself.
     */
    private void playGaugeCardFlash() {
        if (gaugeCard == null) return;
        int flashColor = ContextCompat.getColor(this, R.color.remaining_negative);
        int normalColor = ContextCompat.getColor(this, R.color.surface);

        gaugeCard.setCardBackgroundColor(ColorStateList.valueOf(flashColor));
        gaugeCard.postDelayed(() ->
                gaugeCard.setCardBackgroundColor(ColorStateList.valueOf(normalColor)), 400);
    }

    /** Small upward bounce on the needle when returning to the green zone. */
    private void playNeedleCelebration() {
        float base = currentNeedleRotation;
        ObjectAnimator celebrate = ObjectAnimator.ofFloat(
                gaugeNeedle, "rotation",
                base, base + 20f, base - 5f, base + 8f, base);
        celebrate.setDuration(500);
        celebrate.setInterpolator(new android.view.animation.DecelerateInterpolator());
        celebrate.start();
    }

    private void updatePigState(double budget, double total) {
        if (pigImage == null) return;

        if (budget <= 0) {
            pigImage.setImageResource(R.drawable.nocrack);
            return;
        }

        double percentUsed = total / budget;

        if (percentUsed > 1.0) {
            pigImage.setImageResource(R.drawable.crackfull);
        } else if (percentUsed > 0.7) {
            pigImage.setImageResource(R.drawable.crack);
        } else {
            pigImage.setImageResource(R.drawable.nocrack);
        }
    }

    private void playHammerHitAnimation() {
        if (hammerImage == null || pigImage == null) return;

        hammerImage.setVisibility(View.VISIBLE);
        hammerImage.setAlpha(1f);
        hammerImage.setRotation(0f);
        hammerImage.setTranslationY(-150f);
        hammerImage.setTranslationX(80f);

        ObjectAnimator drop = ObjectAnimator.ofFloat(
                hammerImage, "translationY", -150f, -10f);
        drop.setDuration(220);

        ObjectAnimator rotate = ObjectAnimator.ofFloat(
                hammerImage, "rotation", 0f, -45f);
        rotate.setDuration(220);

        AnimatorSet hammerSet = new AnimatorSet();
        hammerSet.playTogether(drop, rotate);
        hammerSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                playPigShakeAnimation();
                hammerImage.setVisibility(View.GONE);
            }
        });
        hammerSet.start();
    }

    private void playPigShakeAnimation() {
        ObjectAnimator shake = ObjectAnimator.ofFloat(
                pigImage,
                "translationX",
                0f, -12f, 12f, -8f, 8f, -4f, 4f, 0f
        );
        shake.setDuration(260);
        shake.start();
    }

    private void updateEmptyState() {
        emptyStateText.setVisibility(itemList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void playCoinDropAnimation() {
        if (coinImage == null || pigImage == null) return;

        coinImage.setVisibility(View.VISIBLE);

        coinImage.setTranslationY(-200f);
        coinImage.setAlpha(1f);

        ObjectAnimator drop = ObjectAnimator.ofFloat(
                coinImage,
                "translationY",
                -200f,
                40f
        );
        drop.setDuration(400);

        drop.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                coinImage.setVisibility(View.GONE);
                playPigBounce();
            }
        });

        drop.start();
    }

    private void playPigBounce() {
        ObjectAnimator up = ObjectAnimator.ofFloat(
                pigImage,
                "translationY",
                0f,
                -25f
        );
        up.setDuration(150);

        ObjectAnimator down = ObjectAnimator.ofFloat(
                pigImage,
                "translationY",
                -25f,
                0f
        );
        down.setDuration(150);

        AnimatorSet bounce = new AnimatorSet();
        bounce.playSequentially(up, down);
        bounce.start();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_title))
                .setMessage(getString(R.string.about_message))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void checkLocationPermissionAndFetchZip() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentZipFromLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentZipFromLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                handleLocationResult(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "Location still null", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getZipCodeFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String postalCode = addresses.get(0).getPostalCode();
                if (!TextUtils.isEmpty(postalCode)) {
                    if (postalCode.length() >= 5) {
                        return postalCode.substring(0, 5);
                    }
                    return postalCode;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                )
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        handleLocationResult(location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_SHORT).show()
                );
    }

    private void handleLocationResult(double latitude, double longitude) {
        String zipCode = getZipCodeFromLocation(latitude, longitude);

        if (!TextUtils.isEmpty(zipCode) && zipCode.length() == 5) {
            zipEditText.setText(zipCode);
            Toast.makeText(this, getString(R.string.location_zip_set), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.zip_not_found), Toast.LENGTH_SHORT).show();
        }
    }
}
