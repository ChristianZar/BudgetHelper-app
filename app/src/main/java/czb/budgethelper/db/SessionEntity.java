package czb.budgethelper.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long savedAt;      // epoch milliseconds
    public double budget;
    public String zip;
    public double taxRate;
    public double subtotal;
    public double taxAmount;
    public double total;

    public SessionEntity(long savedAt, double budget, String zip,
                         double taxRate, double subtotal, double taxAmount, double total) {
        this.savedAt = savedAt;
        this.budget = budget;
        this.zip = zip;
        this.taxRate = taxRate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.total = total;
    }
}
