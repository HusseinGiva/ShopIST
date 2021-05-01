package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
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
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        viewFinder = findViewById(R.id.viewFinder);
        barcodeNumber = findViewById(R.id.barcodeNumber);
        name = findViewById(R.id.productName);
        pantryQuantity = findViewById(R.id.itemPantryQuantity);
        targetQuantity = findViewById(R.id.itemTargetQuantity);
        if (getIntent().getStringExtra("TYPE").equals("STORE")) {
            pantryQuantity.setHint("Store Quantity");
            targetQuantity.setVisibility(View.INVISIBLE);
        }
        addPictures = findViewById(R.id.addPictures);
        picturesResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        photoPaths = result.getData().getStringArrayListExtra("PATHS");
                    }
                });
        addStores = findViewById(R.id.addStoresButton);
        storesResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        storeViewAddItems = result.getData().getParcelableArrayListExtra("STORES");
                    }
                });
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
    public void onBackPressed() {
        finish();
    }

    public void onClickSaveItem(View view) {
        if (name.getText().toString().equals("")) {
            Toast.makeText(this, "Please insert an item name.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (targetQuantity.getText().toString().equals("") || targetQuantity.getText().toString().equals("0")) {
            Toast.makeText(this, "Please insert an item target quantity.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pantryQuantity.getText().toString().equals("") || pantryQuantity.getText().toString().equals("0")) {
            Toast.makeText(this, "Please insert an item target quantity.", Toast.LENGTH_SHORT).show();
            return;
        }
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
                                        for (StoreViewAddItem store: storeViewAddItems) {
                                            if (store.isChecked) {
                                                StoreItem storeItem = new StoreItem(store.storeId, itemId, 0, store.price);
                                                db.collection("StoreItem").add(storeItem);
                                            }
                                        }
                                        for (String s: photoPaths) {
                                            Uri file = Uri.fromFile(new File(s));
                                            if (!barcodeNumber.getText().toString().equals("")) {
                                                StorageReference imagesRef = storageRef.child(barcodeNumber.getText().toString() + "/" + file.getLastPathSegment());
                                                UploadTask uploadTask = imagesRef.putFile(file);
                                            }
                                            else {
                                                StorageReference imagesRef = storageRef.child(itemId + "/" + file.getLastPathSegment());
                                                UploadTask uploadTask = imagesRef.putFile(file);
                                            }
                                        }
                                        finish();
                                        //Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //Log.w(TAG, "Error adding document", e);
                                    }
                                });
                        //Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    public void onClickAddPictures(View view) {
        Intent intent = new Intent(this, AddPicturesActivity.class);
        intent.putStringArrayListExtra("PATHS", photoPaths);
        picturesResultLauncher.launch(intent);
    }

    public void onClickAddStores(View view) {
        Intent intent = new Intent(this, AddStoresActivity.class);
        db.collection("StoreList")
                .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            StoreList store = document.toObject(StoreList.class);
                            if (!storeViewAddItems.isEmpty()) {
                                Boolean present = false;
                                for (StoreViewAddItem item: storeViewAddItems) {
                                    if (item.name.equals(store.name)) {
                                        present = true;
                                    }
                                }
                                if (!present) {
                                    StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f);
                                    storeViewAddItems.add(storeViewAddItem);
                                }

                            }
                            else {
                                StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document.getId(), store.name, 0f);
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
                                            autocompleteStoreList();
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
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Item item = document.toObject(Item.class);
                                if (item.users.contains(mAuth.getCurrentUser().getUid())) {
                                    name.setText(item.name);
                                }
                                db.collection("StoreItem")
                                        .whereEqualTo("itemId", document.getId())
                                        .get()
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                    StoreItem item2 = document2.toObject(StoreItem.class);
                                                    db.collection("StoreList")
                                                            .document(item2.storeId)
                                                            .get()
                                                            .addOnCompleteListener(task3 -> {
                                                                if (task3.isSuccessful()) {
                                                                    DocumentSnapshot document3 = task3.getResult();
                                                                        Toast.makeText(getApplicationContext(), document3.getId(), Toast.LENGTH_LONG).show();
                                                                        StoreList item3 = document3.toObject(StoreList.class);
                                                                        db.collection("StoreList")
                                                                                .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                                                                .get()
                                                                                .addOnCompleteListener(task4 -> {
                                                                                    if (task4.isSuccessful()) {
                                                                                        for (QueryDocumentSnapshot document4 : task4.getResult()) {
                                                                                            StoreList item4 = document4.toObject(StoreList.class);
                                                                                            float[] results = new float[1];
                                                                                            Location.distanceBetween(item4.latitude, item4.longitude, item3.latitude, item3.longitude,
                                                                                                    results);
                                                                                            //Less than 20 meters
                                                                                            if (results[0] < 20f) {
                                                                                                StoreViewAddItem storeViewAddItem = new StoreViewAddItem(document4.getId(), item4.name, item2.price, true);
                                                                                                storeViewAddItems.add(storeViewAddItem);
                                                                                            }
                                                                                        }
                                                                                    } else {
                                                                                        Log.d("TAG", "Error getting documents: ", task4.getException());
                                                                                    }
                                                                                });
                                                                    }
                                                                else {
                                                                    Log.d("TAG", "Error getting documents: ", task3.getException());
                                                                }
                                                            });
                                                }
                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task2.getException());
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