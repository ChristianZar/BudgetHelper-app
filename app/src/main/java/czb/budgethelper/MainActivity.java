package czb.budgethelper;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText budgetEditText;
    private EditText itemNameEditText;
    private EditText itemPriceEditText;
    private Button addButton;
    private TextView subtotalTextView;
    private TextView remainingTextView;
    private RecyclerView recyclerView;

    private ArrayList<BudgetItem> itemList;
    private BudgetItemAdapter adapter;

    private double subtotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        budgetEditText = findViewById(R.id.budgetEditText);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemPriceEditText = findViewById(R.id.itemPriceEditText);
        addButton = findViewById(R.id.addButton);
        subtotalTextView = findViewById(R.id.subtotalTextView);
        remainingTextView = findViewById(R.id.remainingTextView);
        recyclerView = findViewById(R.id.recyclerView);

        itemList = new ArrayList<>();
        adapter = new BudgetItemAdapter(itemList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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
        double remaining = budget - subtotal;

        subtotalTextView.setText(String.format(Locale.US, "Subtotal: $%.2f", subtotal));
        remainingTextView.setText(String.format(Locale.US, "Remaining: $%.2f", remaining));

        if (remaining < 0) {
            remainingTextView.setTextColor(Color.RED);
            subtotalTextView.setTextColor(Color.RED);
        } else {
            remainingTextView.setTextColor(Color.BLACK);
            subtotalTextView.setTextColor(Color.BLACK);
        }

        itemNameEditText.setText("");
        itemPriceEditText.setText("");
        itemNameEditText.requestFocus();
    }
}