package czb.budgethelper;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import czb.budgethelper.db.AppDatabase;
import czb.budgethelper.db.SessionEntity;

public class MonthlyGraphActivity extends AppCompatActivity {

    private BarChart barChart;
    private TextView highestMonthText;
    private TextView lowestMonthText;
    private TextView averageMonthText;
    private Spinner rangeSpinner;

    private AppDatabase db;

    // -1 = Year to Date
    private static final int[] RANGE_MONTHS = {3, 6, 9, 12, -1};
    private int selectedRangeIndex = 1; // default: 6 months
    private boolean spinnerReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_graph);

        MaterialToolbar toolbar = findViewById(R.id.graphToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        barChart = findViewById(R.id.barChart);
        highestMonthText = findViewById(R.id.highestMonthText);
        lowestMonthText = findViewById(R.id.lowestMonthText);
        averageMonthText = findViewById(R.id.averageMonthText);
        rangeSpinner = findViewById(R.id.rangeSpinner);

        db = AppDatabase.getDatabase(this);

        String[] labels = {"3 Months", "6 Months", "9 Months", "12 Months", "Year to Date"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rangeSpinner.setAdapter(adapter);
        rangeSpinner.setSelection(selectedRangeIndex);

        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRangeIndex = position;
                if (spinnerReady) loadMonthlyGraph();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerReady = true;
        loadMonthlyGraph();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (spinnerReady) loadMonthlyGraph();
    }

    private void loadMonthlyGraph() {
        int rangeValue = RANGE_MONTHS[selectedRangeIndex];
        boolean isYTD = (rangeValue == -1);

        final int months;
        if (isYTD) {
            int m = Calendar.getInstance().get(Calendar.MONTH) + 1;
            months = Math.max(m, 1);
        } else {
            months = rangeValue;
        }

        new Thread(() -> {
            List<SessionEntity> sessions = db.budgetDao().getAllSessions();

            float[] monthTotals = new float[months];
            Calendar now = Calendar.getInstance();

            for (SessionEntity s : sessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(s.savedAt);

                int diff = (now.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12
                        + (now.get(Calendar.MONTH) - cal.get(Calendar.MONTH));

                if (diff >= 0 && diff < months) {
                    monthTotals[months - 1 - diff] += (float) s.total;
                }
            }

            ArrayList<BarEntry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -(months - 1));

            SimpleDateFormat fmt = new SimpleDateFormat("MMM", Locale.getDefault());

            float highest = 0;
            float lowest = Float.MAX_VALUE;
            float totalAll = 0;
            String highMonth = "—";
            String lowMonth = "—";
            boolean hasSpending = false;

            for (int i = 0; i < months; i++) {
                String label = fmt.format(cal.getTime());
                float value = monthTotals[i];

                entries.add(new BarEntry(i, value));
                labels.add(label);

                if (value > highest) { highest = value; highMonth = label; }
                if (value > 0 && value < lowest) { lowest = value; lowMonth = label; hasSpending = true; }

                totalAll += value;
                cal.add(Calendar.MONTH, 1);
            }

            if (!hasSpending) lowest = 0;

            final float fHighest = highest;
            final float fLowest = lowest;
            final float fAvg = totalAll / months;
            final String fHighMonth = highMonth;
            final String fLowMonth = lowMonth;
            final boolean fHasSpending = hasSpending;

            runOnUiThread(() -> {
                barChart.clear();

                BarDataSet dataSet = new BarDataSet(entries, "Monthly Spending");
                dataSet.setDrawValues(true);
                dataSet.setValueTextSize(11f);
                dataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value == 0) return "";
                        if (value >= 1000) return String.format(Locale.US, "$%.1fk", value / 1000);
                        return String.format(Locale.US, "$%.0f", value);
                    }
                });

                BarData data = new BarData(dataSet);
                barChart.setData(data);
                barChart.setDrawValueAboveBar(true);
                barChart.getDescription().setEnabled(false);
                barChart.getAxisLeft().setAxisMinimum(0f);
                barChart.getAxisRight().setEnabled(false);
                barChart.animateY(800);

                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setGranularity(1f);
                xAxis.setLabelCount(months);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                barChart.invalidate();

                highestMonthText.setText(fHighest > 0
                        ? String.format(Locale.US, "%s  $%.2f", fHighMonth, fHighest)
                        : "No data");
                lowestMonthText.setText(fHasSpending
                        ? String.format(Locale.US, "%s  $%.2f", fLowMonth, fLowest)
                        : "No data");
                averageMonthText.setText(String.format(Locale.US, "$%.2f", fAvg));
            });
        }).start();
    }
}
