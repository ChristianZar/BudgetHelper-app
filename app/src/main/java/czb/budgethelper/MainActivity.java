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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getDatabase(this).budgetDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == R.id.action_reports) {
                startActivity(new Intent(this, ReportActivity.class));
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
}
