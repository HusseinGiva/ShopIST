package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
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

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AddItemActivity extends AppCompatActivity {

    PreviewView viewFinder;
    EditText barcodeNumber;
    Button clearBarcode;
    Button addPictures;
    Button saveItem;
    EditText name;
    EditText quantity;
    ArrayList<String> photoPaths = new ArrayList<>();
    ActivityResultLauncher<Intent> picturesResultLauncher;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);
        Toolbar myToolbar = findViewById(R.id.addItemToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        viewFinder = findViewById(R.id.viewFinder);
        barcodeNumber = findViewById(R.id.barcodeNumber);
        name = findViewById(R.id.productName);
        quantity = findViewById(R.id.productQuantity);
        addPictures = findViewById(R.id.addPictures);
        picturesResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        photoPaths = result.getData().getStringArrayListExtra("PATHS");
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
        Intent intent = new Intent(this, AddPicturesActivity.class);
        startActivity(intent);
    }

    public void onClickAddPictures(View view) {
        Intent intent = new Intent(this, AddPicturesActivity.class);
        intent.putStringArrayListExtra("PATHS", photoPaths);
        picturesResultLauncher.launch(intent);
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
}