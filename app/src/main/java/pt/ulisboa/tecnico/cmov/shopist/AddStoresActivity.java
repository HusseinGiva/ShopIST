package pt.ulisboa.tecnico.cmov.shopist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AddStoresActivity extends AppCompatActivity implements StoresFragment.OnListFragmentInteractionListener {

    private RecyclerView.Adapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    ArrayList<StoreViewAddItem> stores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stores);
        Toolbar myToolbar = findViewById(R.id.addStoresToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        stores = getIntent().getParcelableArrayListExtra("STORES");
        if (recyclerViewAdapter == null) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.storesFragment);
            recyclerView = (RecyclerView) currentFragment.getView();
            recyclerViewAdapter = ((RecyclerView) currentFragment.getView()).getAdapter();
        }
        StoreContent.emptyList();
        recyclerViewAdapter.notifyDataSetChanged();
        if (!stores.isEmpty()) {
            for (StoreViewAddItem item : stores) {
                StoreContent.addItem(item);
                recyclerViewAdapter.notifyItemInserted(0);
            }
        }
    }

    @Override
    public void onListFragmentInteraction(StoreViewAddItem mItem) {
        //Intent intent = new Intent(this, ViewPicturesActivity.class);
        //intent.putExtra("URI", mItem.toString());
        //startActivity(intent);
        Toast.makeText(this, mItem.name, Toast.LENGTH_SHORT).show();
        mItem.isChecked = !mItem.isChecked;
    }

    @Override
    public void onListFragmentPriceInteraction(StoreViewAddItem mItem, Editable s) {
        if (!s.toString().equals("")) {
            mItem.price = Float.parseFloat(s.toString());
        } else {
            mItem.price = 0;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();    //Call the back button's method
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("STORES", stores);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}