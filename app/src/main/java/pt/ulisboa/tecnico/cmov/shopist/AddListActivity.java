package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import io.reactivex.disposables.CompositeDisposable;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class AddListActivity extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private GoogleMap map;
    private Marker m;
    private String list_type = "";
    private Location lastKnownLocation = null;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_list);
        Toolbar myToolbar = findViewById(R.id.addListToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        m = null;
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCMZvnATlqHjaigRVtypLf06ukJxanwXl8");
        }
        //PlacesClient placesClient = Places.createClient(this);
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull Place place) {
                if (m != null) {
                    m.setPosition(place.getLatLng());
                } else {
                    m = map.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(getResources().getString(R.string.selectedLocation))
                            .draggable(true));
                }
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
            }

            @Override
            public void onError(@NotNull Status status) {
                // TODO: Handle the error.
                //Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);
        map.setOnMapClickListener(point -> {
            if (m != null) {
                m.setPosition(point);
            } else {
                m = map.addMarker(new MarkerOptions()
                        .position(point)
                        .title(getResources().getString(R.string.selectedLocation))
                        .draggable(true));
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (map != null) {
                map.setMyLocationEnabled(true);
            }

            Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            Log.d("ADD_LIST", "Latitude : " + lastKnownLocation.getLatitude() + ", Longitude : " +
                                    lastKnownLocation.getLongitude());
                        }
                    } else {
                        Log.d("ADD_LIST", "Current location is null. Using defaults.");
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    public void onClickClearLocation(View view) {
        if (m != null) {
            m.remove();
            m = null;
        }
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radio_pantry:
                if (checked) {
                    this.list_type = getResources().getString(R.string.pantry);
                    break;
                }
            case R.id.radio_store:
                if (checked) {
                    this.list_type = getResources().getString(R.string.store);
                    break;
                }
        }
    }

    public void onClickSaveList(View view) {
        EditText e = (EditText) findViewById(R.id.listName);
        if (e.getText().toString().equals("")) {
            Toast.makeText(this, R.string.pleaseInsertListName, Toast.LENGTH_SHORT).show();
            return;
        }
        if (this.list_type.equals("")) {
            Toast.makeText(this, R.string.pleaseSelectListType, Toast.LENGTH_SHORT).show();
            return;
        }

        if(!isConnected(getApplicationContext()))
            Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

        if (this.list_type.equals(getResources().getString(R.string.pantry))) {

            PantryList l;

            if(m != null && m.getPosition() != null)
                l = new PantryList(e.getText().toString(), String.valueOf(m.getPosition().latitude), String.valueOf(m.getPosition().longitude), mAuth.getCurrentUser().getUid());
            else
                l = new PantryList(e.getText().toString(), mAuth.getCurrentUser().getUid());

            db.collection("PantryList").add(l);

            Intent intent = new Intent(AddListActivity.this, HomeActivity.class);
            startActivity(intent);

        } else if (this.list_type.equals(getResources().getString(R.string.store))) {

            StoreList l;

            if(m != null && m.getPosition() != null)
                l = new StoreList(e.getText().toString(), String.valueOf(m.getPosition().latitude),String.valueOf(m.getPosition().longitude), mAuth.getCurrentUser().getUid());
            else
                l = new StoreList(e.getText().toString(), mAuth.getCurrentUser().getUid());


            db.collection("StoreList").add(l);


            Intent intent = new Intent(AddListActivity.this, HomeActivity.class);
            startActivity(intent);

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

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (m != null) {
            m.setPosition(latLng);
        } else {
            m = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getResources().getString(R.string.selectedLocation))
                    .draggable(true));
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
                    if (map != null) {
                        map.setMyLocationEnabled(true);
                    }
                    Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                    locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful()) {
                                lastKnownLocation = task.getResult();
                                if (lastKnownLocation != null) {
                                    Log.d("ADD_LIST", "Latitude : " + lastKnownLocation.getLatitude() + ", Longitude : " +
                                            lastKnownLocation.getLongitude());
                                }
                            } else {
                                Log.d("ADD_LIST", "Current location is null. Using defaults.");
                            }
                        }
                    });
                } else if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startQrCodeActivity();
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.qrcode, menu);
        return true;
    }

    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            startQrCodeActivity();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.manualEntry:
                manualEntry();
                return true;
            case R.id.qrcode:
                requestCamera();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void manualEntry() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.manualInput);
        alert.setMessage(R.string.enterListCode);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String[] splitted = input.getText().toString().split("_");

                db = FirebaseFirestore.getInstance();
                mAuth = FirebaseAuth.getInstance();

                if (splitted.length > 1) {
                    String type = splitted[0];
                    String id = splitted[1];
                    if (type.equals("PANTRY")) {

                        if(!isConnected(getApplicationContext()))
                            Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

                        db.collection("PantryList").document(id).update("users", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid())).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Intent intent = new Intent(AddListActivity.this, PantryListActivity.class);
                                intent.putExtra("ID", id);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }else{
                                Toast.makeText(AddListActivity.this, R.string.invalidCode, Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }
                        });

                        return;
                    } else if (type.equals("STORE")) {

                        if(!isConnected(getApplicationContext()))
                            Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

                        db.collection("StoreList").document(id).update("users", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid())).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Intent intent = new Intent(AddListActivity.this, StoreListActivity.class);
                                intent.putExtra("ID", id);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }else{
                                Toast.makeText(AddListActivity.this, R.string.invalidCode, Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }
                        });

                        return;
                    }
                }
                Toast.makeText(AddListActivity.this, R.string.invalidCode, Toast.LENGTH_SHORT).show();
                dialog.cancel();

            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void startQrCodeActivity() {
        Intent intent = new Intent(AddListActivity.this, QrCodeScanner.class);
        startActivity(intent);
    }

}