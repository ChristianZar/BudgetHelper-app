package czb.budgethelper;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class DescriptionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);

        MaterialToolbar toolbar = findViewById(R.id.descriptionToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }
}
