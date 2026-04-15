package czb.budgethelper;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

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

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_graph);

        barChart = findViewById(R.id.barChart);
        highestMonthText = findViewById(R.id.highestMonthText);
        lowestMonthText = findViewById(R.id.lowestMonthText);
        averageMonthText = findViewById(R.id.averageMonthText);

        db = AppDatabase.getDatabase(this);

        loadMonthlyGraph();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMonthlyGraph();
    }

    private void loadMonthlyGraph() {
        new Thread(() -> {
            List<SessionEntity> sessions = db.budgetDao().getAllSessions();

            float[] monthTotals = new float[6];
            Calendar now = Calendar.getInstance();

            for (SessionEntity s : sessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(s.savedAt);

                int diff = (now.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12
                        + (now.get(Calendar.MONTH) - cal.get(Calendar.MONTH));

                if (diff >= 0 && diff < 6) {
                    int index = 5 - diff;
                    monthTotals[index] += (float) s.total;
                }
            }

            ArrayList<BarEntry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -5);

            SimpleDateFormat format = new SimpleDateFormat("MMM", Locale.getDefault());

            float highest = -1;
            float lowest = Float.MAX_VALUE;
            float totalAll = 0;
            String highMonth = "";
            String lowMonth = "";

            for (int i = 0; i < 6; i++) {
                String label = format.format(cal.getTime());
                float value = monthTotals[i];

                entries.add(new BarEntry(i, value));
                labels.add(label);

                if (value > highest) {
                    highest = value;
                    highMonth = label;
                }

                if (value < lowest) {
                    lowest = value;
                    lowMonth = label;
                }

                totalAll += value;
                cal.add(Calendar.MONTH, 1);
            }

            float avg = totalAll / 6;
            String finalHighMonth = highMonth;
            String finalLowMonth = lowMonth;
            float finalHighest = highest;
            float finalLowest = lowest;
            float finalAvg = avg;

            runOnUiThread(() -> {
                barChart.clear();

                BarDataSet dataSet = new BarDataSet(entries, "Monthly Spending");
                dataSet.setDrawValues(true);
                dataSet.setValueTextSize(12f);
                dataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value >= 1000) {
                            return String.format(Locale.getDefault(), "%.1fk", value / 1000);
                        } else {
                            return String.format(Locale.getDefault(), "%.0f", value);
                        }
                    }
                });

                BarData data = new BarData(dataSet);

                barChart.setData(data);
                barChart.setDrawValueAboveBar(true);
                barChart.getDescription().setEnabled(false);
                barChart.getAxisLeft().setAxisMinimum(0f);
                barChart.getAxisRight().setEnabled(false);
                barChart.animateY(1000);

                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setGranularity(1f);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                barChart.invalidate();

                highestMonthText.setText("Highest: " + finalHighMonth + " ($" + finalHighest + ")");
                lowestMonthText.setText("Lowest: " + finalLowMonth + " ($" + finalLowest + ")");
                averageMonthText.setText("Average: $" + finalAvg);
            });
        }).start();
    }
}