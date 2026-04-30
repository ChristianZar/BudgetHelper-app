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

    @Query("UPDATE sessions SET savedAt=:savedAt, budget=:budget, zip=:zip, taxRate=:taxRate, subtotal=:subtotal, taxAmount=:taxAmount, total=:total WHERE id=:id")
    void updateSession(long id, long savedAt, double budget, String zip, double taxRate, double subtotal, double taxAmount, double total);

    @Query("DELETE FROM session_items WHERE sessionId=:sessionId")
    void deleteItemsForSession(long sessionId);

    @Query("DELETE FROM sessions WHERE id=:id")
    void deleteSession(long id);

    @Query("DELETE FROM sessions")
    void deleteAllSessions();

    @Query("DELETE FROM session_items")
    void deleteAllSessionItems();

    @Query("SELECT * FROM sessions WHERE savedAt >= :sinceMs ORDER BY savedAt DESC")
    List<SessionEntity> getSessionsFrom(long sinceMs);

    @Query("SELECT * FROM sessions ORDER BY savedAt DESC")
    List<SessionEntity> getAllSessions();

    @Query("SELECT * FROM session_items WHERE sessionId = :sessionId")
    List<SessionItemEntity> getItemsForSession(long sessionId);
}
