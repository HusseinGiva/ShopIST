package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class QrCodeScanner extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;
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
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Programmatically initialize the scanner view
        mScannerView = new ZXingScannerView(this);
        // Set the scanner view as the content view
        setContentView(mScannerView);

        if (isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;
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


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (splitted.length > 1) {
            String type = splitted[0];
            String id = splitted[1];
            if (type.equals("PANTRY")) {

                if (!isConnected(getApplicationContext()))
                    Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

                db.collection("PantryList").document(id).update("users", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid())).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(QrCodeScanner.this, PantryListActivity.class);
                        intent.putExtra("ID", id);
                        intent.putExtra("SENDER", "start");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, R.string.invalidQRCode, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, AddListActivity.class);
                        intent.putExtra("MODE", "add");
                        startActivity(intent);
                    }
                    finish();
                });
                return;
            } else if (type.equals("STORE")) {

                if (!isConnected(getApplicationContext()))
                    Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

                db.collection("StoreList").document(id).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        DocumentSnapshot document = task.getResult();

                        if(document.exists()){
                            StoreList s = document.toObject(StoreList.class);

                            StoreList newStore = new StoreList(s.name, s.latitude, s.longitude, mAuth.getCurrentUser().getUid());

                            db.collection("StoreList").add(newStore).addOnSuccessListener(documentReference -> {
                                Intent intent = new Intent(QrCodeScanner.this, StoreListActivity.class);
                                intent.putExtra("ID", documentReference.getId());
                                intent.putExtra("SENDER", "start");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);

                            });

                        }else{
                            Toast.makeText(this, R.string.invalidQRCode, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, AddListActivity.class);
                            intent.putExtra("MODE", "add");
                            startActivity(intent);
                        }


                    } else {
                        Toast.makeText(this, R.string.invalidQRCode, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, AddListActivity.class);
                        intent.putExtra("MODE", "add");
                        startActivity(intent);
                    }
                    finish();
                });

                return;

            }
        }

        Toast.makeText(this, R.string.invalidQRCode, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, AddListActivity.class);
        intent.putExtra("MODE", "add");
        startActivity(intent);
        finish();


    }
}