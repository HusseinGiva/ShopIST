package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
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
import java.util.List;
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;

public class PantryListActivity extends AppCompatActivity {

    private static String latitude = null;
    private static String longitude = null;
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        db.collection("PantryList").document(id)
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                            PantryList pantry = document.toObject(PantryList.class);
                            latitude = pantry.latitude;
                            longitude = pantry.longitude;
                            getSupportActionBar().setTitle(pantry.name);

                            db.collection("PantryItem").whereEqualTo("pantryId", id).get(source).addOnCompleteListener(task1 -> {

                                if (task1.isSuccessful()) {
                                    ArrayList<String> itemIds = new ArrayList<>();
                                    List<String> pantry_item_names = new ArrayList<>();
                                    List<Integer> pantry_item_quantities = new ArrayList<>();
                                    List<Integer> pantry_item_ideal_quantities = new ArrayList<>();
                                    for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                        PantryItem pi = document1.toObject(PantryItem.class);
                                        db.collection("Item").document(pi.itemId).get(source).addOnCompleteListener(task11 -> {
                                            if (task11.isSuccessful()) {
                                                DocumentSnapshot document11 = task11.getResult();
                                                if (document11.exists()) {
                                                    Item i = document11.toObject(Item.class);
                                                    pantry_item_names.add(i.users.get(mAuth.getCurrentUser().getUid()));
                                                    pantry_item_quantities.add(pi.quantity);
                                                    pantry_item_ideal_quantities.add(pi.idealQuantity);
                                                    itemIds.add(document11.getId());
                                                    list.invalidateViews();
                                                } else {
                                                    Log.d("TAG", "No such document");
                                                }


                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task11.getException());
                                            }
                                        });
                                    }
                                    PantryListAdapter a = new PantryListAdapter(PantryListActivity.this, pantry_item_names, pantry_item_quantities, pantry_item_ideal_quantities, itemIds, id);
                                    list.setAdapter(a);

                                } else {
                                    Log.d("TAG", "Error getting documents: ", task1.getException());
                                }

                            });


                        } else {
                            Log.d("TAG", "No such document");
                        }

                    } else {
                        Log.d("TAG", "Error getting documents: ", task.getException());
                    }
                });

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
}