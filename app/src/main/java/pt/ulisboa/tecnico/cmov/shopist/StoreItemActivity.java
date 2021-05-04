package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class StoreItemActivity extends AppCompatActivity {

    public String id;
    public String storeId;
    public EditText itemStoreQuantity;
    public EditText itemCartQuantity;
    public EditText barcodeNumber;
    public EditText price;
    public Item item;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_item);
        Toolbar myToolbar = findViewById(R.id.storeItemToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        id = getIntent().getStringExtra("ID");
        storeId = getIntent().getStringExtra("StoreId");
        itemStoreQuantity = findViewById(R.id.itemStoreQuantity);
        itemCartQuantity = findViewById(R.id.itemCartQuantity);
        SharedPreferences sharedPref = getSharedPreferences("language", Context.MODE_PRIVATE);
        String language = sharedPref.getString("language", "en");
        if (language.equals("pt")) {
            TextView textViewCartQuantity = findViewById(R.id.textView2);
            textViewCartQuantity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        }
        barcodeNumber = findViewById(R.id.barcodeNumberStoreItem);
        price = findViewById(R.id.priceStoreItem);
        if (isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        db.collection("Item").document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                item = document.toObject(Item.class);
                                getSupportActionBar().setTitle(item.users.get(mAuth.getCurrentUser().getUid()));
                                barcodeNumber.setText(item.barcode);
                                for (String s : item.stores.keySet()) {
                                    if (storeId.equals(s)) {
                                        price.setText(String.valueOf(item.stores.get(s)));
                                    } else {
                                        db.collection("StoreList").document(s).get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        StoreList sl = document.toObject(StoreList.class);
                                                        float[] results = new float[1];
                                                        db.collection("StoreList").document(id).get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    DocumentSnapshot document = task.getResult();
                                                                    if (document.exists()) {
                                                                        StoreList storeList = document.toObject(StoreList.class);
                                                                        Location.distanceBetween(Double.parseDouble(storeList.latitude), Double.parseDouble(storeList.longitude),
                                                                                Double.parseDouble(sl.latitude), Double.parseDouble(sl.longitude),
                                                                                results);
                                                                        //Less than 20 meters
                                                                        if (results[0] < 20f) {
                                                                            price.setText(String.valueOf(item.stores.get(s)));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                                db.collection("StoreItem").whereEqualTo("itemId", id).whereEqualTo("storeId", storeId).get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task2) {
                                        if (task2.isSuccessful()) {
                                            for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                StoreItem pi = document2.toObject(StoreItem.class);
                                                itemStoreQuantity.setText(String.valueOf(pi.quantity));
                                                itemCartQuantity.setText(String.valueOf(pi.cartQuantity));
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_share_flag, menu);
        return true;
    }

    public void onClickViewImages(View view) {
        Intent intent = new Intent(this, AddPicturesActivity.class);
        intent.putExtra("MODE", "read");
        if (barcodeNumber.getText().toString().equals("")) {
            intent.putExtra("ID", id);
        } else {
            intent.putExtra("ID", barcodeNumber.getText().toString());
        }
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shareItem:
                /*Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "This is the text that will be shared.");
                startActivity(Intent.createChooser(sharingIntent,"Share using"));

                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                Uri screenshotUri = Uri.parse(path);

                sharingIntent.setType("image/png");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                startActivity(Intent.createChooser(sharingIntent, "Share image using"));

                startActivity(Intent.createChooser(share, "Title of the dialog the system will open"));
                return true;*/
            case android.R.id.home:
                onBackPressed();    //Call the back button's method
                return true;
            case R.id.editItem:
                Intent intent = new Intent(this, AddItemActivity.class);
                intent.putExtra("TYPE", getResources().getString(R.string.store));
                intent.putExtra("ID", storeId);
                intent.putExtra("ItemId", id);
                intent.putExtra("MODE", "update");
                startActivity(intent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public static boolean isConnected(Context getApplicationContext) {
        boolean status = false;

        ConnectivityManager cm = (ConnectivityManager) getApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null) {
            // connected to the internet
            status = true;
        }


        return status;
    }
}