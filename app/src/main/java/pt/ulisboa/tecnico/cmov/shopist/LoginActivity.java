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
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DecimalFormat;

import pt.ulisboa.tecnico.cmov.shopist.persistence.AppDatabase;
import pt.ulisboa.tecnico.cmov.shopist.persistence.GlobalClass;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryWithItems;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreWithItems;

/*import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;*/

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "MAIN" ;
    private FirebaseAuth mAuth;

    private Location lastKnownLocation = null;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private EditText email;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //Termite Test - DELETE LATER
        /*SimWifiP2pBroadcast a = new SimWifiP2pBroadcast();
        SimWifiP2pManager mManager = null;
        SimWifiP2pManager.Channel mChanell = null;
        SimWifiP2pSocketServer mSrvSocket = null;
        SimWifiP2pSocket mCliSocket = null;*/

        email =  (EditText) findViewById(R.id.emailTextBox);
        password =  (EditText) findViewById(R.id.passwordTextBox);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
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
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Something went wrong. Please make sure you have Internet Connection.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    public void onClickLogin(View view) {

        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();

        //Check if fields are empty
        if(emailText.trim().isEmpty() || passwordText.trim().isEmpty()){
            Toast.makeText(this, "Please fill in the required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });

    }

    private void updateUI(FirebaseUser user) {
        if(user != null){

            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "database-name").build();
            GlobalClass globalVariable = (GlobalClass) getApplicationContext();
            globalVariable.setUp(db);

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                Log.d("ADD_LIST", "Latitude : " + lastKnownLocation.getLatitude() + ", Longitude : " +
                                        lastKnownLocation.getLongitude());




                                Handler timerHandler = new Handler();
                                Runnable timerRunnable = new Runnable() {

                                    @Override
                                    public void run() {
                                        if (globalVariable.getLoaded() == 0) {

                                            boolean found = false;
                                            int i = 0;
                                            for(PantryWithItems p : globalVariable.getPantryWithItems()){
                                                float[] results = new float[1];
                                                Location.distanceBetween(p.pantry.latitude, p.pantry.longitude,lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                                                        results);


                                                //Less than 20 meters
                                                if(results[0] < 20f){
                                                    found = true;
                                                    globalVariable.setTypeSelected("PANTRY");
                                                    globalVariable.setPositionSelected(i);

                                                    LoginActivity.this.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            Intent intent = new Intent(LoginActivity.this, ListActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        }
                                                    });

                                                    break;
                                                }

                                                i++;
                                            }

                                            i = 0;

                                            if(!found){
                                                for(StoreWithItems s : globalVariable.getStoreWithItems()){
                                                    float[] results = new float[1];
                                                    Location.distanceBetween(s.store.latitude, s.store.longitude,lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                                                            results);

                                                    //Less than 20 meters
                                                    if(results[0] < 20f){
                                                        found = true;
                                                        globalVariable.setTypeSelected("SHOPPING");
                                                        globalVariable.setPositionSelected(i);
                                                        LoginActivity.this.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {

                                                                Intent intent = new Intent(LoginActivity.this, ListActivity.class);
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        });

                                                        break;
                                                    }
                                                    i++;
                                                }
                                            }

                                            timerHandler.removeCallbacks(this);
                                        } else {
                                            timerHandler.postDelayed(this, 500);
                                        }
                                    }
                                };
                                timerHandler.postDelayed(timerRunnable, 0);

                            }
                        } else {
                            Log.d("ADD_LIST", "Current location is null. Using defaults.");
                        }
                    }
                });
            }

            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }
}