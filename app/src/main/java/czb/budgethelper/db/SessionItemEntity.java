package czb.budgethelper.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "session_items")
public class SessionItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;
    public String name;
    public double price;

    public SessionItemEntity(long sessionId, String name, double price) {
        this.sessionId = sessionId;
        this.name = name;
        this.price = price;
    }
}
