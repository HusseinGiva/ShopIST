package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;

public class PantryListActivity extends AppCompatActivity {

    private ListView list;
    private String id;
    private FirebaseFirestore db;

    private static Double latitude = null;
    private static Double longitude = null;

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

        db = FirebaseFirestore.getInstance();

        id = getIntent().getStringExtra("ID");

        list = findViewById(R.id.pantry_list);
    }

    @Override
    protected void onResume() {
        super.onResume();

        db.collection("PantryList").document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                                PantryList pantry = document.toObject(PantryList.class);
                                latitude = pantry.latitude;
                                longitude = pantry.longitude;
                                getSupportActionBar().setTitle(pantry.name);

                                db.collection("PantryItem").whereEqualTo("pantryId", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                        if (task.isSuccessful()) {
                                            ArrayList<String> itemIds = new ArrayList<String>();
                                            List<String> pantry_item_names = new ArrayList<>();
                                            List<Integer> pantry_item_quantities = new ArrayList<>();
                                            PantryListAdapter a = new PantryListAdapter(PantryListActivity.this, pantry_item_names, pantry_item_quantities);
                                            list.setAdapter(a);
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                PantryItem pi = document.toObject(PantryItem.class);
                                                itemIds.add(pi.itemId);
                                                db.collection("Item").document(pi.itemId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            DocumentSnapshot document = task.getResult();
                                                            if (document.exists()) {
                                                                Item i = document.toObject(Item.class);
                                                                pantry_item_names.add(i.name);
                                                                pantry_item_quantities.add(pi.idealQuantity);
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
                intent.putExtra("TYPE", getResources().getString(R.string.pantry));
                intent.putExtra("ID", id);
                startActivity(intent);
                return true;
            case R.id.manualEntry:
                intent = new Intent(this, ShareListActivity.class);
                intent.putExtra("TYPE", getResources().getString(R.string.pantry));
                intent.putExtra("ID", id);
                startActivity(intent);
                return true;
            case R.id.locationList:
                if (latitude == null || longitude == null) {
                    Toast.makeText(this, R.string.listDoesntHaveLocationSet, Toast.LENGTH_LONG).show();
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