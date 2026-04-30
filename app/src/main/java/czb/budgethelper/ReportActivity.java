package czb.budgethelper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import czb.budgethelper.db.AppDatabase;
import czb.budgethelper.db.BudgetDao;
import czb.budgethelper.db.SessionEntity;
import czb.budgethelper.db.SessionItemEntity;

public class ReportActivity extends AppCompatActivity {

    private RecyclerView sessionsRecyclerView;
    private TextView reportEmptyText;
    private Spinner filterSpinner;

    private BudgetDao dao;
    private List<SessionEntity> currentSessions = new ArrayList<>();
    private SessionAdapter sessionAdapter;
    private int currentFilter = 0;

    private static final int FILTER_TODAY = 0;
    private static final int FILTER_WEEK = 1;
    private static final int FILTER_MONTH = 2;
    private static final int FILTER_ALL = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        MaterialToolbar toolbar = findViewById(R.id.reportToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_all) {
                confirmClearAll();
                return true;
            }
            return false;
        });

        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
        reportEmptyText = findViewById(R.id.reportEmptyText);
        filterSpinner = findViewById(R.id.filterSpinner);

        dao = AppDatabase.getDatabase(this).budgetDao();

        sessionAdapter = new SessionAdapter();
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView.setAdapter(sessionAdapter);

        // Swipe left to delete individual session
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                SessionEntity deleted = currentSessions.get(pos);
                currentSessions.remove(pos);
                sessionAdapter.notifyItemRemoved(pos);
                updateEmptyState();
                new Thread(() -> {
                    dao.deleteItemsForSession(deleted.id);
                    dao.deleteSession(deleted.id);
                }).start();
                Snackbar.make(sessionsRecyclerView, "Session deleted", Snackbar.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(sessionsRecyclerView);

        String[] filters = {
                getString(R.string.filter_today),
                getString(R.string.filter_week),
                getString(R.string.filter_month),
                getString(R.string.filter_all)
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = position;
                loadSessions(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        filterSpinner.setSelection(FILTER_TODAY);

        findViewById(R.id.exportButton).setOnClickListener(v -> exportCsv());
    }

    private void loadSessions(int filter) {
        new Thread(() -> {
            long sinceMs = getSinceMs(filter);
            List<SessionEntity> sessions;
            if (filter == FILTER_ALL) {
                sessions = dao.getAllSessions();
            } else {
                sessions = dao.getSessionsFrom(sinceMs);
            }
            runOnUiThread(() -> {
                currentSessions = new ArrayList<>(sessions);
                sessionAdapter.notifyDataSetChanged();
                updateEmptyState();
            });
        }).start();
    }

    private void updateEmptyState() {
        boolean empty = currentSessions.isEmpty();
        sessionsRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        reportEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Sessions?")
                .setMessage("This will permanently delete all saved sessions and cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    new Thread(() -> {
                        dao.deleteAllSessionItems();
                        dao.deleteAllSessions();
                        runOnUiThread(() -> {
                            currentSessions.clear();
                            sessionAdapter.notifyDataSetChanged();
                            updateEmptyState();
                            Snackbar.make(sessionsRecyclerView, "All sessions cleared", Snackbar.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private long getSinceMs(int filter) {
        Calendar cal = Calendar.getInstance();
        switch (filter) {
            case FILTER_TODAY:
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            case FILTER_WEEK:
                cal.add(Calendar.DAY_OF_YEAR, -7);
                return cal.getTimeInMillis();
            case FILTER_MONTH:
                cal.add(Calendar.MONTH, -1);
                return cal.getTimeInMillis();
            default:
                return 0L;
        }
    }

    private void exportCsv() {
        if (currentSessions.isEmpty()) {
            Snackbar.make(sessionsRecyclerView, "No sessions to export", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            StringBuilder csv = new StringBuilder();
            csv.append("Date,Budget,Subtotal,Tax,Total,Remaining,ZIP,Items\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

            for (SessionEntity s : currentSessions) {
                List<SessionItemEntity> items = dao.getItemsForSession(s.id);

                StringBuilder itemsStr = new StringBuilder();
                for (SessionItemEntity item : items) {
                    if (itemsStr.length() > 0) itemsStr.append(" | ");
                    itemsStr.append(csvEscape(item.name)).append(":$")
                            .append(String.format(Locale.US, "%.2f", item.price));
                }

                double remaining = s.budget - s.total;
                csv.append(String.format(Locale.US,
                        "\"%s\",%.2f,%.2f,%.2f,%.2f,%.2f,\"%s\",\"%s\"\n",
                        sdf.format(new Date(s.savedAt)),
                        s.budget, s.subtotal, s.taxAmount, s.total, remaining,
                        csvEscape(s.zip != null ? s.zip : ""),
                        csvEscape(itemsStr.toString())));
            }

            try {
                File csvFile = new File(getCacheDir(), "budget_report.csv");
                FileWriter writer = new FileWriter(csvFile);
                writer.write(csv.toString());
                writer.flush();
                writer.close();

                Uri fileUri = FileProvider.getUriForFile(
                        this, "czb.budgethelper.fileprovider", csvFile);

                runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/csv");
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Budget Helper Report");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent,
                            getString(R.string.export_chooser_title)));
                });
            } catch (IOException e) {
                runOnUiThread(() ->
                        Snackbar.make(sessionsRecyclerView, "Export failed", Snackbar.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String csvEscape(String value) {
        return value.replace("\"", "\"\"");
    }

    // ── RecyclerView adapter ──────────────────────────────────────────────────

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

        private final SimpleDateFormat sdf =
                new SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US);

        @NonNull
        @Override
        public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_session, parent, false);
            return new SessionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
            SessionEntity s = currentSessions.get(position);
            double remaining = s.budget - s.total;

            holder.dateText.setText(sdf.format(new Date(s.savedAt)));
            holder.budgetText.setText(String.format(Locale.US, "$%.2f", s.budget));
            holder.totalText.setText(String.format(Locale.US, "$%.2f", s.total));
            holder.remainingText.setText(String.format(Locale.US, "$%.2f", remaining));
            holder.remainingText.setTextColor(getColor(
                    remaining < 0 ? R.color.remaining_negative : R.color.remaining_positive));

            new Thread(() -> {
                List<SessionItemEntity> items = dao.getItemsForSession(s.id);
                runOnUiThread(() -> {
                    int count = items.size();
                    holder.itemCountText.setText(count + (count == 1 ? " item" : " items"));

                    StringBuilder sb = new StringBuilder();
                    for (SessionItemEntity item : items) {
                        sb.append("• ").append(item.name)
                          .append("  —  $")
                          .append(String.format(Locale.US, "%.2f", item.price))
                          .append("\n");
                    }
                    holder.itemsText.setText(sb.toString().trim());
                });
            }).start();

            holder.itemView.setOnClickListener(v -> {
                boolean expanded = holder.itemsContainer.getVisibility() == View.VISIBLE;
                holder.itemsContainer.setVisibility(expanded ? View.GONE : View.VISIBLE);
            });
        }

        @Override
        public int getItemCount() {
            return currentSessions.size();
        }

        class SessionViewHolder extends RecyclerView.ViewHolder {
            TextView dateText, itemCountText, budgetText, totalText, remainingText, itemsText;
            LinearLayout itemsContainer;

            SessionViewHolder(@NonNull View itemView) {
                super(itemView);
                dateText = itemView.findViewById(R.id.sessionDateText);
                itemCountText = itemView.findViewById(R.id.sessionItemCountText);
                budgetText = itemView.findViewById(R.id.sessionBudgetText);
                totalText = itemView.findViewById(R.id.sessionTotalText);
                remainingText = itemView.findViewById(R.id.sessionRemainingText);
                itemsContainer = itemView.findViewById(R.id.sessionItemsContainer);
                itemsText = itemView.findViewById(R.id.sessionItemsText);
            }
        }
    }
}
