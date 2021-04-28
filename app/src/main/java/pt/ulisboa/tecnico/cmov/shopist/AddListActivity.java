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
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

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

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import pt.ulisboa.tecnico.cmov.shopist.persistence.AppDatabase;
import pt.ulisboa.tecnico.cmov.shopist.persistence.GlobalClass;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class AddListActivity extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap map;

    private Marker m;

    private String list_type = "";

    private Location lastKnownLocation = null;

    private AppDatabase db;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private FusedLocationProviderClient fusedLocationProviderClient;

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
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database-name").build();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyATItFqioRqPJqHdbrH8wDddm_LqKBCpBk");
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
                            .title("Marker in Location")
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
                        .title("Marker in Location")
                        .draggable(true));
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
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
        switch(view.getId()) {
            case R.id.radio_pantry:
                if (checked) {
                    this.list_type = "pantry";
                    break;
                }
            case R.id.radio_store:
                if (checked) {
                    this.list_type = "store";
                    break;
                }
        }
    }

    public void onClickSaveList(View view) {
        EditText e = (EditText) findViewById(R.id.listName);
        if(e.getText().toString().equals("")) {
            Toast.makeText(this, "Please insert a list name.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(this.list_type.equals("")) {
            Toast.makeText(this, "Please select a list type.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(this.m == null) {
            Toast.makeText(this, "Please select a list location on the map.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(lastKnownLocation == null) {
            Toast.makeText(this, "Retrying to get current location.", Toast.LENGTH_SHORT).show();
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
                                onClickSaveList(findViewById(R.id.saveListButton));
                            }
                        } else {
                            Log.d("ADD_LIST", "Current location is null. Using defaults.");
                        }
                    }
                });
            }
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + lastKnownLocation.getLatitude() + "," +
                lastKnownLocation.getLongitude() + "&destinations=" + m.getPosition().latitude + "," + m.getPosition().longitude +
                "&key=AIzaSyATItFqioRqPJqHdbrH8wDddm_LqKBCpBk";

        GlobalClass globalVariable = (GlobalClass) getApplicationContext();

        if(this.list_type.equals("pantry")) {
            PantryList l = new PantryList(e.getText().toString(), m.getPosition().latitude, m.getPosition().longitude);
            globalVariable.addPantry(l);

            Object[] dataTransfer = new Object[] { l , url };
            new DownloadUrl().execute(dataTransfer);

            Handler timerHandler = new Handler();
            Runnable timerRunnable = new Runnable() {

                @Override
                public void run() {
                    if (l.driveTime != null) {
                        mDisposable.add(db.pantryDao().insertPantryList(l)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe());
                        Intent intent = new Intent(AddListActivity.this, HomeActivity.class);
                        startActivity(intent);
                        timerHandler.removeCallbacks(this);
                    } else {
                        timerHandler.postDelayed(this, 500);
                    }
                }
            };
            timerHandler.postDelayed(timerRunnable, 0);
        }
        else if(this.list_type.equals("store")) {
            StoreList l = new StoreList(e.getText().toString(), m.getPosition().latitude, m.getPosition().longitude);
            globalVariable.addStore(l);

            Object[] dataTransfer = new Object[] { l , url };
            new DownloadUrl().execute(dataTransfer);

            Handler timerHandler = new Handler();
            Runnable timerRunnable = new Runnable() {

                @Override
                public void run() {
                    if (l.driveTime != null) {
                        mDisposable.add(db.storeDao().insertStoreList(l)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe());
                        Intent intent = new Intent(AddListActivity.this, HomeActivity.class);
                        startActivity(intent);
                        timerHandler.removeCallbacks(this);
                    } else {
                        timerHandler.postDelayed(this, 500);
                    }
                }
            };
            timerHandler.postDelayed(timerRunnable, 0);
        }
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
                    .title("Marker in Location")
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
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}