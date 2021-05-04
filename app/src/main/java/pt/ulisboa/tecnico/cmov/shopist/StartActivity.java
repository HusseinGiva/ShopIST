package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Locale;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class StartActivity extends AppCompatActivity {

    private static final String TAG = "START";
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private FirebaseFirestore db;
    private Source source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar myToolbar = findViewById(R.id.startToolbar);
        setSupportActionBar(myToolbar);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if(isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //Termite Test - DELETE LATER
        SimWifiP2pBroadcast a = new SimWifiP2pBroadcast();
        SimWifiP2pManager mManager = null;
        SimWifiP2pManager.Channel mChanell = null;
        SimWifiP2pSocketServer mSrvSocket = null;
        SimWifiP2pSocket mCliSocket = null;


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }else{


            Task<Location> locationResult = fusedLocationClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            updateUI();
                        } else {
                            Intent intent = new Intent(StartActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Log.d("ADD_LIST", "Current location is null. Using defaults.");
                    }
                }
            });


        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Task<Location> locationResult = fusedLocationClient.getLastLocation();
                    locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser currentUser = mAuth.getCurrentUser();
                                if (currentUser != null) {
                                    updateUI();
                                } else {
                                    Intent intent = new Intent(StartActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                Log.d("ADD_LIST", "Current location is null. Using defaults.");
                            }
                        }
                    });
                }
            } else {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                Intent intent;
                if (currentUser != null) {
                    intent = new Intent(StartActivity.this, HomeActivity.class);
                } else {
                    intent = new Intent(StartActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
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
                                    .get(source)
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    PantryList p = document.toObject(PantryList.class);

                                                    if(p.latitude != null && p.longitude != null){
                                                        float[] results = new float[1];
                                                        Location.distanceBetween(Double.parseDouble(p.latitude), Double.parseDouble(p.longitude), location.getLatitude(), location.getLongitude(),
                                                                results);

                                                        //Less than 20 meters
                                                        if (results[0] < 20f) {

                                                            pantries.add(document.getId());


                                                        }
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
                                    .get(source)
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    StoreList s = document.toObject(StoreList.class);

                                                    if(s.latitude != null && s.longitude != null){
                                                        float[] results = new float[1];
                                                        Location.distanceBetween(Double.parseDouble(s.latitude), Double.parseDouble(s.longitude), location.getLatitude(), location.getLongitude(),
                                                                results);

                                                        //Less than 20 meters
                                                        if (results[0] < 20f) {
                                                            stores.add(document.getId());

                                                        }
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

                                                StartActivity.this.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        db.collection("user").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                                                            if (task.isSuccessful()) {

                                                                DocumentSnapshot document = task.getResult();
                                                                if (document.exists()) {
                                                                    SharedPreferences sharedPref = getSharedPreferences("language", Context.MODE_PRIVATE);
                                                                    SharedPreferences.Editor editor = sharedPref.edit();
                                                                    String language = document.getData().get("language").toString();
                                                                    editor.putString("language", language);
                                                                    editor.commit();
                                                                    Intent intent = new Intent(StartActivity.this, PantryListActivity.class);
                                                                    intent.putExtra("TAB", getResources().getString(R.string.pantry));
                                                                    intent.putExtra("ID", pantries.get(0));
                                                                    startActivity(intent);
                                                                    finish();
                                                                }

                                                            }
                                                        });
                                                    }
                                                });


                                            } else if (stores.size() == 1) {
                                                StartActivity.this.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        db.collection("user").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                                                            if (task.isSuccessful()) {
                                                                DocumentSnapshot document = task.getResult();
                                                                if (document.exists()) {
                                                                    SharedPreferences sharedPref = getSharedPreferences("language", Context.MODE_PRIVATE);
                                                                    SharedPreferences.Editor editor = sharedPref.edit();
                                                                    String language = document.getData().get("language").toString();
                                                                    editor.putString("language", language);
                                                                    editor.commit();
                                                                    Intent intent = new Intent(StartActivity.this, StoreListActivity.class);
                                                                    intent.putExtra("TAB", getResources().getString(R.string.store));
                                                                    intent.putExtra("ID", stores.get(0));
                                                                    startActivity(intent);
                                                                    finish();
                                                                }
                                                            }
                                                        });
                                                    }
                                                });

                                            }

                                        } else {
                                            StartActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    db.collection("user").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            DocumentSnapshot document = task.getResult();
                                                            if (document.exists()) {
                                                                SharedPreferences sharedPref = getSharedPreferences("language", Context.MODE_PRIVATE);
                                                                SharedPreferences.Editor editor = sharedPref.edit();
                                                                String language = document.getData().get("language").toString();
                                                                editor.putString("language", language);
                                                                editor.commit();
                                                                Intent intent = new Intent(StartActivity.this, HomeActivity.class);
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        }
                                                    });
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
                            StartActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    db.collection("user").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                SharedPreferences sharedPref = getSharedPreferences("language", Context.MODE_PRIVATE);
                                                SharedPreferences.Editor editor = sharedPref.edit();
                                                String language = document.getData().get("language").toString();
                                                editor.putString("language", language);
                                                editor.commit();
                                                Intent intent = new Intent(StartActivity.this, HomeActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
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
}