package czb.budgethelper.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BudgetDao {

    @Insert
    long insertSession(SessionEntity session);

    @Insert
    void insertItem(SessionItemEntity item);

    @Query("SELECT * FROM sessions WHERE savedAt >= :sinceMs ORDER BY savedAt DESC")
    List<SessionEntity> getSessionsFrom(long sinceMs);

    @Query("SELECT * FROM sessions ORDER BY savedAt DESC")
    List<SessionEntity> getAllSessions();

    @Query("SELECT * FROM session_items WHERE sessionId = :sessionId")
    List<SessionItemEntity> getItemsForSession(long sessionId);
}
