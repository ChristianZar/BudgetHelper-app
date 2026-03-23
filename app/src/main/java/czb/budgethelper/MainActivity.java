package czb.budgethelper;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText budgetEditText;
    private EditText zipEditText;
    private EditText itemNameEditText;
    private EditText itemPriceEditText;
    private Button addButton;

    private TextView subtotalTextView;
    private TextView taxTextView;
    private TextView totalTextView;
    private TextView remainingTextView;

    private RecyclerView recyclerView;

    private ArrayList<BudgetItem> itemList;
    private BudgetItemAdapter adapter;

    private double subtotal = 0.0;
    private double taxRate = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        budgetEditText = findViewById(R.id.budgetEditText);
        zipEditText = findViewById(R.id.zipEditText);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemPriceEditText = findViewById(R.id.itemPriceEditText);
        addButton = findViewById(R.id.addButton);

        subtotalTextView = findViewById(R.id.subtotalTextView);
        taxTextView = findViewById(R.id.taxTextView);
        totalTextView = findViewById(R.id.totalTextView);
        remainingTextView = findViewById(R.id.remainingTextView);

        recyclerView = findViewById(R.id.recyclerView);

        itemList = new ArrayList<>();
        adapter = new BudgetItemAdapter(itemList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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

                        String budgetText = budgetEditText.getText().toString().trim();
                        double budget = 0.0;

                        if (!TextUtils.isEmpty(budgetText)) {
                            try {
                                budget = Double.parseDouble(budgetText);
                            } catch (NumberFormatException e) {
                                budget = 0.0;
                            }
                        }

                        updateTotals(budget);

                        Toast.makeText(MainActivity.this,
                                deletedItem.getName() + " removed",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        itemTouchHelper.attachToRecyclerView(recyclerView);

        addButton.setOnClickListener(v -> addItem());
    }

    private void addItem() {
        String budgetText = budgetEditText.getText().toString().trim();
        String itemName = itemNameEditText.getText().toString().trim();
        String itemPriceText = itemPriceEditText.getText().toString().trim();

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

        BudgetItem item = new BudgetItem(itemName, itemPrice);
        itemList.add(item);
        adapter.notifyItemInserted(itemList.size() - 1);

        subtotal += itemPrice;
        updateTotals(budget);

        itemNameEditText.setText("");
        itemPriceEditText.setText("");
        itemNameEditText.requestFocus();
    }

    private void updateTotals(double budget) {
        double taxAmount = subtotal * taxRate;
        double total = subtotal + taxAmount;
        double remaining = budget - total;

        subtotalTextView.setText(String.format(Locale.US, "Subtotal: $%.2f", subtotal));
        taxTextView.setText(String.format(Locale.US, "Tax: $%.2f", taxAmount));
        totalTextView.setText(String.format(Locale.US, "Total: $%.2f", total));
        remainingTextView.setText(String.format(Locale.US, "Remaining: $%.2f", remaining));

        if (remaining < 0) {
            remainingTextView.setTextColor(Color.RED);
            subtotalTextView.setTextColor(Color.RED);
            totalTextView.setTextColor(Color.RED);
        } else {
            remainingTextView.setTextColor(Color.BLACK);
            subtotalTextView.setTextColor(Color.BLACK);
            totalTextView.setTextColor(Color.BLACK);
        }
    }
}