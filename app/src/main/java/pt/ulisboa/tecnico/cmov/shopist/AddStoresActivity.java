package pt.ulisboa.tecnico.cmov.shopist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Objects;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class AddStoresActivity extends AppCompatActivity implements StoresFragment.OnListFragmentInteractionListener {

    ArrayList<StoreViewAddItem> stores = new ArrayList<>();
    private RecyclerView.Adapter recyclerViewAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;

    public static boolean isConnected(Context getApplicationContext) {
        boolean status = false;

        ConnectivityManager cm = (ConnectivityManager) getApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null) {
            // connected to the internet
            status = true;
        }


        return status;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stores);
        Toolbar myToolbar = findViewById(R.id.addStoresToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        if (isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;
        if (recyclerViewAdapter == null) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.storesFragment);
            assert currentFragment != null;
            recyclerViewAdapter = ((RecyclerView) currentFragment.requireView()).getAdapter();
        }
        StoreContent.emptyList();
        recyclerViewAdapter.notifyDataSetChanged();
        if (getIntent().getStringExtra("MODE").equals("read") || getIntent().getStringExtra("MODE").equals("update")) {
            if (getIntent().getStringExtra("MODE").equals("read")) {
                StoresRecyclerViewAdapter adapt = (StoresRecyclerViewAdapter) recyclerViewAdapter;
                adapt.isRead = true;
                getSupportActionBar().setTitle(R.string.viewStores);
            }
            if (getIntent().getParcelableArrayListExtra("STORES") != null) {
                stores = getIntent().getParcelableArrayListExtra("STORES");
                if (!stores.isEmpty()) {
                    for (StoreViewAddItem item : stores) {
                        StoreContent.addItem(item);
                        recyclerViewAdapter.notifyItemInserted(0);
                    }
                }
            } else {
                String id = getIntent().getStringExtra("ID");
                db = FirebaseFirestore.getInstance();
                mAuth = FirebaseAuth.getInstance();
                db.collection("Item")
                        .document(id)
                        .get(source)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                Item item = document.toObject(Item.class);
                                for (String storeId : Objects.requireNonNull(item).stores.keySet()) {
                                    db.collection("StoreList")
                                            .document(storeId)
                                            .get(source)
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    DocumentSnapshot document2 = task2.getResult();
                                                    StoreList item2 = document2.toObject(StoreList.class);
                                                    if (Objects.requireNonNull(item2).users.contains(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())) {
                                                        StoreViewAddItem storeViewAddItem = new StoreViewAddItem(storeId, item2.name, item.stores.get(storeId), true);
                                                        storeViewAddItem.latitude = item2.latitude;
                                                        storeViewAddItem.longitude = item2.longitude;
                                                        stores.add(storeViewAddItem);
                                                        StoreContent.addItem(storeViewAddItem);
                                                        recyclerViewAdapter.notifyItemInserted(0);
                                                    } else if (item2.latitude != null && item2.longitude != null) {
                                                        db.collection("StoreList")
                                                                .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                                                .get(source)
                                                                .addOnCompleteListener(task3 -> {
                                                                    if (task3.isSuccessful()) {
                                                                        for (QueryDocumentSnapshot document3 : task3.getResult()) {
                                                                            StoreList item3 = document3.toObject(StoreList.class);
                                                                            if (item3.latitude != null && item3.longitude != null) {
                                                                                float[] results = new float[1];
                                                                                Location.distanceBetween(Double.parseDouble(item3.latitude), Double.parseDouble(item3.longitude),
                                                                                        Double.parseDouble(item2.latitude), Double.parseDouble(item2.longitude),
                                                                                        results);
                                                                                //Less than 20 meters
                                                                                if (results[0] < 20f) {
                                                                                    StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document3.getId(), item3.name, item.stores.get(storeId), true);
                                                                                    storeViewAddItem.latitude = item3.latitude;
                                                                                    storeViewAddItem.longitude = item3.longitude;
                                                                                    stores.add(storeViewAddItem);
                                                                                    StoreContent.addItem(storeViewAddItem);
                                                                                    recyclerViewAdapter.notifyItemInserted(0);
                                                                                }
                                                                            }


                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            }
        } else if (getIntent().getStringExtra("MODE").equals("add")) {
            stores = getIntent().getParcelableArrayListExtra("STORES");
            if (!stores.isEmpty()) {
                for (StoreViewAddItem item : stores) {
                    StoreContent.addItem(item);
                    recyclerViewAdapter.notifyItemInserted(0);
                }
            }
        }
    }

    @Override
    public void onListFragmentInteraction(StoreViewAddItem mItem) {
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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();    //Call the back button's method
            return true;
        }// If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("STORES", stores);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}