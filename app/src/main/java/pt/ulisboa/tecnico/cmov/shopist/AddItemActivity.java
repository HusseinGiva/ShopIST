package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.util.ArrayList;
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
    private Source source;
    private StorageReference storageRef;

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
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        viewFinder = findViewById(R.id.viewFinder);
        barcodeNumber = findViewById(R.id.barcodeNumberStoreItem);
        barcodeNumber.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                storeViewAddItems.clear();
                AlertDialog.Builder builder = new AlertDialog.Builder(AddItemActivity.this);
                builder.setCancelable(true);
                builder.setTitle(R.string.pleaseSubmitPriceDataAndPictures);
                builder.setMessage(R.string.ifPossibleSubmitStoresPricesImages);
                builder.setPositiveButton(R.string.addPictures, (dialog, which) -> onClickAddPictures());
                if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
                    builder.setNeutralButton(R.string.addStores, (dialog, which) -> onClickAddStores());
                } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                    targetQuantity.setText("");
                }
                autocompleteStoreList();
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            } else {
                return false;
            }
        });
        barcodeNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                storeViewAddItems.clear();
                AlertDialog.Builder builder = new AlertDialog.Builder(AddItemActivity.this);
                builder.setCancelable(true);
                builder.setTitle(R.string.pleaseSubmitPriceDataAndPictures);
                builder.setMessage(R.string.ifPossibleSubmitStoresPricesImages);
                builder.setPositiveButton(R.string.addPictures, (dialog, which) -> onClickAddPictures());
                if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
                    builder.setNeutralButton(R.string.addStores, (dialog, which) -> onClickAddStores());
                } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                    targetQuantity.setText("");
                }
                autocompleteStoreList();
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        name = findViewById(R.id.productName);
        pantryQuantity = findViewById(R.id.itemStoreQuantity);
        targetQuantity = findViewById(R.id.itemTargetQuantity);
        addPictures = findViewById(R.id.viewPicturesStore);
        addPictures.setOnClickListener(v -> onClickAddPictures());
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
            targetQuantity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            addStores.setVisibility(View.INVISIBLE);
            if (getIntent().getStringExtra("MODE").equals("update")) {
                getSupportActionBar().setTitle(R.string.editProduct);
                db.collection("Item").document(getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Item item = document.toObject(Item.class);
                            barcodeNumber.setText(item.barcode);
                            name.setText(item.users.get(mAuth.getCurrentUser().getUid()));
                            db.collection("StoreItem").whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).whereEqualTo("storeId", getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                        StoreItem storeItem = document1.toObject(StoreItem.class);
                                        pantryQuantity.setText(String.valueOf(storeItem.quantity));
                                    }
                                }
                            });
                            String storeId = getIntent().getStringExtra("ID");
                            for (String s : item.stores.keySet()) {
                                if (storeId.equals(s)) {
                                    targetQuantity.setText(String.valueOf(item.stores.get(s)));
                                } else {
                                    db.collection("StoreList").document(s).get(source).addOnCompleteListener(task12 -> {
                                        if (task12.isSuccessful()) {
                                            DocumentSnapshot document12 = task12.getResult();
                                            if (document12.exists()) {
                                                StoreList sl = document12.toObject(StoreList.class);
                                                float[] results = new float[1];
                                                db.collection("StoreList").document(storeId).get(source).addOnCompleteListener(task121 -> {
                                                    if (task121.isSuccessful()) {
                                                        DocumentSnapshot document121 = task121.getResult();
                                                        if (document121.exists()) {
                                                            StoreList storeList = document121.toObject(StoreList.class);
                                                            if (storeList.latitude != null && storeList.longitude != null && sl.latitude != null && sl.longitude != null) {
                                                                Location.distanceBetween(Double.parseDouble(storeList.latitude), Double.parseDouble(storeList.longitude),
                                                                        Double.parseDouble(sl.latitude), Double.parseDouble(sl.longitude),
                                                                        results);
                                                                //Less than 20 meters
                                                                if (results[0] < 20f) {
                                                                    targetQuantity.setText(String.valueOf(item.stores.get(s)));
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
                        }
                    }
                });
            }
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
            addStores.setOnClickListener(v -> onClickAddStores());
            storesResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            storeViewAddItems = result.getData().getParcelableArrayListExtra("STORES");
                        }
                    });
            if (getIntent().getStringExtra("MODE").equals("update")) {
                String id = getIntent().getStringExtra("ItemId");
                getSupportActionBar().setTitle(R.string.editProduct);
                db = FirebaseFirestore.getInstance();
                mAuth = FirebaseAuth.getInstance();
                db.collection("Item")
                        .document(id)
                        .get(source)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                Item item = document.toObject(Item.class);
                                for (String storeId : item.stores.keySet()) {
                                    db.collection("StoreList")
                                            .document(storeId)
                                            .get(source)
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
                                barcodeNumber.setText(item.barcode);
                                name.setText(item.users.get(mAuth.getCurrentUser().getUid()));
                                db.collection("PantryItem").whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).whereEqualTo("pantryId", getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(task13 -> {
                                    if (task13.isSuccessful()) {
                                        for (QueryDocumentSnapshot document13 : task13.getResult()) {
                                            PantryItem pantryItem = document13.toObject(PantryItem.class);
                                            pantryQuantity.setText(String.valueOf(pantryItem.quantity));
                                            targetQuantity.setText(String.valueOf(pantryItem.idealQuantity));
                                        }
                                    }
                                });
                            }
                        });
            } else if (getIntent().getStringExtra("MODE").equals("add")) {
                db = FirebaseFirestore.getInstance();
                mAuth = FirebaseAuth.getInstance();
                db.collection("StoreList")
                        .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                        .get(source)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    StoreList item = document.toObject(StoreList.class);
                                    StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), item.name, 0f, true);
                                    storeViewAddItem.latitude = item.latitude;
                                    storeViewAddItem.longitude = item.longitude;
                                    storeViewAddItems.add(storeViewAddItem);
                                }
                            }
                        });
            }
        }
        saveItem = findViewById(R.id.saveItemButton);
        saveItem.setOnClickListener(this::onClickSaveItem);
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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();    //Call the back button's method
            return true;
        }// If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
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
        if (pantryQuantity.getText().toString().equals("")) {
            if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                Toast.makeText(this, R.string.pleaseInsertItemStoreQuantity, Toast.LENGTH_SHORT).show();
                return;
            } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
                pantryQuantity.setText("0");
            }
        }
        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry)) && targetQuantity.getText().toString().equals("") || targetQuantity.getText().toString().equals("0")) {
            Toast.makeText(this, R.string.pleaseInsertItemTargetQuantity, Toast.LENGTH_SHORT).show();
            return;
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store)) && targetQuantity.getText().toString().equals("")) {
            targetQuantity.setText("0");
        }
        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry)) && Integer.parseInt(targetQuantity.getText().toString()) < Integer.parseInt(pantryQuantity.getText().toString())) {
            Toast.makeText(this, R.string.targetQuantityBiggerPantryQuantity, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isConnected(getApplicationContext()))
            Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
            StoreViewAddItem storeViewAddItem = new StoreViewAddItem(getIntent().getStringExtra("ID"), "", Float.parseFloat(targetQuantity.getText().toString()), true);
            db.collection("StoreList").document(getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        StoreList storeList = document.toObject(StoreList.class);
                        storeViewAddItem.latitude = storeList.latitude;
                        storeViewAddItem.longitude = storeList.longitude;
                        storeViewAddItems.clear();
                        storeViewAddItems.add(storeViewAddItem);
                        if (barcodeNumber.getText().toString().equals("")) {
                            if (getIntent().getStringExtra("MODE").equals("update")) {
                                db.collection("Item")
                                        .document(getIntent().getStringExtra("ItemId"))
                                        .get(source)
                                        .addOnCompleteListener(task16 -> {
                                            if (task16.isSuccessful()) {
                                                DocumentSnapshot document14 = task16.getResult();
                                                if (document14.exists()) {
                                                    Item item = document14.toObject(Item.class);
                                                    item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                                    item.barcode = barcodeNumber.getText().toString();
                                                    item.stores.clear();
                                                    itemId = getIntent().getStringExtra("ItemId");
                                                    db.collection("Item").document(itemId).update("users", item.users, "barcode", item.barcode);
                                                    for (StoreViewAddItem store : storeViewAddItems) {
                                                        if (store.isChecked) {
                                                            item.stores.put(store.storeId, store.price);
                                                            db.collection("StoreItem").whereEqualTo("storeId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task161 -> {
                                                                if (task161.isSuccessful()) {
                                                                    if (task161.getResult().size() != 0) {
                                                                        for (QueryDocumentSnapshot document141 : task161.getResult()) {
                                                                            db.collection("StoreItem").document(document141.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()));
                                                                        }
                                                                    } else {
                                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                        db.collection("StoreItem").add(storeItem);
                                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                    }
                                                                } else {
                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                    db.collection("StoreItem").add(storeItem);
                                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                }
                                                            });
                                                        }
                                                    }
                                                    db.collection("Item").document(itemId).update("stores", item.stores);
                                                    for (String s : photoPaths) {
                                                        Uri file = Uri.fromFile(new File(s));
                                                        StorageReference imagesRef = storageRef.child(itemId + "/" + file.getLastPathSegment());
                                                        imagesRef.putFile(file);
                                                    }
                                                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                    for (File f : storageDir.listFiles()) {
                                                        deleteRecursive(f);
                                                    }
                                                    finish();
                                                }
                                            }
                                        });
                            } else if (getIntent().getStringExtra("MODE").equals("add")) {
                                Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                db.collection("Item")
                                        .add(item)
                                        .addOnSuccessListener(documentReference -> {
                                            itemId = documentReference.getId();
                                            for (StoreViewAddItem store : storeViewAddItems) {
                                                if (store.isChecked) {
                                                    item.stores.put(store.storeId, store.price);
                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                    db.collection("StoreItem").add(storeItem);
                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                }
                                            }
                                            db.collection("Item").document(itemId).update("stores", item.stores);
                                            for (String s : photoPaths) {
                                                Uri file = Uri.fromFile(new File(s));
                                                StorageReference imagesRef = storageRef.child(itemId + "/" + file.getLastPathSegment());
                                                imagesRef.putFile(file);
                                            }
                                            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                            for (File f : storageDir.listFiles()) {
                                                deleteRecursive(f);
                                            }
                                            finish();
                                        });
                            }
                        } else {
                            db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).
                                    get(source).addOnCompleteListener(task18 -> {
                                if (task18.isSuccessful()) {
                                    if (task18.getResult().size() == 0) {
                                        if (getIntent().getStringExtra("MODE").equals("update")) {
                                            db.collection("Item")
                                                    .document(getIntent().getStringExtra("ItemId"))
                                                    .get(source)
                                                    .addOnCompleteListener(task1819 -> {
                                                        if (task1819.isSuccessful()) {
                                                            DocumentSnapshot document16 = task1819.getResult();
                                                            if (document16.exists()) {
                                                                Item item = document16.toObject(Item.class);
                                                                itemId = getIntent().getStringExtra("ItemId");
                                                                item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                                                item.barcode = barcodeNumber.getText().toString();
                                                                item.stores.clear();
                                                                db.collection("Item").document(itemId).update("users", item.users, "barcode", item.barcode);
                                                                for (StoreViewAddItem store : storeViewAddItems) {
                                                                    if (store.isChecked) {
                                                                        item.stores.put(store.storeId, store.price);
                                                                        db.collection("StoreItem").whereEqualTo("storeId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task1818 -> {
                                                                            if (task1818.isSuccessful()) {
                                                                                if (task1818.getResult().size() != 0) {
                                                                                    for (QueryDocumentSnapshot document1615 : task1818.getResult()) {
                                                                                        db.collection("StoreItem").document(document1615.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                    }
                                                                                } else {
                                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                    db.collection("StoreItem").add(storeItem);
                                                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);

                                                                                }
                                                                            } else {
                                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                db.collection("StoreItem").add(storeItem);
                                                                                db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);

                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                                for (String s : photoPaths) {
                                                                    Uri file = Uri.fromFile(new File(s));
                                                                    StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                    imagesRef.putFile(file);
                                                                }
                                                                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                                for (File f : storageDir.listFiles()) {
                                                                    deleteRecursive(f);
                                                                }
                                                                finish();
                                                            }
                                                        }
                                                    });
                                        } else if (getIntent().getStringExtra("MODE").equals("add")) {
                                            Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                            db.collection("Item")
                                                    .add(item)
                                                    .addOnSuccessListener(documentReference -> {
                                                        itemId = documentReference.getId();
                                                        for (StoreViewAddItem store : storeViewAddItems) {
                                                            if (store.isChecked) {
                                                                item.stores.put(store.storeId, store.price);
                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                db.collection("StoreItem").add(storeItem);
                                                                db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                            }
                                                        }
                                                        db.collection("Item").document(itemId).update("stores", item.stores);
                                                        for (String s : photoPaths) {
                                                            Uri file = Uri.fromFile(new File(s));
                                                            StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                            imagesRef.putFile(file);
                                                        }
                                                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                        for (File f : storageDir.listFiles()) {
                                                            deleteRecursive(f);
                                                        }
                                                        finish();
                                                    });
                                        }
                                    } else {
                                        for (QueryDocumentSnapshot document16 : task18.getResult()) {
                                            if (document16.exists()) {
                                                if (getIntent().getStringExtra("MODE").equals("update")) {
                                                    db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).whereNotEqualTo(FieldPath.documentId(), getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task2030 -> {
                                                        if (task2030.isSuccessful()) {
                                                            if (task2030.getResult().size() != 0) {
                                                                for (QueryDocumentSnapshot document2030 : task2030.getResult()) {
                                                                    db.collection("StoreItem").whereEqualTo("storeId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", document2030.getId()).get(source).addOnCompleteListener(task2031 -> {
                                                                        if (task2031.isSuccessful()) {
                                                                            if (task2031.getResult().size() != 0) {
                                                                                for (QueryDocumentSnapshot document2031 : task2031.getResult()) {
                                                                                    Toast.makeText(this, R.string.productSameBarcodeAlreadyList, Toast.LENGTH_LONG).show();
                                                                                    return;
                                                                                }
                                                                            } else {
                                                                                Item item = document16.toObject(Item.class);
                                                                                itemId = document16.getId();
                                                                                auxAddItemStoreFunction(item);
                                                                            }
                                                                        } else {
                                                                            Item item = document16.toObject(Item.class);
                                                                            itemId = document16.getId();
                                                                            auxAddItemStoreFunction(item);
                                                                        }
                                                                    });
                                                                }
                                                            } else {
                                                                Item item = document16.toObject(Item.class);
                                                                itemId = document16.getId();
                                                                auxAddItemStoreFunction(item);
                                                            }
                                                        } else {
                                                            Item item = document16.toObject(Item.class);
                                                            itemId = document16.getId();
                                                            auxAddItemStoreFunction(item);
                                                        }
                                                    });
                                                } else if (getIntent().getStringExtra("MODE").equals("add")) {
                                                    db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).get(source).addOnCompleteListener(task2030 -> {
                                                        if (task2030.isSuccessful()) {
                                                            if (task2030.getResult().size() != 0) {
                                                                for (QueryDocumentSnapshot document2030 : task2030.getResult()) {
                                                                    db.collection("StoreItem").whereEqualTo("storeId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", document2030.getId()).get(source).addOnCompleteListener(task2031 -> {
                                                                        if (task2031.isSuccessful()) {
                                                                            if (task2031.getResult().size() != 0) {
                                                                                for (QueryDocumentSnapshot document2031 : task2031.getResult()) {
                                                                                    Toast.makeText(this, R.string.productSameBarcodeAlreadyList, Toast.LENGTH_LONG).show();
                                                                                    return;
                                                                                }
                                                                            } else {
                                                                                Item item = document16.toObject(Item.class);
                                                                                itemId = document16.getId();
                                                                                auxAddItemStoreFunction(item);
                                                                            }
                                                                        } else {
                                                                            Item item = document16.toObject(Item.class);
                                                                            itemId = document16.getId();
                                                                            auxAddItemStoreFunction(item);
                                                                        }
                                                                    });
                                                                }
                                                            } else {
                                                                Item item = document16.toObject(Item.class);
                                                                itemId = document16.getId();
                                                                auxAddItemStoreFunction(item);
                                                            }
                                                        } else {
                                                            Item item = document16.toObject(Item.class);
                                                            itemId = document16.getId();
                                                            auxAddItemStoreFunction(item);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (getIntent().getStringExtra("MODE").equals("update")) {
                                        db.collection("Item")
                                                .document(getIntent().getStringExtra("ItemId"))
                                                .get(source)
                                                .addOnCompleteListener(task1813 -> {
                                                    if (task1813.isSuccessful()) {
                                                        DocumentSnapshot document16 = task1813.getResult();
                                                        if (document16.exists()) {
                                                            Item item = document16.toObject(Item.class);
                                                            itemId = getIntent().getStringExtra("ItemId");
                                                            item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                                            item.stores.clear();
                                                            item.barcode = barcodeNumber.getText().toString();
                                                            db.collection("Item").document(itemId).update("users", item.users, "barcode", item.barcode);
                                                            for (StoreViewAddItem store : storeViewAddItems) {
                                                                if (store.isChecked) {
                                                                    item.stores.put(store.storeId, store.price);
                                                                    db.collection("StoreItem").whereEqualTo("storeId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task1812 -> {
                                                                        if (task1812.isSuccessful()) {
                                                                            if (task1812.getResult().size() != 0) {
                                                                                for (QueryDocumentSnapshot document161 : task1812.getResult()) {
                                                                                    db.collection("StoreItem").document(document161.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                }
                                                                            } else {
                                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                db.collection("StoreItem").add(storeItem);
                                                                                db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);

                                                                            }
                                                                        } else {
                                                                            StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                                            db.collection("StoreItem").add(storeItem);
                                                                            db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);

                                                                        }
                                                                    });
                                                                }
                                                            }
                                                            db.collection("Item").document(itemId).update("stores", item.stores);
                                                            for (String s : photoPaths) {
                                                                Uri file = Uri.fromFile(new File(s));
                                                                StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                imagesRef.putFile(file);
                                                            }
                                                            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                            for (File f : storageDir.listFiles()) {
                                                                deleteRecursive(f);
                                                            }
                                                            finish();
                                                        }
                                                    }
                                                });
                                    } else if (getIntent().getStringExtra("MODE").equals("add")) {
                                        Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                        db.collection("Item")
                                                .add(item)
                                                .addOnSuccessListener(documentReference -> {
                                                    itemId = documentReference.getId();
                                                    for (StoreViewAddItem store : storeViewAddItems) {
                                                        if (store.isChecked) {
                                                            item.stores.put(store.storeId, store.price);
                                                            StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                                            db.collection("StoreItem").add(storeItem);
                                                            db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                        }
                                                    }
                                                    db.collection("Item").document(itemId).update("stores", item.stores);
                                                    for (String s : photoPaths) {
                                                        Uri file = Uri.fromFile(new File(s));
                                                        StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                        imagesRef.putFile(file);
                                                    }
                                                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                    for (File f : storageDir.listFiles()) {
                                                        deleteRecursive(f);
                                                    }
                                                    finish();
                                                });
                                    }
                                }
                            });
                        }

                    }
                }
            });
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
            if (barcodeNumber.getText().toString().equals("")) {
                if (getIntent().getStringExtra("MODE").equals("update")) {
                    db.collection("Item")
                            .document(getIntent().getStringExtra("ItemId"))
                            .get(source)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        Item item = document.toObject(Item.class);
                                        itemId = getIntent().getStringExtra("ItemId");
                                        item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                        item.barcode = barcodeNumber.getText().toString();
                                        item.stores.clear();
                                        db.collection("Item").document(itemId).update("users", item.users, "barcode", item.barcode);
                                        db.collection("PantryItem").whereEqualTo("pantryId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task117 -> {
                                            if (task117.isSuccessful()) {
                                                if (task117.getResult().size() != 0) {
                                                    for (QueryDocumentSnapshot document113 : task117.getResult()) {
                                                        PantryItem pantryItem = document113.toObject(PantryItem.class);
                                                        db.collection("PantryItem").document(document113.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()), "idealQuantity", Integer.parseInt(targetQuantity.getText().toString()));
                                                        for (StoreViewAddItem store : storeViewAddItems) {
                                                            if (store.isChecked) {
                                                                item.stores.put(store.storeId, store.price);
                                                                db.collection("StoreItem").whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task116 -> {
                                                                    if (task116.isSuccessful()) {
                                                                        if (task116.getResult().size() != 0) {
                                                                            for (QueryDocumentSnapshot document112 : task116.getResult()) {
                                                                                StoreItem storeItem = document112.toObject(StoreItem.class);
                                                                                int oldQuantity = pantryItem.idealQuantity - pantryItem.quantity;
                                                                                int newQuantity = storeItem.quantity - oldQuantity + (Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                if (newQuantity < 0) {
                                                                                    newQuantity = 0;
                                                                                }
                                                                                db.collection("StoreItem").document(document112.getId()).update("quantity", newQuantity);
                                                                            }
                                                                        } else {
                                                                            StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                            db.collection("StoreItem").add(storeItem);
                                                                            db.collection("StoreList").document(store.storeId)
                                                                                    .get(source)
                                                                                    .addOnCompleteListener(task1814 -> {
                                                                                        if (task1814.isSuccessful()) {
                                                                                            DocumentSnapshot document1612 = task1814.getResult();
                                                                                            if (document1612.exists()) {
                                                                                                StoreList storeList = document1612.toObject(StoreList.class);
                                                                                                db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        }
                                                                    } else {
                                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                        db.collection("StoreItem").add(storeItem);
                                                                        db.collection("StoreList").document(store.storeId)
                                                                                .get(source)
                                                                                .addOnCompleteListener(task1814 -> {
                                                                                    if (task1814.isSuccessful()) {
                                                                                        DocumentSnapshot document1612 = task1814.getResult();
                                                                                        if (document1612.exists()) {
                                                                                            StoreList storeList = document1612.toObject(StoreList.class);
                                                                                            db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                        }
                                                                                    }
                                                                                });
                                                                    }
                                                                });
                                                            }
                                                        }
                                                        db.collection("Item").document(itemId).update("stores", item.stores);
                                                        for (String s : photoPaths) {
                                                            Uri file = Uri.fromFile(new File(s));
                                                            StorageReference imagesRef = storageRef.child(itemId + "/" + file.getLastPathSegment());
                                                            imagesRef.putFile(file);
                                                        }
                                                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                        for (File f : storageDir.listFiles()) {
                                                            deleteRecursive(f);
                                                        }
                                                        finish();
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                } else if (getIntent().getStringExtra("MODE").equals("add")) {
                    Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                    db.collection("Item")
                            .add(item)
                            .addOnSuccessListener(documentReference -> {
                                itemId = documentReference.getId();
                                PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
                                db.collection("PantryItem")
                                        .add(pantryItem)
                                        .addOnSuccessListener(documentReference12 -> {
                                            for (StoreViewAddItem store : storeViewAddItems) {
                                                if (store.isChecked) {
                                                    item.stores.put(store.storeId, store.price);
                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                    db.collection("StoreItem").add(storeItem);
                                                    db.collection("StoreList").document(store.storeId)
                                                            .get(source)
                                                            .addOnCompleteListener(task1814 -> {
                                                                if (task1814.isSuccessful()) {
                                                                    DocumentSnapshot document1612 = task1814.getResult();
                                                                    if (document1612.exists()) {
                                                                        StoreList storeList = document1612.toObject(StoreList.class);
                                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                            db.collection("Item").document(itemId).update("stores", item.stores);
                                            for (String s : photoPaths) {
                                                Uri file = Uri.fromFile(new File(s));
                                                StorageReference imagesRef = storageRef.child(itemId + "/" + file.getLastPathSegment());
                                                imagesRef.putFile(file);
                                            }
                                            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                            for (File f : storageDir.listFiles()) {
                                                deleteRecursive(f);
                                            }
                                            db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                                                    .get(source)
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            DocumentSnapshot document = task.getResult();
                                                            if (document.exists()) {
                                                                PantryList pantry = document.toObject(PantryList.class);
                                                                db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                                            }
                                                        }
                                                    });
                                            finish();
                                        });
                            });
                }
            } else {
                db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).
                        get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() == 0) {
                            if (getIntent().getStringExtra("MODE").equals("update")) {
                                db.collection("Item")
                                        .document(getIntent().getStringExtra("ItemId"))
                                        .get(source)
                                        .addOnCompleteListener(task115 -> {
                                            if (task115.isSuccessful()) {
                                                DocumentSnapshot document = task115.getResult();
                                                if (document.exists()) {
                                                    Item item = document.toObject(Item.class);
                                                    itemId = getIntent().getStringExtra("ItemId");
                                                    item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                                    item.barcode = barcodeNumber.getText().toString();
                                                    item.stores.clear();
                                                    db.collection("Item").document(itemId).update("users", item.users, "barcode", item.barcode);
                                                    db.collection("PantryItem").whereEqualTo("pantryId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task114 -> {
                                                        if (task114.isSuccessful()) {
                                                            if (task114.getResult().size() != 0) {
                                                                for (QueryDocumentSnapshot document111 : task114.getResult()) {
                                                                    PantryItem pantryItem = document111.toObject(PantryItem.class);
                                                                    db.collection("PantryItem").document(document111.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()), "idealQuantity", Integer.parseInt(targetQuantity.getText().toString()));
                                                                    for (StoreViewAddItem store : storeViewAddItems) {
                                                                        if (store.isChecked) {
                                                                            item.stores.put(store.storeId, store.price);
                                                                            db.collection("StoreItem").whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task113 -> {
                                                                                if (task113.isSuccessful()) {
                                                                                    if (task113.getResult().size() != 0) {
                                                                                        for (QueryDocumentSnapshot document110 : task113.getResult()) {
                                                                                            StoreItem storeItem = document110.toObject(StoreItem.class);
                                                                                            int oldQuantity = pantryItem.idealQuantity - pantryItem.quantity;
                                                                                            int newQuantity = storeItem.quantity - oldQuantity + (Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                            if (newQuantity < 0) {
                                                                                                newQuantity = 0;
                                                                                            }
                                                                                            db.collection("StoreItem").document(document110.getId()).update("quantity", newQuantity);
                                                                                        }
                                                                                    } else {
                                                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                        db.collection("StoreItem").add(storeItem);
                                                                                        db.collection("StoreList").document(store.storeId)
                                                                                                .get(source)
                                                                                                .addOnCompleteListener(task1814 -> {
                                                                                                    if (task1814.isSuccessful()) {
                                                                                                        DocumentSnapshot document1612 = task1814.getResult();
                                                                                                        if (document1612.exists()) {
                                                                                                            StoreList storeList = document1612.toObject(StoreList.class);
                                                                                                            db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                                        }
                                                                                                    }
                                                                                                });
                                                                                    }
                                                                                } else {
                                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                    db.collection("StoreItem").add(storeItem);
                                                                                    db.collection("StoreList").document(store.storeId)
                                                                                            .get(source)
                                                                                            .addOnCompleteListener(task1814 -> {
                                                                                                if (task1814.isSuccessful()) {
                                                                                                    DocumentSnapshot document1612 = task1814.getResult();
                                                                                                    if (document1612.exists()) {
                                                                                                        StoreList storeList = document1612.toObject(StoreList.class);
                                                                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                                    }
                                                                                                }
                                                                                            });
                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                    db.collection("Item").document(itemId).update("stores", item.stores);
                                                                    for (String s : photoPaths) {
                                                                        Uri file = Uri.fromFile(new File(s));
                                                                        StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                        imagesRef.putFile(file);
                                                                    }
                                                                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                                    for (File f : storageDir.listFiles()) {
                                                                        deleteRecursive(f);
                                                                    }
                                                                    finish();
                                                                }
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        });
                            } else if (getIntent().getStringExtra("MODE").equals("add")) {
                                Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                                db.collection("Item")
                                        .add(item)
                                        .addOnSuccessListener(documentReference -> {
                                            itemId = documentReference.getId();
                                            PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
                                            db.collection("PantryItem")
                                                    .add(pantryItem)
                                                    .addOnSuccessListener(documentReference13 -> {
                                                        for (StoreViewAddItem store : storeViewAddItems) {
                                                            if (store.isChecked) {
                                                                item.stores.put(store.storeId, store.price);
                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                db.collection("StoreItem").add(storeItem);
                                                                db.collection("StoreList").document(store.storeId)
                                                                        .get(source)
                                                                        .addOnCompleteListener(task1814 -> {
                                                                            if (task1814.isSuccessful()) {
                                                                                DocumentSnapshot document1612 = task1814.getResult();
                                                                                if (document1612.exists()) {
                                                                                    StoreList storeList = document1612.toObject(StoreList.class);
                                                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                }
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                        db.collection("Item").document(itemId).update("stores", item.stores);
                                                        for (String s : photoPaths) {
                                                            Uri file = Uri.fromFile(new File(s));
                                                            StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                            imagesRef.putFile(file);
                                                        }
                                                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                        for (File f : storageDir.listFiles()) {
                                                            deleteRecursive(f);
                                                        }
                                                        db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                                                                .get(source)
                                                                .addOnCompleteListener(task112 -> {
                                                                    if (task112.isSuccessful()) {
                                                                        DocumentSnapshot document = task112.getResult();
                                                                        if (document.exists()) {
                                                                            PantryList pantry = document.toObject(PantryList.class);
                                                                            db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                                                        }
                                                                    }
                                                                });
                                                        finish();
                                                    });
                                        });
                            }
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    Item item = document.toObject(Item.class);
                                    itemId = document.getId();
                                    if (getIntent().getStringExtra("MODE").equals("update")) {
                                        db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).whereNotEqualTo(FieldPath.documentId(), getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task2030 -> {
                                            if (task2030.isSuccessful()) {
                                                if (task2030.getResult().size() != 0) {
                                                    for (QueryDocumentSnapshot document2030 : task2030.getResult()) {
                                                        db.collection("PantryItem").whereEqualTo("pantryId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", document2030.getId()).get(source).addOnCompleteListener(task2031 -> {
                                                            if (task2031.isSuccessful()) {
                                                                if (task2031.getResult().size() != 0) {
                                                                    for (QueryDocumentSnapshot document2031 : task2031.getResult()) {
                                                                        Toast.makeText(this, R.string.productSameBarcodeAlreadyList, Toast.LENGTH_LONG).show();
                                                                        return;
                                                                    }
                                                                } else {
                                                                    auxUpdateItemPantryFunction(item);
                                                                }
                                                            } else {
                                                                auxUpdateItemPantryFunction(item);
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    auxUpdateItemPantryFunction(item);
                                                }
                                            } else {
                                                auxUpdateItemPantryFunction(item);
                                            }
                                        });
                                    } else if (getIntent().getStringExtra("MODE").equals("add")) {
                                        db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).get(source).addOnCompleteListener(task2030 -> {
                                            if (task2030.isSuccessful()) {
                                                if (task2030.getResult().size() != 0) {
                                                    for (QueryDocumentSnapshot document2030 : task2030.getResult()) {
                                                        db.collection("PantryItem").whereEqualTo("pantryId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", document2030.getId()).get(source).addOnCompleteListener(task2031 -> {
                                                            if (task2031.isSuccessful()) {
                                                                if (task2031.getResult().size() != 0) {
                                                                    for (QueryDocumentSnapshot document2031 : task2031.getResult()) {
                                                                        Toast.makeText(this, R.string.productSameBarcodeAlreadyList, Toast.LENGTH_LONG).show();
                                                                        return;
                                                                    }
                                                                } else {
                                                                    auxAddItemPantryFunction(item);
                                                                }
                                                            } else {
                                                                auxAddItemPantryFunction(item);
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    auxAddItemPantryFunction(item);
                                                }
                                            } else {
                                                auxAddItemPantryFunction(item);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    } else {
                        if (getIntent().getStringExtra("MODE").equals("update")) {
                            db.collection("Item")
                                    .document(getIntent().getStringExtra("ItemId"))
                                    .get(source)
                                    .addOnCompleteListener(task13 -> {
                                        if (task13.isSuccessful()) {
                                            DocumentSnapshot document = task13.getResult();
                                            if (document.exists()) {
                                                Item item = document.toObject(Item.class);
                                                itemId = getIntent().getStringExtra("ItemId");
                                                item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
                                                item.barcode = barcodeNumber.getText().toString();
                                                item.stores.clear();
                                                db.collection("Item").document(itemId).update("users", item.users, "barcode", item.barcode);
                                                db.collection("PantryItem").whereEqualTo("pantryId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task12 -> {
                                                    if (task12.isSuccessful()) {
                                                        if (task12.getResult().size() != 0) {
                                                            for (QueryDocumentSnapshot document1 : task12.getResult()) {
                                                                PantryItem pantryItem = document1.toObject(PantryItem.class);
                                                                db.collection("PantryItem").document(document1.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()), "idealQuantity", Integer.parseInt(targetQuantity.getText().toString()));
                                                                for (StoreViewAddItem store : storeViewAddItems) {
                                                                    if (store.isChecked) {
                                                                        item.stores.put(store.storeId, store.price);
                                                                        db.collection("StoreItem").whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task121 -> {
                                                                            if (task121.isSuccessful()) {
                                                                                if (task121.getResult().size() != 0) {
                                                                                    for (QueryDocumentSnapshot document11 : task121.getResult()) {
                                                                                        StoreItem storeItem = document11.toObject(StoreItem.class);
                                                                                        int oldQuantity = pantryItem.idealQuantity - pantryItem.quantity;
                                                                                        int newQuantity = storeItem.quantity - oldQuantity + (Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                        if (newQuantity < 0) {
                                                                                            newQuantity = 0;
                                                                                        }
                                                                                        db.collection("StoreItem").document(document11.getId()).update("quantity", newQuantity);
                                                                                    }
                                                                                } else {
                                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                    db.collection("StoreItem").add(storeItem);
                                                                                    db.collection("StoreList").document(store.storeId)
                                                                                            .get(source)
                                                                                            .addOnCompleteListener(task1814 -> {
                                                                                                if (task1814.isSuccessful()) {
                                                                                                    DocumentSnapshot document1612 = task1814.getResult();
                                                                                                    if (document1612.exists()) {
                                                                                                        StoreList storeList = document1612.toObject(StoreList.class);
                                                                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                                    }
                                                                                                }
                                                                                            });
                                                                                }
                                                                            } else {
                                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                db.collection("StoreItem").add(storeItem);
                                                                                db.collection("StoreList").document(store.storeId)
                                                                                        .get(source)
                                                                                        .addOnCompleteListener(task1814 -> {
                                                                                            if (task1814.isSuccessful()) {
                                                                                                DocumentSnapshot document1612 = task1814.getResult();
                                                                                                if (document1612.exists()) {
                                                                                                    StoreList storeList = document1612.toObject(StoreList.class);
                                                                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                                db.collection("Item").document(itemId).update("stores", item.stores);
                                                                for (String s : photoPaths) {
                                                                    Uri file = Uri.fromFile(new File(s));
                                                                    StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                                    imagesRef.putFile(file);
                                                                }
                                                                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                                for (File f : storageDir.listFiles()) {
                                                                    deleteRecursive(f);
                                                                }
                                                                finish();
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                        } else if (getIntent().getStringExtra("MODE").equals("add")) {
                            Item item = new Item(name.getText().toString(), barcodeNumber.getText().toString(), mAuth.getCurrentUser().getUid());
                            db.collection("Item")
                                    .add(item)
                                    .addOnSuccessListener(documentReference -> {
                                        itemId = documentReference.getId();
                                        PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
                                        db.collection("PantryItem")
                                                .add(pantryItem)
                                                .addOnSuccessListener(documentReference1 -> {
                                                    for (StoreViewAddItem store : storeViewAddItems) {
                                                        if (store.isChecked) {
                                                            item.stores.put(store.storeId, store.price);
                                                        }
                                                        db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).get(source).addOnCompleteListener(task20 -> {
                                                            if (task20.isSuccessful()) {
                                                                if (task20.getResult().size() != 0) {
                                                                    for (QueryDocumentSnapshot document20 : task20.getResult()) {
                                                                        db.collection("StoreItem").whereEqualTo("itemId", document20.getId()).get(source).addOnCompleteListener(task19 -> {
                                                                            if (task19.isSuccessful()) {
                                                                                if (task19.getResult().size() != 0) {
                                                                                    for (QueryDocumentSnapshot document17 : task19.getResult()) {
                                                                                        StoreItem storeItem = document17.toObject(StoreItem.class);
                                                                                        int newQuantity = storeItem.quantity + (Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                        if (newQuantity < 0) {
                                                                                            newQuantity = 0;
                                                                                        }
                                                                                        db.collection("StoreItem").document(document17.getId()).update("quantity", newQuantity);
                                                                                    }
                                                                                } else {
                                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                    db.collection("StoreItem").add(storeItem);
                                                                                    db.collection("StoreList").document(store.storeId)
                                                                                            .get(source)
                                                                                            .addOnCompleteListener(task1814 -> {
                                                                                                if (task1814.isSuccessful()) {
                                                                                                    DocumentSnapshot document1612 = task1814.getResult();
                                                                                                    if (document1612.exists()) {
                                                                                                        StoreList storeList = document1612.toObject(StoreList.class);
                                                                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                                    }
                                                                                                }
                                                                                            });
                                                                                }
                                                                            } else {
                                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                                db.collection("StoreItem").add(storeItem);
                                                                                db.collection("StoreList").document(store.storeId)
                                                                                        .get(source)
                                                                                        .addOnCompleteListener(task1814 -> {
                                                                                            if (task1814.isSuccessful()) {
                                                                                                DocumentSnapshot document1612 = task1814.getResult();
                                                                                                if (document1612.exists()) {
                                                                                                    StoreList storeList = document1612.toObject(StoreList.class);
                                                                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        });
                                                                    }
                                                                } else {
                                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                    db.collection("StoreItem").add(storeItem);
                                                                    db.collection("StoreList").document(store.storeId)
                                                                            .get(source)
                                                                            .addOnCompleteListener(task1814 -> {
                                                                                if (task1814.isSuccessful()) {
                                                                                    DocumentSnapshot document1612 = task1814.getResult();
                                                                                    if (document1612.exists()) {
                                                                                        StoreList storeList = document1612.toObject(StoreList.class);
                                                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            } else {
                                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                                db.collection("StoreItem").add(storeItem);
                                                                db.collection("StoreList").document(store.storeId)
                                                                        .get(source)
                                                                        .addOnCompleteListener(task1814 -> {
                                                                            if (task1814.isSuccessful()) {
                                                                                DocumentSnapshot document1612 = task1814.getResult();
                                                                                if (document1612.exists()) {
                                                                                    StoreList storeList = document1612.toObject(StoreList.class);
                                                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                                }
                                                                            }
                                                                        });
                                                            }
                                                        });
                                                    }
                                                    db.collection("Item").document(itemId).update("stores", item.stores);
                                                    for (String s : photoPaths) {
                                                        Uri file = Uri.fromFile(new File(s));
                                                        StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                        imagesRef.putFile(file);
                                                    }
                                                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                                    for (File f : storageDir.listFiles()) {
                                                        deleteRecursive(f);
                                                    }
                                                    db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                                                            .get(source)
                                                            .addOnCompleteListener(task1 -> {
                                                                if (task1.isSuccessful()) {
                                                                    DocumentSnapshot document = task1.getResult();
                                                                    if (document.exists()) {
                                                                        PantryList pantry = document.toObject(PantryList.class);
                                                                        db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                                                    }
                                                                }
                                                            });
                                                    finish();
                                                });
                                    });
                        }
                    }
                });
            }
        }

    }

    void deleteRecursive(File fileOrDirectory) {
        if (!fileOrDirectory.isDirectory()) {
            fileOrDirectory.delete();
        }
    }

    public void onClickAddPictures() {
        Intent intent = new Intent(this, AddPicturesActivity.class);
        intent.putStringArrayListExtra("PATHS", photoPaths);
        intent.putExtra("MODE", "add");
        intent.putExtra("ID", barcodeNumber.getText().toString());
        picturesResultLauncher.launch(intent);
    }

    public void onClickAddStores() {
        Intent intent = new Intent(this, AddStoresActivity.class);
        if (getIntent().getStringExtra("MODE").equals("update")) {
            intent.putExtra("MODE", "update");
            intent.putExtra("ID", getIntent().getStringExtra("ItemId"));
            db.collection("StoreList")
                    .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                    .get(source)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                StoreList store = document.toObject(StoreList.class);
                                if (!storeViewAddItems.isEmpty()) {
                                    boolean present = false;
                                    for (StoreViewAddItem item : storeViewAddItems) {
                                        if (store.latitude != null && store.longitude != null && item.latitude != null && item.longitude != null) {
                                            float[] results = new float[1];
                                            Location.distanceBetween(Double.parseDouble(store.latitude), Double.parseDouble(store.longitude),
                                                    Double.parseDouble(item.latitude), Double.parseDouble(item.longitude),
                                                    results);
                                            //Less than 20 meters
                                            if (results[0] < 20f) {
                                                present = true;
                                            }
                                        }
                                        else if (item.storeId.equals(document.getId())) {
                                            present = true;
                                        }
                                    }
                                    if (!present) {
                                        StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f, false);
                                        storeViewAddItem.latitude = store.latitude;
                                        storeViewAddItem.longitude = store.longitude;
                                        storeViewAddItems.add(storeViewAddItem);
                                    }

                                } else {
                                    StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f, false);
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
        } else if (getIntent().getStringExtra("MODE").equals("add")) {
            intent.putExtra("MODE", "add");
            db.collection("StoreList")
                    .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                    .get(source)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                StoreList store = document.toObject(StoreList.class);
                                if (!storeViewAddItems.isEmpty()) {
                                    boolean present = false;
                                    for (StoreViewAddItem item : storeViewAddItems) {
                                        if (store.latitude != null && store.longitude != null && item.latitude != null && item.longitude != null) {
                                            float[] results = new float[1];
                                            Location.distanceBetween(Double.parseDouble(store.latitude), Double.parseDouble(store.longitude),
                                                    Double.parseDouble(item.latitude), Double.parseDouble(item.longitude),
                                                    results);
                                            //Less than 20 meters
                                            if (results[0] < 20f) {
                                                present = true;
                                            }
                                        }
                                        else if (item.storeId.equals(document.getId())) {
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
    }

    void auxUpdateItemPantryFunction(Item item) {
        item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
        db.collection("Item").document(itemId).update("users", item.users);
        db.collection("PantryItem").whereEqualTo("pantryId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task111 -> {
            if (task111.isSuccessful()) {
                if (task111.getResult().size() != 0) {
                    for (QueryDocumentSnapshot document19 : task111.getResult()) {
                        PantryItem pantryItem = document19.toObject(PantryItem.class);
                        db.collection("PantryItem").document(document19.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()), "idealQuantity", Integer.parseInt(targetQuantity.getText().toString()), "itemId", itemId);
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
                                            db.collection("StoreList").document(s).get(source).addOnCompleteListener(task110 -> {
                                                if (task110.isSuccessful()) {
                                                    DocumentSnapshot document18 = task110.getResult();
                                                    if (document18.exists()) {
                                                        StoreList sl = document18.toObject(StoreList.class);
                                                        float[] results = new float[1];
                                                        if (store.latitude != null && store.longitude != null && sl.latitude != null && sl.longitude != null) {
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
                                    db.collection("Item").document(itemId).update("stores", item.stores);
                                }
                                db.collection("StoreItem").whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task19 -> {
                                    if (task19.isSuccessful()) {
                                        if (task19.getResult().size() != 0) {
                                            for (QueryDocumentSnapshot document17 : task19.getResult()) {
                                                StoreItem storeItem = document17.toObject(StoreItem.class);
                                                int oldQuantity = pantryItem.idealQuantity - pantryItem.quantity;
                                                int newQuantity = storeItem.quantity - oldQuantity + (Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                if (newQuantity < 0) {
                                                    newQuantity = 0;
                                                }
                                                db.collection("StoreItem").document(document17.getId()).update("quantity", newQuantity, "itemId", itemId);
                                            }
                                        } else {
                                            StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                            db.collection("StoreItem").add(storeItem);
                                            db.collection("StoreList").document(store.storeId)
                                                    .get(source)
                                                    .addOnCompleteListener(task1814 -> {
                                                        if (task1814.isSuccessful()) {
                                                            DocumentSnapshot document1612 = task1814.getResult();
                                                            if (document1612.exists()) {
                                                                StoreList storeList = document1612.toObject(StoreList.class);
                                                                db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                            }
                                                        }
                                                    });
                                        }
                                    } else {
                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                        db.collection("StoreItem").add(storeItem);
                                        db.collection("StoreList").document(store.storeId)
                                                .get(source)
                                                .addOnCompleteListener(task1814 -> {
                                                    if (task1814.isSuccessful()) {
                                                        DocumentSnapshot document1612 = task1814.getResult();
                                                        if (document1612.exists()) {
                                                            StoreList storeList = document1612.toObject(StoreList.class);
                                                            db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                        }
                                                    }
                                                });
                                    }
                                });
                            }
                        }
                        for (String s : photoPaths) {
                            Uri file = Uri.fromFile(new File(s));
                            StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                            imagesRef.putFile(file);
                        }
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        for (File f : storageDir.listFiles()) {
                            deleteRecursive(f);
                        }
                        finish();
                    }
                }
            }
        });
    }

    void auxAddItemPantryFunction(Item item) {
        item.users.put(mAuth.getCurrentUser().getUid(), name.getText().toString());
        db.collection("Item").document(itemId).update("users", item.users);
        PantryItem pantryItem = new PantryItem(getIntent().getStringExtra("ID"), itemId, Integer.parseInt(pantryQuantity.getText().toString()), Integer.parseInt(targetQuantity.getText().toString()));
        db.collection("PantryItem")
                .add(pantryItem)
                .addOnSuccessListener(documentReference -> {
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
                                        db.collection("StoreList").document(s).get(source).addOnCompleteListener(task15 -> {
                                            if (task15.isSuccessful()) {
                                                DocumentSnapshot document13 = task15.getResult();
                                                if (document13.exists()) {
                                                    StoreList sl = document13.toObject(StoreList.class);
                                                    float[] results = new float[1];
                                                    if (store.latitude != null && store.longitude != null && sl.latitude != null && sl.longitude != null) {
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
                                db.collection("Item").document(itemId).update("stores", item.stores);
                            }
                            db.collection("Item").whereEqualTo("barcode", barcodeNumber.getText().toString()).get(source).addOnCompleteListener(task20 -> {
                                if (task20.isSuccessful()) {
                                    if (task20.getResult().size() != 0) {
                                        for (QueryDocumentSnapshot document20 : task20.getResult()) {
                                            db.collection("StoreItem").whereEqualTo("itemId", document20.getId()).get(source).addOnCompleteListener(task19 -> {
                                                if (task19.isSuccessful()) {
                                                    if (task19.getResult().size() != 0) {
                                                        for (QueryDocumentSnapshot document17 : task19.getResult()) {
                                                            StoreItem storeItem = document17.toObject(StoreItem.class);
                                                            int newQuantity = storeItem.quantity + (Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                            if (newQuantity < 0) {
                                                                newQuantity = 0;
                                                            }
                                                            db.collection("StoreItem").document(document17.getId()).update("quantity", newQuantity);
                                                        }
                                                    } else {
                                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                        db.collection("StoreItem").add(storeItem);
                                                        db.collection("StoreList").document(store.storeId)
                                                                .get(source)
                                                                .addOnCompleteListener(task1814 -> {
                                                                    if (task1814.isSuccessful()) {
                                                                        DocumentSnapshot document1612 = task1814.getResult();
                                                                        if (document1612.exists()) {
                                                                            StoreList storeList = document1612.toObject(StoreList.class);
                                                                            db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                } else {
                                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                                    db.collection("StoreItem").add(storeItem);
                                                    db.collection("StoreList").document(store.storeId)
                                                            .get(source)
                                                            .addOnCompleteListener(task1814 -> {
                                                                if (task1814.isSuccessful()) {
                                                                    DocumentSnapshot document1612 = task1814.getResult();
                                                                    if (document1612.exists()) {
                                                                        StoreList storeList = document1612.toObject(StoreList.class);
                                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                                    }
                                                                }
                                                            });
                                                }
                                            });
                                        }
                                    } else {
                                        StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                        db.collection("StoreItem").add(storeItem);
                                        db.collection("StoreList").document(store.storeId)
                                                .get(source)
                                                .addOnCompleteListener(task1814 -> {
                                                    if (task1814.isSuccessful()) {
                                                        DocumentSnapshot document1612 = task1814.getResult();
                                                        if (document1612.exists()) {
                                                            StoreList storeList = document1612.toObject(StoreList.class);
                                                            db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(targetQuantity.getText().toString()) - Integer.parseInt(pantryQuantity.getText().toString()));
                                    db.collection("StoreItem").add(storeItem);
                                    db.collection("StoreList").document(store.storeId)
                                            .get(source)
                                            .addOnCompleteListener(task1814 -> {
                                                if (task1814.isSuccessful()) {
                                                    DocumentSnapshot document1612 = task1814.getResult();
                                                    if (document1612.exists()) {
                                                        StoreList storeList = document1612.toObject(StoreList.class);
                                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                    }
                                                }
                                            });
                                }
                            });
                        }
                    }
                    for (String s : photoPaths) {
                        Uri file = Uri.fromFile(new File(s));
                        StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                        imagesRef.putFile(file);
                    }
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    for (File f : storageDir.listFiles()) {
                        deleteRecursive(f);
                    }
                    db.collection("PantryList").document(getIntent().getStringExtra("ID"))
                            .get(source)
                            .addOnCompleteListener(task14 -> {
                                if (task14.isSuccessful()) {
                                    DocumentSnapshot document12 = task14.getResult();
                                    if (document12.exists()) {
                                        PantryList pantry = document12.toObject(PantryList.class);
                                        db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("number_of_items", pantry.number_of_items + 1);
                                        finish();
                                    }
                                }
                            });
                    finish();
                });
    }

    void auxAddItemStoreFunction(Item item) {
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
                            db.collection("StoreList").document(s).get(source).addOnCompleteListener(task1816 -> {
                                if (task1816.isSuccessful()) {
                                    DocumentSnapshot document1614 = task1816.getResult();
                                    if (document1614.exists()) {
                                        StoreList sl = document1614.toObject(StoreList.class);
                                        float[] results = new float[1];
                                        if (store.latitude != null && store.longitude != null && sl.latitude != null && sl.longitude != null) {
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
                    db.collection("Item").document(itemId).update("stores", item.stores);
                }
                if (getIntent().getStringExtra("MODE").equals("update")) {
                    db.collection("StoreItem").whereEqualTo("storeId", getIntent().getStringExtra("ID")).whereEqualTo("itemId", getIntent().getStringExtra("ItemId")).get(source).addOnCompleteListener(task1815 -> {
                        if (task1815.isSuccessful()) {
                            if (task1815.getResult().size() != 0) {
                                for (QueryDocumentSnapshot document1613 : task1815.getResult()) {
                                    db.collection("StoreItem").document(document1613.getId()).update("quantity", Integer.parseInt(pantryQuantity.getText().toString()), "itemId", itemId);
                                }
                            } else {
                                StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                                db.collection("StoreItem").add(storeItem);
                                db.collection("StoreList").document(store.storeId)
                                        .get(source)
                                        .addOnCompleteListener(task1814 -> {
                                            if (task1814.isSuccessful()) {
                                                DocumentSnapshot document1612 = task1814.getResult();
                                                if (document1612.exists()) {
                                                    StoreList storeList = document1612.toObject(StoreList.class);
                                                    db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                                }
                                            }
                                        });
                            }
                        } else {
                            StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                            db.collection("StoreItem").add(storeItem);
                            db.collection("StoreList").document(store.storeId)
                                    .get(source)
                                    .addOnCompleteListener(task1814 -> {
                                        if (task1814.isSuccessful()) {
                                            DocumentSnapshot document1612 = task1814.getResult();
                                            if (document1612.exists()) {
                                                StoreList storeList = document1612.toObject(StoreList.class);
                                                db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                            }
                                        }
                                    });
                        }
                    });
                } else if (getIntent().getStringExtra("MODE").equals("add")) {
                    StoreItem storeItem = new StoreItem(store.storeId, itemId, Integer.parseInt(pantryQuantity.getText().toString()));
                    db.collection("StoreItem").add(storeItem);
                    db.collection("StoreList").document(store.storeId)
                            .get(source)
                            .addOnCompleteListener(task1814 -> {
                                if (task1814.isSuccessful()) {
                                    DocumentSnapshot document1612 = task1814.getResult();
                                    if (document1612.exists()) {
                                        StoreList storeList = document1612.toObject(StoreList.class);
                                        db.collection("StoreList").document(store.storeId).update("number_of_items", storeList.number_of_items + 1);
                                    }
                                }
                            });
                }
            }
        }
        for (String s : photoPaths) {
            Uri file = Uri.fromFile(new File(s));
            StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
            imagesRef.putFile(file);
        }
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        for (File f : storageDir.listFiles()) {
            deleteRecursive(f);
        }
        finish();
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
                    assert mediaImage != null;
                    InputImage imageInput = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                    scanner.process(imageInput)
                            .addOnSuccessListener(barcodes -> {
                                for (Barcode barcode : barcodes) {
                                    String rawValue = barcode.getRawValue();

                                    // See API reference for complete list of supported types
                                    if (barcodeNumber.getText().toString().matches("")) {
                                        barcodeNumber.setText(rawValue);
                                        storeViewAddItems.clear();
                                        AlertDialog.Builder builder = new AlertDialog.Builder(AddItemActivity.this);
                                        builder.setCancelable(true);
                                        builder.setTitle(R.string.pleaseSubmitPriceDataAndPictures);
                                        builder.setMessage(R.string.ifPossibleSubmitStoresPricesImages);
                                        builder.setPositiveButton(R.string.addPictures, (dialog, which) -> onClickAddPictures());
                                        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
                                            builder.setNeutralButton(R.string.addStores, (dialog, which) -> onClickAddStores());
                                        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                                            targetQuantity.setText("");
                                        }
                                        autocompleteStoreList();
                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Error", "exception", e);
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnCompleteListener(task -> image.close());
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
                                            .get(source)
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    DocumentSnapshot document2 = task2.getResult();
                                                    StoreList item2 = document2.toObject(StoreList.class);
                                                    if (item2.users.contains(mAuth.getCurrentUser().getUid())) {
                                                        StoreViewAddItem storeViewAddItem = new StoreViewAddItem(storeId, item2.name, item.stores.get(storeId), true);
                                                        storeViewAddItem.latitude = item2.latitude;
                                                        storeViewAddItem.longitude = item2.longitude;
                                                        storeViewAddItems.add(storeViewAddItem);
                                                        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                                                            if (document2.getId().equals(getIntent().getStringExtra("ID"))) {
                                                                targetQuantity.setText(String.valueOf(item.stores.get(storeId)));
                                                            }
                                                        }
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
                                                                                    if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                                                                                        if (document3.getId().equals(getIntent().getStringExtra("ID"))) {
                                                                                            targetQuantity.setText(String.valueOf(item.stores.get(storeId)));
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }


                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            });
                                }
                                db.collection("StoreList")
                                        .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                        .get(source)
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                    StoreList store = document2.toObject(StoreList.class);
                                                    if (!storeViewAddItems.isEmpty()) {
                                                        boolean present = false;
                                                        for (StoreViewAddItem item2 : storeViewAddItems) {
                                                            if (store.latitude != null && store.longitude != null && item2.latitude != null && item2.longitude != null) {
                                                                float[] results = new float[1];
                                                                Location.distanceBetween(Double.parseDouble(store.latitude), Double.parseDouble(store.longitude),
                                                                        Double.parseDouble(item2.latitude), Double.parseDouble(item2.longitude),
                                                                        results);
                                                                //Less than 20 meters
                                                                if (results[0] < 20f) {
                                                                    present = true;
                                                                }
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
                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task.getException());
                                            }
                                        });
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    });
        }
    }
}