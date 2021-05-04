package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class AddItemActivity extends AppCompatActivity {

    PreviewView viewFinder;
    EditText barcodeNumber;
    Button clearBarcode;
    Button addPictures;
    Button addStores;
    Button saveItem;
    EditText name;
    EditText pantryQuantity;
    EditText targetQuantity;
    String itemId;
    ArrayList<String> photoPaths = new ArrayList<>();
    ArrayList<StoreViewAddItem> storeViewAddItems = new ArrayList<>();
    ActivityResultLauncher<Intent> picturesResultLauncher;
    ActivityResultLauncher<Intent> storesResultLauncher;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private Source source;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);
        Toolbar myToolbar = findViewById(R.id.addItemToolbar);
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
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        viewFinder = findViewById(R.id.viewFinder);
        barcodeNumber = findViewById(R.id.barcodeNumberStoreItem);
        name = findViewById(R.id.productName);
        pantryQuantity = findViewById(R.id.itemStoreQuantity);
        targetQuantity = findViewById(R.id.itemTargetQuantity);
        addPictures = findViewById(R.id.viewPicturesStore);
        picturesResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        photoPaths = result.getData().getStringArrayListExtra("PATHS");
                    }
                });
        addStores = findViewById(R.id.addStoresButton);
        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
            pantryQuantity.setHint(R.string.storeQuantity);
            targetQuantity.setHint(R.string.price);
            addStores.setVisibility(View.INVISIBLE);
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
            storesResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            storeViewAddItems = result.getData().getParcelableArrayListExtra("STORES");
                        }
                    });
        }
        saveItem = findViewById(R.id.saveItemButton);
        saveItem.setOnClickListener(v -> onClickSaveItem(v));
        clearBarcode = findViewById(R.id.clearBarcodeNumber);
        clearBarcode.setOnClickListener(v -> onClickClearBarcode());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            startCamera();
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
        finish();
    }

    public void onClickSaveItem(View view) {
        if (name.getText().toString().equals("")) {
            Toast.makeText(this, R.string.pleaseInsertItemName, Toast.LENGTH_SHORT).show();
            return;
        }
        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry)) && (pantryQuantity.getText().toString().equals(""))) {
            pantryQuantity.setText("0");
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store)) && (pantryQuantity.getText().toString().equals("") || pantryQuantity.getText().toString().equals("0"))) {
            Toast.makeText(this, R.string.pleaseInsertItemStoreQuantity, Toast.LENGTH_SHORT).show();
            return;
        }
        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry)) && targetQuantity.getText().toString().equals("") || targetQuantity.getText().toString().equals("0")) {
            Toast.makeText(this, R.string.pleaseInsertItemTargetQuantity, Toast.LENGTH_SHORT).show();
            return;
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store)) && targetQuantity.getText().toString().equals("")) {
            targetQuantity.setText("0");
        }
        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry)) && Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()) < 0) {
            Toast.makeText(this, "Target Quantity Should Be Bigger Than Pantry Quantity", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isConnected(getApplicationContext()))
            Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
            StoreViewAddItem storeViewAddItem = new StoreViewAddItem(getIntent().getStringExtra("ID"), "", Float.parseFloat(targetQuantity.getText().toString()), true);
            db.collection("StoreList").document(getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            StoreList storeList = document.toObject(StoreList.class);
                            storeViewAddItem.latitude = storeList.latitude;
                            storeViewAddItem.longitude = storeList.longitude;
                            storeViewAddItems.add(storeViewAddItem);
                            if (barcodeNumber.getText().toString().equals("")) {
                                Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                db.collection("Item")
                                        .add(item)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                itemId = documentReference.getId();
                                                for (StoreViewAddItem store : storeViewAddItems) {
                                                    if (store.isChecked) {
                                                        item.stores.put(store.storeId, store.price);
                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                        db.collection("StoreItem").add(storeItem);
                                                    }
                                                }
                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                for (String s : photoPaths) {
                                                    Uri file = Uri.fromFile(new File(s));
                                                    StorageReference imagesRef = storageRef.child(itemId + "/" + file.getLastPathSegment());
                                                    UploadTask uploadTask = imagesRef.putFile(file);
                                                }
                                                db.collection("StoreList").document(getIntent().getStringExtra("ID"))
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    DocumentSnapshot document = task.getResult();
                                                                    if (document.exists()) {
                                                                        StoreList store = document.toObject(StoreList.class);
                                                                        db.collection("StoreList").document(getIntent().getStringExtra("ID")).update("number_of_items", store.number_of_items + 1);
                                                                    }
                                                                }
                                                            }
                                                        });
                                                finish();
                                            }
                                        });
                            } else {
                                db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).
                                        get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            if (task.getResult().size() == 0) {
                                                Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                                db.collection("Item")
                                                        .add(item)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                itemId = documentReference.getId();
                                                                for (StoreViewAddItem store : storeViewAddItems) {
                                                                    if (store.isChecked) {
                                                                        item.stores.put(store.storeId, store.price);
                                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                        db.collection("StoreItem").add(storeItem);
                                                                    }
                                                                }
                                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                                for (String s : photoPaths) {
                                                                    Uri file = Uri.fromFile(new File(s));
                                                                    StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                    UploadTask uploadTask = imagesRef.putFile(file);
                                                                }
                                                                db.collection("StoreList").document(getIntent().getStringExtra("ID"))
                                                                        .get()
                                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    DocumentSnapshot document = task.getResult();
                                                                                    if (document.exists()) {
                                                                                        StoreList store = document.toObject(StoreList.class);
                                                                                        db.collection("StoreList").document(getIntent().getStringExtra("ID")).update("number_of_items", store.number_of_items + 1);
                                                                                    }
                                                                                }
                                                                            }
                                                                        });
                                                                finish();
                                                            }
                                                        });
                                            } else {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    if (document.exists()) {
                                                        Item item = document.toObject(Item.class);
                                                        itemId = document.getId();
                                                        item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                                        db.collection("Item").document(itemId).update("users", item.users);
                                                        for (StoreViewAddItem store : storeViewAddItems) {
                                                            if (store.isChecked) {
                                                                if (!item.stores.keySet().isEmpty()) {
                                                                    if (!item.stores.containsKey(store.storeId)) {
                                                                        item.stores.put(store.storeId, store.price);
                                                                    }
                                                                    for (String s : item.stores.keySet()) {
                                                                        if (store.storeId.equals(s)) {
                                                                            if (store.price != 0) {
                                                                                item.stores.put(s, store.price);
                                                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                                            }
                                                                        } else {
                                                                            db.collection("StoreList").document(s).get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        DocumentSnapshot document = task.getResult();
                                                                                        if (document.exists()) {
                                                                                            StoreList sl = document.toObject(StoreList.class);
                                                                                            float[] results = new float[1];
                                                                                            Location.distanceBetween(Double.parseDouble(store.latitude), Double.parseDouble(store.longitude),
                                                                                                    Double.parseDouble(sl.latitude), Double.parseDouble(sl.longitude),
                                                                                                    results);
                                                                                            //Less than 20 meters
                                                                                            if (results[0] < 20f) {
                                                                                                item.stores.remove(store.storeId);
                                                                                                if (store.price != 0) {
                                                                                                    item.stores.put(s, store.price);
                                                                                                    db.collection("Item").document(itemId).update("stores", item.stores);
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                } else {
                                                                    item.stores.put(store.storeId, store.price);
                                                                }
                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                db.collection("StoreItem").add(storeItem);
                                                            }
                                                        }
                                                        for (String s : photoPaths) {
                                                            Uri file = Uri.fromFile(new File(s));
                                                            StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                            UploadTask uploadTask = imagesRef.putFile(file);
                                                        }
                                                        db.collection("StoreList").document(getIntent().getStringExtra("ID"))
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                        if (task.isSuccessful()) {
                                                                            DocumentSnapshot document = task.getResult();
                                                                            if (document.exists()) {
                                                                                StoreList store = document.toObject(StoreList.class);
                                                                                db.collection("StoreList").document(getIntent().getStringExtra("ID")).update("number_of_items", store.number_of_items + 1);
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                        finish();
                                                    }
                                                }
                                            }
                                        } else {
                                            Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                            db.collection("Item")
                                                    .add(item)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            itemId = documentReference.getId();
                                                            for (StoreViewAddItem store : storeViewAddItems) {
                                                                if (store.isChecked) {
                                                                    item.stores.put(store.storeId, store.price);
                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                    db.collection("StoreItem").add(storeItem);
                                                                }
                                                            }
                                                            db.collection("Item").document(itemId).update("stores", item.stores);
                                                            for (String s : photoPaths) {
                                                                Uri file = Uri.fromFile(new File(s));
                                                                StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                UploadTask uploadTask = imagesRef.putFile(file);
                                                            }
                                                            db.collection("StoreList").document(getIntent().getStringExtra("ID"))
                                                                    .get()
                                                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                            if (task.isSuccessful()) {
                                                                                DocumentSnapshot document = task.getResult();
                                                                                if (document.exists()) {
                                                                                    StoreList store = document.toObject(StoreList.class);
                                                                                    db.collection("StoreList").document(getIntent().getStringExtra("ID")).update("number_of_items", store.number_of_items + 1);
                                                                                }
                                                                            }
                                                                        }
                                                                    });
                                                            finish();
                                                        }
                                                    });
                                        }
                                    }
                                });
                            }

                        }
                    }
                }
            });
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
            if (barcodeNumber.getText().toString().equals("")) {
                Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                db.collection("Item")
                        .add(item)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                itemId = documentReference.getId();
                                PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
                                db.collection("PantryItem")
                                        .add(pantryItem)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                for (StoreViewAddItem store : storeViewAddItems) {
                                                    if (store.isChecked) {
                                                        item.stores.put(store.storeId, store.price);
                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                        db.collection("StoreItem").add(storeItem);
                                                    }
                                                }
                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                for (String s : photoPaths) {
                                                    Uri file = Uri.fromFile(new File(s));
                                                    StorageReference imagesRef = storageRef.child(itemId + "/" + file.getLastPathSegment());
                                                    UploadTask uploadTask = imagesRef.putFile(file);
                                                }
                                                db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    DocumentSnapshot document = task.getResult();
                                                                    if (document.exists()) {
                                                                        PantryList pantry = document.toObject(PantryList.class);
                                                                        db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                                                    }
                                                                }
                                                            }
                                                        });
                                                finish();
                                            }
                                        });
                            }
                        });
            } else {
                db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).
                        get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() == 0) {
                                Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                db.collection("Item")
                                        .add(item)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                itemId = documentReference.getId();
                                                PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
                                                db.collection("PantryItem")
                                                        .add(pantryItem)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                for (StoreViewAddItem store : storeViewAddItems) {
                                                                    if (store.isChecked) {
                                                                        item.stores.put(store.storeId, store.price);
                                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                        db.collection("StoreItem").add(storeItem);
                                                                    }
                                                                }
                                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                                for (String s : photoPaths) {
                                                                    Uri file = Uri.fromFile(new File(s));
                                                                    StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                    UploadTask uploadTask = imagesRef.putFile(file);
                                                                }
                                                                db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                                                                        .get()
                                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    DocumentSnapshot document = task.getResult();
                                                                                    if (document.exists()) {
                                                                                        PantryList pantry = document.toObject(PantryList.class);
                                                                                        db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                                                                    }
                                                                                }
                                                                            }
                                                                        });
                                                                finish();
                                                            }
                                                        });
                                            }
                                        });
                            } else {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if (document.exists()) {
                                        Item item = document.toObject(Item.class);
                                        itemId = document.getId();
                                        item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                        db.collection("Item").document(itemId).update("users", item.users);
                                        PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
                                        db.collection("PantryItem")
                                                .add(pantryItem)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {
                                                        for (StoreViewAddItem store : storeViewAddItems) {
                                                            if (store.isChecked) {
                                                                if (!item.stores.keySet().isEmpty()) {
                                                                    if (!item.stores.containsKey(store.storeId)) {
                                                                        item.stores.put(store.storeId, store.price);
                                                                    }
                                                                    for (String s : item.stores.keySet()) {
                                                                        if (store.storeId.equals(s)) {
                                                                            if (store.price != 0) {
                                                                                item.stores.put(s, store.price);
                                                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                                            }
                                                                        } else {
                                                                            db.collection("StoreList").document(s).get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        DocumentSnapshot document = task.getResult();
                                                                                        if (document.exists()) {
                                                                                            StoreList sl = document.toObject(StoreList.class);
                                                                                            float[] results = new float[1];
                                                                                            Location.distanceBetween(Double.parseDouble(store.latitude), Double.parseDouble(store.longitude),
                                                                                                    Double.parseDouble(sl.latitude), Double.parseDouble(sl.longitude),
                                                                                                    results);
                                                                                            //Less than 20 meters
                                                                                            if (results[0] < 20f) {
                                                                                                item.stores.remove(store.storeId);
                                                                                                if (store.price != 0) {
                                                                                                    item.stores.put(s, store.price);
                                                                                                    db.collection("Item").document(itemId).update("stores", item.stores);
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                } else {
                                                                    item.stores.put(store.storeId, store.price);
                                                                }
                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                db.collection("StoreItem").add(storeItem);
                                                            }
                                                        }
                                                        for (String s : photoPaths) {
                                                            Uri file = Uri.fromFile(new File(s));
                                                            StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                            UploadTask uploadTask = imagesRef.putFile(file);
                                                        }
                                                        db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                        if (task.isSuccessful()) {
                                                                            DocumentSnapshot document = task.getResult();
                                                                            if (document.exists()) {
                                                                                PantryList pantry = document.toObject(PantryList.class);
                                                                                db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                                                                finish();
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                        finish();
                                                    }
                                                });
                                    }
                                }
                            }
                        } else {
                            Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                            db.collection("Item")
                                    .add(item)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            itemId = documentReference.getId();
                                            PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
                                            db.collection("PantryItem")
                                                    .add(pantryItem)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            for (StoreViewAddItem store : storeViewAddItems) {
                                                                if (store.isChecked) {
                                                                    item.stores.put(store.storeId, store.price);
                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                    db.collection("StoreItem").add(storeItem);
                                                                }
                                                            }
                                                            db.collection("Item").document(itemId).update("stores", item.stores);
                                                            for (String s : photoPaths) {
                                                                Uri file = Uri.fromFile(new File(s));
                                                                StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                UploadTask uploadTask = imagesRef.putFile(file);
                                                            }
                                                            db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                                                                    .get()
                                                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                            if (task.isSuccessful()) {
                                                                                DocumentSnapshot document = task.getResult();
                                                                                if (document.exists()) {
                                                                                    PantryList pantry = document.toObject(PantryList.class);
                                                                                    db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                                                                }
                                                                            }
                                                                        }
                                                                    });
                                                            finish();
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
                });
            }
        }
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

    public void onClickAddPictures(View view) {
        Intent intent = new Intent(this, AddPicturesActivity.class);
        intent.putStringArrayListExtra("PATHS", photoPaths);
        intent.putExtra("MODE", "add");
        intent.putExtra("ID", barcodeNumber.getText());
        picturesResultLauncher.launch(intent);
    }

    public void onClickAddStores(View view) {
        Intent intent = new Intent(this, AddStoresActivity.class);
        intent.putExtra("MODE", "add");
        db.collection("StoreList")
                .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            StoreList store = document.toObject(StoreList.class);
                            if (!storeViewAddItems.isEmpty()) {
                                Boolean present = false;
                                for (StoreViewAddItem item : storeViewAddItems) {
                                    if (item.storeId.equals(document.getId())) {
                                        present = true;
                                    }
                                }
                                if (!present) {
                                    StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f, true);
                                    storeViewAddItem.latitude = store.latitude;
                                    storeViewAddItem.longitude = store.longitude;
                                    storeViewAddItems.add(storeViewAddItem);
                                }

                            } else {
                                StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f, true);
                                storeViewAddItem.latitude = store.latitude;
                                storeViewAddItem.longitude = store.longitude;
                                storeViewAddItems.add(storeViewAddItem);
                            }
                        }
                        intent.putParcelableArrayListExtra("STORES", storeViewAddItems);
                        storesResultLauncher.launch(intent);
                    } else {
                        Log.d("TAG", "Error getting documents: ", task.getException());
                    }
                });
    }

    void onClickClearBarcode() {
        barcodeNumber.setText("");
    }

    void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder()
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(1920, 1080))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getApplication()), image -> {
                    BarcodeScannerOptions options =
                            new BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(
                                            Barcode.FORMAT_CODE_128,
                                            Barcode.FORMAT_UPC_A,
                                            Barcode.FORMAT_UPC_E)
                                    .build();
                    BarcodeScanner scanner = BarcodeScanning.getClient(options);
                    @SuppressLint("UnsafeOptInUsageError") Image mediaImage = image.getImage();
                    if (image != null) {
                        InputImage imageInput = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                        Task<List<Barcode>> result = scanner.process(imageInput)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        Rect bounds = barcode.getBoundingBox();
                                        Point[] corners = barcode.getCornerPoints();

                                        String rawValue = barcode.getRawValue();

                                        // See API reference for complete list of supported types
                                        if (barcodeNumber.getText().toString().matches("")) {
                                            barcodeNumber.setText(rawValue);
                                            storeViewAddItems.clear();
                                            autocompleteStoreList();
                                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                            builder.setCancelable(true);
                                            builder.setTitle(R.string.pleaseSubmitPriceDataAndPictures);
                                            builder.setMessage(R.string.ifPossibleSubmitStoresPricesImages);
                                            builder.setPositiveButton(R.string.addPictures, (dialog, which) -> {
                                                Intent intent = new Intent(this, AddPicturesActivity.class);
                                                intent.putExtra("MODE", "add");
                                                intent.putStringArrayListExtra("PATHS", photoPaths);
                                                picturesResultLauncher.launch(intent);
                                            });
                                            builder.setNeutralButton(R.string.addStores, (dialog, which) -> {
                                                Intent intent = new Intent(this, AddStoresActivity.class);
                                                intent.putExtra("MODE", "add");
                                                db.collection("StoreList")
                                                        .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                                        .get(source)
                                                        .addOnCompleteListener(task -> {
                                                            if (task.isSuccessful()) {
                                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                                    StoreList store = document.toObject(StoreList.class);
                                                                    if (!storeViewAddItems.isEmpty()) {
                                                                        Boolean present = false;
                                                                        for (StoreViewAddItem item : storeViewAddItems) {
                                                                            if (item.name.equals(store.name)) {
                                                                                present = true;
                                                                            }
                                                                        }
                                                                        if (!present) {
                                                                            StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f);
                                                                            storeViewAddItem.latitude = store.latitude;
                                                                            storeViewAddItem.longitude = store.longitude;
                                                                            storeViewAddItems.add(storeViewAddItem);
                                                                        }

                                                                    } else {
                                                                        StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f);
                                                                        storeViewAddItem.latitude = store.latitude;
                                                                        storeViewAddItem.longitude = store.longitude;
                                                                        storeViewAddItems.add(storeViewAddItem);
                                                                    }
                                                                }
                                                                intent.putParcelableArrayListExtra("STORES", storeViewAddItems);
                                                                storesResultLauncher.launch(intent);
                                                            } else {
                                                                Log.d("TAG", "Error getting documents: ", task.getException());
                                                            }
                                                        });
                                            });
                                            AlertDialog dialog = builder.create();
                                            dialog.show();
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Error", "exception", e);
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                })
                                .addOnCompleteListener(task -> {
                                    image.close();
                                });
                    }
                });
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future
                // This should never be reached
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void autocompleteStoreList() {
        if (storeViewAddItems.isEmpty()) {
            db.collection("Item")
                    .whereEqualTo("barcode", barcodeNumber.getText().toString())
                    .get(source)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Item item = document.toObject(Item.class);
                                if (item.users.containsKey(mAuth.getCurrentUser().getUid())) {
                                    name.setText(item.users.get(mAuth.getCurrentUser().getUid()));
                                }
                                for (String storeId : item.stores.keySet()) {
                                    db.collection("StoreList")
                                            .document(storeId)
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    DocumentSnapshot document2 = task2.getResult();
                                                    StoreList item2 = document2.toObject(StoreList.class);
                                                    if (item2.users.contains(mAuth.getCurrentUser().getUid())) {
                                                        StoreViewAddItem storeViewAddItem = new StoreViewAddItem(storeId, item2.name, item.stores.get(storeId), true);
                                                        storeViewAddItem.latitude = item2.latitude;
                                                        storeViewAddItem.longitude = item2.longitude;
                                                        storeViewAddItems.add(storeViewAddItem);
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
                                                                                    storeViewAddItems.add(storeViewAddItem);
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
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    });
        }
    }
}