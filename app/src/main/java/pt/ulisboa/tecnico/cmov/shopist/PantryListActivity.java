package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;

public class PantryListActivity extends AppCompatActivity {

    private static String latitude = null;
    private static String longitude = null;
    final List<String> itemIds = new ArrayList<>();
    final List<String> pantry_item_names = new ArrayList<>();
    final List<Integer> pantry_item_quantities = new ArrayList<>();
    final List<Integer> pantry_item_ideal_quantities = new ArrayList<>();
    final List<String> imageIds = new ArrayList<>();
    private final List<Data> data = new ArrayList<>();
    private ListView list;
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
        setContentView(R.layout.activity_pantry_list);
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

        list = findViewById(R.id.pantry_list);

        PantryListAdapter a = new PantryListAdapter(PantryListActivity.this, pantry_item_names, pantry_item_quantities, pantry_item_ideal_quantities, itemIds, imageIds, id, list);
        list.setAdapter(a);
    }

    @Override
    protected void onResume() {
        super.onResume();

        data.clear();

        int[] async_operations = {0};
        long[] server_n_items = {0};
        long[] real_n_items = {0};
        async_operations[0]++;
        db.collection("PantryList").document(id)
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            PantryList pantry = document.toObject(PantryList.class);
                            server_n_items[0] = Objects.requireNonNull(pantry).number_of_items;
                            latitude = pantry.latitude;
                            longitude = pantry.longitude;
                            Objects.requireNonNull(getSupportActionBar()).setTitle(pantry.name);

                            async_operations[0]++;
                            db.collection("PantryItem").whereEqualTo("pantryId", id).get(source).addOnCompleteListener(task1 -> {

                                if (task1.isSuccessful()) {
                                    for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                        PantryItem pi = document1.toObject(PantryItem.class);
                                        real_n_items[0]++;
                                        async_operations[0]++;
                                        db.collection("Item").document(pi.itemId).get(source).addOnCompleteListener(task11 -> {
                                            if (task11.isSuccessful()) {
                                                DocumentSnapshot document11 = task11.getResult();
                                                if (document11.exists()) {
                                                    Item i = document11.toObject(Item.class);

                                                    Data d = new Data();
                                                    if (Objects.requireNonNull(i).users.containsKey(Objects.requireNonNull(mAuth.getCurrentUser()).getUid()))
                                                        d.pantry_item_name = i.users.get(mAuth.getCurrentUser().getUid());
                                                    else {
                                                        Map.Entry<String, String> entry = i.users.entrySet().iterator().next();
                                                        d.pantry_item_name = entry.getValue();
                                                    }
                                                    d.pantry_item_quantity = pi.quantity;
                                                    d.pantry_item_ideal_quantity = pi.idealQuantity;
                                                    d.itemId = pi.itemId;
                                                    if (i.barcode.equals("")) d.imageId = pi.itemId;
                                                    else d.imageId = i.barcode;
                                                    data.add(d);

                                                    async_operations[0]--;
                                                }
                                            }
                                        });
                                    }

                                    async_operations[0]--;
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
                    if (server_n_items[0] != real_n_items[0]) {
                        db.collection("PantryList").document(id).update("number_of_items", real_n_items[0]);
                    }
                    sort();
                    list.invalidateViews();
                } else {
                    timerHandler.postDelayed(this, 100);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);

    }

    public void sort() {

        data.sort(Comparator.comparing(i -> i.pantry_item_name.toLowerCase()));

        itemIds.clear();
        pantry_item_names.clear();
        pantry_item_quantities.clear();
        pantry_item_ideal_quantities.clear();
        imageIds.clear();

        for (Data d : data) {
            itemIds.add(d.itemId);
            pantry_item_names.add(d.pantry_item_name);
            pantry_item_quantities.add(d.pantry_item_quantity);
            pantry_item_ideal_quantities.add(d.pantry_item_ideal_quantity);
            imageIds.add(d.imageId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_share_menu, menu);
        menu.findItem(R.id.resetList).setVisible(false);
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
                intent.putExtra("TYPE", getResources().getString(R.string.pantry));
                intent.putExtra("ID", id);
                intent.putExtra("MODE", "add");
                startActivity(intent);
                return true;
            case R.id.shareList:
                intent = new Intent(this, ShareListActivity.class);
                intent.putExtra("TYPE", "PANTRY");
                intent.putExtra("ID", id);
                startActivity(intent);
                return true;
            case R.id.editList:
                intent = new Intent(this, AddListActivity.class);
                intent.putExtra("TYPE", getResources().getString(R.string.pantry));
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
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private static class Data {
        String itemId;
        String pantry_item_name;
        Integer pantry_item_quantity;
        Integer pantry_item_ideal_quantity;
        String imageId;
    }
}