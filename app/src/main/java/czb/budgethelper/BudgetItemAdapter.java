package czb.budgethelper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class BudgetItemAdapter extends RecyclerView.Adapter<BudgetItemAdapter.BudgetViewHolder> {

    private ArrayList<BudgetItem> itemList;

    public BudgetItemAdapter(ArrayList<BudgetItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetItem item = itemList.get(position);

        holder.itemNameText.setText(item.getName());
        holder.itemPriceText.setText(
                String.format(Locale.US, "$%.2f", item.getPrice())
        );
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public BudgetItem getItemAt(int position) {
        return itemList.get(position);
    }

    public void removeItem(int position) {
        itemList.remove(position);
        notifyItemRemoved(position);
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {

        TextView itemNameText;
        TextView itemPriceText;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);

            itemNameText = itemView.findViewById(R.id.itemNameText);
            itemPriceText = itemView.findViewById(R.id.itemPriceText);
        }
    }
}