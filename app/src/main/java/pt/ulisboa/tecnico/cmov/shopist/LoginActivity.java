package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

/*import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;*/

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private EditText email;
    private EditText password;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar myToolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //Termite Test - DELETE LATER
        /*SimWifiP2pBroadcast a = new SimWifiP2pBroadcast();
        SimWifiP2pManager mManager = null;
        SimWifiP2pManager.Channel mChanell = null;
        SimWifiP2pSocketServer mSrvSocket = null;
        SimWifiP2pSocket mCliSocket = null;*/

        email = findViewById(R.id.emailTextBox);
        password = findViewById(R.id.passwordTextBox);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    public void onClickSignUp(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void onClickNoAccount(View view) {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInAnonymously:success");
                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(LoginActivity.this, R.string.somethingWentWrongNoInternetConnection,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void onClickLogin(View view) {

        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();

        //Check if fields are empty
        if (emailText.trim().isEmpty() || passwordText.trim().isEmpty()) {
            Toast.makeText(this, R.string.pleaseFillRequiredFields, Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, R.string.authenticationFailed,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void updateUI() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            updateUI();
        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.d("ADD_LIST", "Latitude : " + location.getLatitude() + ", Longitude : " +
                                    location.getLongitude());

                            final boolean[] found = {false};
                            final int[] loaded = {2};


                            ArrayList<String> pantries = new ArrayList<>();
                            ArrayList<String> stores = new ArrayList<>();

                            db.collection("PantryList")
                                    .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    PantryList p = document.toObject(PantryList.class);

                                                    float[] results = new float[1];
                                                    Location.distanceBetween(p.latitude, p.longitude, location.getLatitude(), location.getLongitude(),
                                                            results);

                                                    //Less than 20 meters
                                                    if (results[0] < 20f) {

                                                        pantries.add(document.getId());


                                                    }
                                                }
                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task.getException());
                                            }
                                            loaded[0]--;
                                        }
                                    });

                            db.collection("StoreList")
                                    .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    StoreList s = document.toObject(StoreList.class);

                                                    float[] results = new float[1];
                                                    Location.distanceBetween(s.latitude, s.longitude, location.getLatitude(), location.getLongitude(),
                                                            results);

                                                    //Less than 20 meters
                                                    if (results[0] < 20f) {
                                                        stores.add(document.getId());

                                                    }
                                                }
                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task.getException());
                                            }
                                            loaded[0]--;
                                        }
                                    });


                            Handler timerHandler = new Handler();
                            Runnable timerRunnable = new Runnable() {

                                @Override
                                public void run() {
                                    if (loaded[0] == 0) {

                                        if (pantries.size() + stores.size() == 1) {

                                            if (pantries.size() == 1) {


                                                LoginActivity.this.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        Intent intent = new Intent(LoginActivity.this, PantryListActivity.class);
                                                        intent.putExtra("TAB", getResources().getString(R.string.pantry));
                                                        intent.putExtra("ID", pantries.get(0));
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });


                                            } else if (stores.size() == 1) {

                                                LoginActivity.this.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        Intent intent = new Intent(LoginActivity.this, PantryListActivity.class);
                                                        intent.putExtra("TAB", getResources().getString(R.string.store));
                                                        intent.putExtra("ID", stores.get(0));
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });

                                            }

                                        } else {
                                            LoginActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            });

                                        }

                                        timerHandler.removeCallbacks(this);
                                    } else {
                                        timerHandler.postDelayed(this, 100);
                                    }
                                }
                            };
                            timerHandler.postDelayed(timerRunnable, 0);


                        } else {
                            LoginActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    });
        }
    }
}