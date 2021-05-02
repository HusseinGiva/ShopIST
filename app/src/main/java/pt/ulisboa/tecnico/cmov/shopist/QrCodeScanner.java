package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrCodeScanner extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Programmatically initialize the scanner view
        mScannerView = new ZXingScannerView(this);
        // Set the scanner view as the content view
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register ourselves as a handler for scan results.
        mScannerView.setResultHandler(this);
        // Start camera on resume
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop camera on pause
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {

        String[] splitted = rawResult.getText().split("_");


        db = FirebaseFirestore.getInstance();
        mAuth =  FirebaseAuth.getInstance();

        if(splitted.length > 1) {
            String type = splitted[0];
            String id = splitted[1];
            if (type.equals(getResources().getString(R.string.pantry))) {
                db.collection("PantryList").document(id).update("users", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid()));

                Intent intent = new Intent(this, PantryListActivity.class);
                intent.putExtra("TAB", type);
                intent.putExtra("ID", id);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            } else if (type.equals(getResources().getString(R.string.store))) {
                db.collection("StoreList").document(id).update("users", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid()));

                Intent intent = new Intent(this, PantryListActivity.class);
                intent.putExtra("TAB", type);
                intent.putExtra("ID", id);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        }

        Toast.makeText(this, R.string.invalidQRCode, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, AddListActivity.class);
        startActivity(intent);
        finish();


    }
}