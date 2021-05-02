package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class StoreListActivity extends AppCompatActivity {

    private ListView list;
    private String id;
    private FirebaseFirestore db;

    private static Double latitude = null;
    private static Double longitude = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_list);
        Toolbar myToolbar = findViewById(R.id.listToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();

        id = getIntent().getStringExtra("ID");

        list = findViewById(R.id.store_list);
    }

    @Override
    protected void onResume() {
        super.onResume();

        db.collection("StoreList").document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                                StoreList store = document.toObject(StoreList.class);
                                latitude = store.latitude;
                                longitude = store.longitude;
                                getSupportActionBar().setTitle(store.name);

                                db.collection("StoreItem").whereEqualTo("storeId", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                        if (task.isSuccessful()) {
                                            ArrayList<String> itemIds = new ArrayList<String>();
                                            List<String> store_item_names = new ArrayList<>();
                                            List<Integer> store_item_quantities = new ArrayList<>();
                                            List<Float> item_prices = new ArrayList<>();
                                            StoreListAdapter a = new StoreListAdapter(StoreListActivity.this, store_item_names, store_item_quantities, item_prices);
                                            list.setAdapter(a);
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                StoreItem si = document.toObject(StoreItem.class);
                                                itemIds.add(si.itemId);
                                                db.collection("Item").document(si.itemId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            DocumentSnapshot document = task.getResult();
                                                            if (document.exists()) {
                                                                Item i = document.toObject(Item.class);
                                                                store_item_names.add(i.name);
                                                                store_item_quantities.add(si.quantity);
                                                                item_prices.add(si.price);
                                                                list.invalidateViews();
                                                            } else {
                                                                Log.d("TAG", "No such document");
                                                            }


                                                        } else {
                                                            Log.d("TAG", "Error getting documents: ", task.getException());
                                                        }
                                                    }
                                                });
                                            }

                                        } else {
                                            Log.d("TAG", "Error getting documents: ", task.getException());
                                        }

                                    }
                                });


                            } else {
                                Log.d("TAG", "No such document");
                            }

                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
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
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
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
                intent.putExtra("TYPE", "STORE");
                intent.putExtra("ID", id);
                startActivity(intent);
                return true;
            case R.id.manualEntry:
                intent = new Intent(this, ShareListActivity.class);
                intent.putExtra("TYPE", "STORE");
                intent.putExtra("ID", id);
                startActivity(intent);
                return true;
            case R.id.locationList:
                if(latitude == null || longitude == null) {
                    Toast.makeText(this, "The list doesn't have a location set.", Toast.LENGTH_LONG).show();
                    return false;
                }
                intent = new Intent(this, ListLocationActivity.class);
                intent.putExtra("LATITUDE", String.valueOf(latitude));
                intent.putExtra("LONGITUDE", String.valueOf(longitude));
                startActivity(intent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
