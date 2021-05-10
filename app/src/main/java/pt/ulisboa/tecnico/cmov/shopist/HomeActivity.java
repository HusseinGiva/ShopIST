package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;


public class HomeActivity extends AppCompatActivity {

    private String typeSelected;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences sharedPref = newBase.getSharedPreferences("language", Context.MODE_PRIVATE);
        String language = sharedPref.getString("language", "en");
        if (language.equals("auto")) {
            language = Locale.getDefault().getLanguage();
        }
        super.attachBaseContext(ContextUtils.updateLocale(newBase, language));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        typeSelected = this.getResources().getString(R.string.pantry);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, ListFragment.class, null)
                    .commit();
        }
        //language = ContextUtils.getSystemLocale(getResources().getConfiguration()).getLanguage();
        //Toast.makeText(this, language, Toast.LENGTH_LONG).show();
        Toolbar myToolbar = findViewById(R.id.homeToolbar);
        setSupportActionBar(myToolbar);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.lists:
                    myToolbar.getMenu().findItem(R.id.addList).setVisible(true);
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.fragment_container_view, ListFragment.class, null)
                            .setReorderingAllowed(true)
                            .commit();
                    break;
                case R.id.profile:
                    myToolbar.getMenu().findItem(R.id.addList).setVisible(false);
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.fragment_container_view, ProfileFragment.class, null)
                            .setReorderingAllowed(true)
                            .commit();
                    break;
            }
            return true;
        });
    }

    public String getTypeSelected() {
        return this.typeSelected;
    }

    public void setTypeSelected(String type) {
        this.typeSelected = type;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.addlist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addList) {
            Intent intent = new Intent(this, AddListActivity.class);
            intent.putExtra("TYPE", this.typeSelected);
            intent.putExtra("MODE", "add");
            startActivity(intent);
            return true;
        }// If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

}