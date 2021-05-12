package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class StoreListActivity extends AppCompatActivity {

    private static String latitude = null;
    private static String longitude = null;
    TabLayout tabLayout;
    private String id;
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
        setContentView(R.layout.activity_store_list);
        Toolbar myToolbar = findViewById(R.id.listToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

        if (isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        id = getIntent().getStringExtra("ID");

        Bundle bundle = new Bundle();
        bundle.putString("ID", id);

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, StoreListFragment.class, bundle)
                .commit();

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText().equals(getResources().getString(R.string.items))) {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, StoreListFragment.class, bundle)
                            .commit();
                } else if (tab.getText().equals(getResources().getString(R.string.cart))) {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, CartFragment.class, bundle)
                            .commit();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        db.collection("StoreList").document(id)
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                            StoreList store = document.toObject(StoreList.class);
                            latitude = store.latitude;
                            longitude = store.longitude;
                            getSupportActionBar().setTitle(store.name);
                        } else {
                            Log.d("TAG", "No such document");
                        }

                    } else {
                        Log.d("TAG", "Error getting documents: ", task.getException());
                    }
                });
    }

    public void goToStore() {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        TabLayout.Tab tab = tabLayout.getTabAt(0);
        assert tab != null;
        tab.select();
        Bundle bundle = new Bundle();
        bundle.putString("ID", id);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, StoreListFragment.class, bundle)
                .commit();
    }

    public void goToCart() {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        TabLayout.Tab tab = tabLayout.getTabAt(1);
        assert tab != null;
        tab.select();
        Bundle bundle = new Bundle();
        bundle.putString("ID", id);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, CartFragment.class, bundle)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_share_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getStringExtra("SENDER") != null && getIntent().getStringExtra("SENDER").equals("start")) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();    //Call the back button's method
                return true;
            case R.id.addProduct:
                Intent intent = new Intent(this, AddItemActivity.class);
                intent.putExtra("TYPE", getResources().getString(R.string.store));
                intent.putExtra("ID", id);
                intent.putExtra("MODE", "add");
                startActivity(intent);
                return true;
            case R.id.shareList:
                intent = new Intent(this, ShareListActivity.class);
                intent.putExtra("TYPE", "STORE");
                intent.putExtra("ID", id);
                startActivity(intent);
                return true;
            case R.id.editList:
                intent = new Intent(this, AddListActivity.class);
                intent.putExtra("TYPE", getResources().getString(R.string.store));
                intent.putExtra("ID", id);
                intent.putExtra("MODE", "update");
                startActivity(intent);
                return true;
            case R.id.locationList:
                if (latitude == null || longitude == null) {
                    Toast.makeText(this, R.string.listDoesntHaveLocationSet, Toast.LENGTH_LONG).show();
                    return false;
                }
                intent = new Intent(this, ListLocationActivity.class);
                intent.putExtra("LATITUDE", latitude);
                intent.putExtra("LONGITUDE", longitude);
                startActivity(intent);
                return true;
            case R.id.resetList:
                reset();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void reset() {
        // Item id -> quantity needed
        Map<String, Integer> m1 = new HashMap<>();
        // Item id -> StoreItem id
        Map<String, String> m2 = new HashMap<>();
        List<PantryItem> pis = new ArrayList<>();

        int[] async_operations = {0};

        async_operations[0]++;
        db.collection("StoreItem").whereEqualTo("storeId", id).get(source)
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            for(QueryDocumentSnapshot document_1 : task.getResult()) {
                                StoreItem si = document_1.toObject(StoreItem.class);
                                m1.put(si.itemId, 0);
                                m2.put(si.itemId, document_1.getId());
                            }

                            async_operations[0]++;
                            db.collection("PantryList")
                                    .whereArrayContains("users", mAuth.getCurrentUser().getUid()).get(source)
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if(task.isSuccessful()) {
                                                for(QueryDocumentSnapshot document_2 : task.getResult()) {
                                                    PantryList p = document_2.toObject(PantryList.class);
                                                    async_operations[0]++;
                                                    db.collection("PantryItem")
                                                            .whereEqualTo("pantryId", document_2.getId())
                                                            .get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                            if(task.isSuccessful()) {
                                                                for(QueryDocumentSnapshot document_3 : task.getResult()) {
                                                                    PantryItem pi = document_3.toObject(PantryItem.class);
                                                                    pis.add(pi);
                                                                }
                                                                async_operations[0]--;
                                                            }
                                                        }
                                                    });
                                                }

                                                async_operations[0]--;
                                            }
                                        }
                                    });

                            async_operations[0]--;
                        }
                    }
                });

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (async_operations[0] == 0) {
                    for(PantryItem pi : pis) {
                        if(m1.containsKey(pi.itemId)) {
                            int q = m1.get(pi.itemId);
                            m1.put(pi.itemId, q + pi.idealQuantity - pi.quantity);
                        }
                    }
                    int[] n_items = {0};
                    for(Map.Entry<String, Integer> kvp : m1.entrySet()) {
                        int q = kvp.getValue();
                        String siId = m2.get(kvp.getKey());

                        if(q > 0) n_items[0]++;

                        async_operations[0]++;
                        db.collection("StoreItem").document(siId).update("quantity", q)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()) async_operations[0]--;
                                    }
                                });
                    }

                    Handler timerHandler = new Handler();
                    Runnable timerRunnable = new Runnable() {

                        @Override
                        public void run() {
                            if (async_operations[0] == 0) {
                                db.collection("StoreList").document(id).update("number_of_items", n_items[0])
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                goToStore();
                                            }
                                        });
                            } else {
                                timerHandler.postDelayed(this, 100);
                            }
                        }
                    };
                    timerHandler.postDelayed(timerRunnable, 0);
                } else {
                    timerHandler.postDelayed(this, 100);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }
}
