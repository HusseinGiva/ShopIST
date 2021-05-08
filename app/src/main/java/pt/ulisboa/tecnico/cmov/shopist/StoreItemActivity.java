package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

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
    public ActivityResultLauncher<Intent> editResultLauncher;
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

        editResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent intent2 = getIntent();
                    finish();
                    startActivity(intent2);
                });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        db.collection("Item").document(id)
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            item = document.toObject(Item.class);
                            getSupportActionBar().setTitle(item.users.get(mAuth.getCurrentUser().getUid()));
                            barcodeNumber.setText(item.barcode);
                            for (String s : item.stores.keySet()) {
                                if (storeId.equals(s)) {
                                    if (item.stores.get(s) != 0) {
                                        price.setText(String.valueOf(item.stores.get(s)));
                                    }
                                } else {
                                    db.collection("StoreList").document(s).get(source).addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            DocumentSnapshot document1 = task1.getResult();
                                            if (document1.exists()) {
                                                StoreList sl = document1.toObject(StoreList.class);
                                                float[] results = new float[1];
                                                db.collection("StoreList").document(id).get(source).addOnCompleteListener(task11 -> {
                                                    if (task11.isSuccessful()) {
                                                        DocumentSnapshot document11 = task11.getResult();
                                                        if (document11.exists()) {
                                                            StoreList storeList = document11.toObject(StoreList.class);
                                                            Location.distanceBetween(Double.parseDouble(storeList.latitude), Double.parseDouble(storeList.longitude),
                                                                    Double.parseDouble(sl.latitude), Double.parseDouble(sl.longitude),
                                                                    results);
                                                            //Less than 20 meters
                                                            if (results[0] < 20f) {
                                                                price.setText(String.valueOf(item.stores.get(s)));
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }
                            db.collection("StoreItem").whereEqualTo("itemId", id).whereEqualTo("storeId", storeId).get(source).addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                        StoreItem pi = document2.toObject(StoreItem.class);
                                        itemStoreQuantity.setText(String.valueOf(pi.quantity));
                                        itemCartQuantity.setText(String.valueOf(pi.cartQuantity));
                                    }
                                }
                            });
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
    public boolean onOptionsItemSelected(MenuItem item2) {
        switch (item2.getItemId()) {
            case R.id.shareItem:
                db.collection("StoreList").document(storeId).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            StoreList storeList = document.toObject(StoreList.class);
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            String shareable = getString(R.string.checkoutThisProduct) + item.users.get(mAuth.getCurrentUser().getUid());
                            if (item.barcode != null && !item.barcode.equals("")) {
                                shareable += getString(R.string.itHasTheBarcode) + item.barcode;
                            }
                            shareable += getString(R.string.andIsSoldAt) + storeList.name;
                            if (price.getText().toString().equals("") || Float.parseFloat(price.getText().toString()) == 0) {
                                shareable += ".";
                            }
                            else {
                                shareable += getString(R.string.forString) + price.getText().toString() + getString(R.string.euro);
                            }
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareable);
                            sharingIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.shareUsing));
                            File storageDir;
                            FirebaseStorage storage;
                            StorageReference storageRef;
                            storage = FirebaseStorage.getInstance();
                            storageRef = storage.getReference();
                            StorageReference imagesRef;
                            if (!barcodeNumber.getText().toString().equals("")) {
                                storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + barcodeNumber.getText().toString());
                                imagesRef = storageRef.child(barcodeNumber.getText().toString());
                            } else {
                                storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id);
                                imagesRef = storageRef.child(id);
                            }
                            imagesRef.listAll()
                                    .addOnSuccessListener(listResult -> {
                                        if (listResult.getItems().size() == 0) {
                                            sharingIntent.setType("text/plain");
                                            startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareUsing)));
                                        } else {
                                            for (StorageReference item : listResult.getItems()) {
                                                boolean exists = false;
                                                for (File f : storageDir.listFiles()) {
                                                    if (f.getName().equals(item.getName())) {
                                                        exists = true;
                                                        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                        Uri screenshotUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), f);
                                                        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                                                        sharingIntent.setType("image/jpeg");
                                                        sharingIntent.setData(screenshotUri);
                                                        startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareUsing)));
                                                        break;
                                                    }
                                                }
                                                if (!exists) {
                                                    File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + id).getAbsolutePath() + "/" + item.getName());
                                                    item.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                                                        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                        Uri screenshotUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), localFile);
                                                        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                                                        sharingIntent.setType("image/jpeg");
                                                        sharingIntent.setData(screenshotUri);
                                                        startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareUsing)));
                                                    });
                                                }
                                                break;
                                            }
                                        }
                                    });
                        }
                    }
                });
                return true;
            case android.R.id.home:
                onBackPressed();    //Call the back button's method
                return true;
            case R.id.editItem:
                Intent intent = new Intent(this, AddItemActivity.class);
                intent.putExtra("TYPE", getResources().getString(R.string.store));
                intent.putExtra("ID", storeId);
                intent.putExtra("ItemId", id);
                intent.putExtra("MODE", "update");
                editResultLauncher.launch(intent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item2);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}