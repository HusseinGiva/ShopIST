package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
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
    private Source source;
    private EditText e;
    private LatLng location;
    private boolean noLocation = true;

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
        setContentView(R.layout.activity_add_list);
        if (isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;
        RadioButton rbPantry = (RadioButton) findViewById(R.id.radio_pantry);
        RadioButton rbStore = (RadioButton) findViewById(R.id.radio_store);
        if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
            rbPantry.setChecked(true);
            list_type = getResources().getString(R.string.pantry);
        } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
            rbStore.setChecked(true);
            list_type = getResources().getString(R.string.store);
        }
        Toolbar myToolbar = findViewById(R.id.addListToolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
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
        autocompleteFragment.setPlaceFields(Collections.singletonList(Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull Place place) {
                if (getIntent().getStringExtra("MODE").equals("update") && getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                } else {
                    if (m != null) {
                        m.setPosition(place.getLatLng());
                    } else {
                        m = map.addMarker(new MarkerOptions()
                                .position(place.getLatLng())
                                .title(getResources().getString(R.string.selectedLocation))
                                .draggable(true));
                    }
                }
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
            }

            @Override
            public void onError(@NotNull Status status) {
                // TODO: Handle the error.
                //Log.i(TAG, "An error occurred: " + status);
            }
        });
        e = findViewById(R.id.listName);
        if (getIntent().getStringExtra("MODE").equals("update")) {
            rbPantry.setClickable(false);
            rbStore.setClickable(false);
            getSupportActionBar().setTitle(R.string.editList);
            if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
                db.collection("PantryList").document(getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            PantryList pantryList = document.toObject(PantryList.class);
                            e.setText(pantryList.name);
                            if (pantryList.latitude != null && pantryList.longitude != null && !pantryList.latitude.equals("") && !pantryList.longitude.equals("")) {
                                noLocation = false;
                                location = new LatLng(Double.parseDouble(pantryList.latitude), Double.parseDouble(pantryList.longitude));
                            }
                        }
                    }
                });
            } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                db.collection("StoreList").document(getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            StoreList storeList = document.toObject(StoreList.class);
                            e.setText(storeList.name);
                            Button clearLocation = findViewById(R.id.addListClearLocationButton);
                            clearLocation.setText(R.string.recenterLocation);
                            if (storeList.latitude != null && storeList.longitude != null && !storeList.latitude.equals("") && !storeList.longitude.equals("")) {
                                noLocation = false;
                                location = new LatLng(Double.parseDouble(storeList.latitude), Double.parseDouble(storeList.longitude));
                            } else {
                                clearLocation.setClickable(false);
                            }
                        }
                    }
                });
            }
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);
        map.setOnMapClickListener(point -> {
            if (getIntent().getStringExtra("MODE").equals("update") && getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
            } else {
                if (m != null) {
                    m.setPosition(point);
                } else {
                    m = map.addMarker(new MarkerOptions()
                            .position(point)
                            .title(getResources().getString(R.string.selectedLocation))
                            .draggable(true));
                }
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (map != null) {
                map.setMyLocationEnabled(true);
            }
            Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    lastKnownLocation = task.getResult();
                    if (lastKnownLocation != null) {
                        Log.d("ADD_LIST", "Latitude : " + lastKnownLocation.getLatitude() + ", Longitude : " +
                                lastKnownLocation.getLongitude());
                    }
                } else {
                    Log.d("ADD_LIST", "Current location is null. Using defaults.");
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (getIntent().getStringExtra("MODE").equals("update")) {
            if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.pantry))) {
                db.collection("PantryList").document(getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            PantryList pantryList = document.toObject(PantryList.class);
                            if (pantryList.latitude != null && pantryList.longitude != null && !pantryList.latitude.equals("") && !pantryList.longitude.equals("")) {
                                noLocation = false;
                                location = new LatLng(Double.parseDouble(pantryList.latitude), Double.parseDouble(pantryList.longitude));
                                m = map.addMarker(new MarkerOptions()
                                        .position(location)
                                        .title(getResources().getString(R.string.selectedLocation))
                                        .draggable(true));
                            }
                        }
                    }
                });
            } else if (getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
                db.collection("StoreList").document(getIntent().getStringExtra("ID")).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            StoreList storeList = document.toObject(StoreList.class);
                            e.setText(storeList.name);
                            Button clearLocation = findViewById(R.id.addListClearLocationButton);
                            if (storeList.latitude != null && storeList.longitude != null && !storeList.latitude.equals("") && !storeList.longitude.equals("")) {
                                noLocation = false;
                                location = new LatLng(Double.parseDouble(storeList.latitude), Double.parseDouble(storeList.longitude));
                                m = map.addMarker(new MarkerOptions()
                                        .position(location)
                                        .title(getResources().getString(R.string.selectedLocation))
                                        .draggable(true));
                            } else {
                                clearLocation.setClickable(false);
                            }
                        }
                    }
                });
            }
        }
    }

    public void onClickClearLocation(View view) {
        if (getIntent().getStringExtra("MODE").equals("update") && getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
            if (!noLocation) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            }
        } else {
            if (m != null) {
                m.remove();
                m = null;
            }
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

    @Override
    public void onBackPressed() {
        finish();
    }

    public void onClickSaveList(View view) {
        if (e.getText().toString().equals("")) {
            Toast.makeText(this, R.string.pleaseInsertListName, Toast.LENGTH_SHORT).show();
            return;
        }
        if (this.list_type.equals("")) {
            Toast.makeText(this, R.string.pleaseSelectListType, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isConnected(getApplicationContext()))
            Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

        if (this.list_type.equals(getResources().getString(R.string.pantry))) {
            if (getIntent().getStringExtra("MODE").equals("update")) {
                if (m != null)
                    db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("name", e.getText().toString(), "latitude", String.valueOf(m.getPosition().latitude), "longitude", String.valueOf(m.getPosition().longitude));
                else
                    db.collection("PantryList").document(getIntent().getStringExtra("ID")).update("name", e.getText().toString(), "latitude", null, "longitude", null);

            } else if (getIntent().getStringExtra("MODE").equals("add")) {
                PantryList l;

                if (m != null)
                    l = new PantryList(e.getText().toString(), String.valueOf(m.getPosition().latitude), String.valueOf(m.getPosition().longitude), mAuth.getCurrentUser().getUid());
                else
                    l = new PantryList(e.getText().toString(), mAuth.getCurrentUser().getUid());

                db.collection("PantryList").add(l);

            /*Intent intent = new Intent(AddListActivity.this, HomeActivity.class);
            startActivity(intent);*/
            }
            finish();
        } else if (this.list_type.equals(getResources().getString(R.string.store))) {
            if (getIntent().getStringExtra("MODE").equals("update")) {
                db.collection("StoreList").document(getIntent().getStringExtra("ID")).update("name", e.getText().toString());
                finish();
            } else if (getIntent().getStringExtra("MODE").equals("add")) {
                StoreList l;

                if (m != null)
                    l = new StoreList(e.getText().toString(), String.valueOf(m.getPosition().latitude), String.valueOf(m.getPosition().longitude), mAuth.getCurrentUser().getUid());
                else
                    l = new StoreList(e.getText().toString(), mAuth.getCurrentUser().getUid());

                db.collection("StoreList").add(l).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            DocumentReference document_1 = task.getResult();
                            db.collection("PantryList")
                                    .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                    .get(source)
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                int[] n_new_items = {0};
                                                List<String> unique_barcodes = new ArrayList<>();
                                                List<PantryList> pantries = new ArrayList<>();
                                                List<String> pantry_ids = new ArrayList<>();
                                                for (QueryDocumentSnapshot document_2 : task.getResult()) {
                                                    PantryList pantry = document_2.toObject(PantryList.class);
                                                    pantries.add(pantry);
                                                    pantry_ids.add(document_2.getId());
                                                }

                                                int[] async_operations = {0};
                                                int[] pantry_index = {-1};
                                                Handler timerHandler = new Handler();
                                                Runnable timerRunnable = new Runnable() {

                                                    @Override
                                                    public void run() {
                                                        if (async_operations[0] == 0) {
                                                            pantry_index[0]++;
                                                            if (pantry_index[0] >= pantries.size()) {
                                                                db.collection("StoreList").document(document_1.getId())
                                                                        .update("number_of_items", n_new_items[0])
                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful()) {
                                                                                    finish();
                                                                                }
                                                                            }
                                                                        });
                                                                return;
                                                            }

                                                            async_operations[0]++;
                                                            timerHandler.postDelayed(this, 100);

                                                            db.collection("PantryItem").whereEqualTo("pantryId", pantry_ids.get(pantry_index[0])).get(source).addOnCompleteListener(task14 -> {
                                                                if (task14.isSuccessful()) {
                                                                    for (QueryDocumentSnapshot document_3 : task14.getResult()) {
                                                                        PantryItem pi = document_3.toObject(PantryItem.class);
                                                                        async_operations[0]++;
                                                                        db.collection("Item").document(pi.itemId).get(source).addOnCompleteListener(task141 -> {
                                                                            if (task141.isSuccessful()) {
                                                                                DocumentSnapshot document_4 = task141.getResult();
                                                                                if (document_4.exists()) {
                                                                                    Item i = document_4.toObject(Item.class);
                                                                                    if (!unique_barcodes.contains(i.barcode)) {
                                                                                        if (!i.barcode.equals("")) {
                                                                                            unique_barcodes.add(i.barcode);
                                                                                        }
                                                                                        StoreItem si = new StoreItem(document_1.getId(), pi.itemId, pi.idealQuantity - pi.quantity);
                                                                                        if (pi.idealQuantity - pi.quantity > 0)
                                                                                            n_new_items[0]++;
                                                                                        async_operations[0]++;
                                                                                        db.collection("StoreItem").add(si).addOnCompleteListener(task1 -> {
                                                                                            if (task1.isSuccessful())
                                                                                                async_operations[0]--;
                                                                                        });
                                                                                        i.stores.put(document_1.getId(), 0f);
                                                                                        async_operations[0]++;
                                                                                        db.collection("Item").document(pi.itemId).update("stores", i.stores).addOnCompleteListener(task12 -> {
                                                                                            if (task12.isSuccessful())
                                                                                                async_operations[0]--;
                                                                                        });
                                                                                    } else {
                                                                                        if (pi.idealQuantity - pi.quantity > 0) {
                                                                                            async_operations[0]++;
                                                                                            db.collection("StoreItem").whereEqualTo("itemId", pi.itemId)
                                                                                                    .whereEqualTo("storeId", document_1.getId()).get(source).addOnCompleteListener(task13 -> {
                                                                                                if (task13.isSuccessful()) {
                                                                                                    for (QueryDocumentSnapshot document_5 : task13.getResult()) {
                                                                                                        StoreItem si = document_5.toObject(StoreItem.class);
                                                                                                        async_operations[0]++;
                                                                                                        db.collection("StoreItem").document(document_5.getId()).update("quantity", si.quantity + pi.idealQuantity - pi.quantity)
                                                                                                                .addOnCompleteListener(task131 -> {
                                                                                                                    if (task131.isSuccessful())
                                                                                                                        async_operations[0]--;
                                                                                                                });
                                                                                                    }
                                                                                                    async_operations[0]--;
                                                                                                }
                                                                                            });
                                                                                        }
                                                                                    }
                                                                                    async_operations[0]--;
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                    async_operations[0]--;
                                                                }
                                                            });
                                                        } else {
                                                            timerHandler.postDelayed(this, 100);
                                                        }
                                                    }
                                                };
                                                timerHandler.postDelayed(timerRunnable, 0);
                                            }
                                        }
                                    });
                        }
                    }
                });
            }
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
        if (getIntent().getStringExtra("MODE").equals("update") && getIntent().getStringExtra("TYPE").equals(getResources().getString(R.string.store))) {
        } else {
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
                    locationResult.addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                Log.d("ADD_LIST", "Latitude : " + lastKnownLocation.getLatitude() + ", Longitude : " +
                                        lastKnownLocation.getLongitude());
                            }
                        } else {
                            Log.d("ADD_LIST", "Current location is null. Using defaults.");
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
        if (getIntent().getStringExtra("MODE").equals("add")) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.qrcode, menu);
        }
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
            case android.R.id.home:
                onBackPressed();    //Call the back button's method
                return true;
            case R.id.manualEntry:
                manualEntry();
                return true;
            case R.id.scanQr:
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

        alert.setPositiveButton(R.string.ok, (dialog, whichButton) -> {

            String[] splitted = input.getText().toString().split("_");

            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            if (splitted.length > 1) {
                String type = splitted[0];
                String id = splitted[1];
                if (type.equals("PANTRY")) {

                    if (!isConnected(getApplicationContext()))
                        Toast.makeText(getApplicationContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();

                    db.collection("PantryList").document(id).update("users", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid())).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            db.collection("PantryItem").whereEqualTo("pantryId", id).get(source).addOnCompleteListener(task1 -> {

                                if (task1.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task1.getResult()) {
                                        PantryItem pi = document.toObject(PantryItem.class);

                                        db.collection("Item").document(pi.itemId).get(source).addOnCompleteListener(task2 -> {

                                            if(task2.isSuccessful()){

                                                Item i = task2.getResult().toObject(Item.class);

                                                if(!i.users.containsKey(mAuth.getCurrentUser().getUid())){
                                                    Map.Entry<String,String> entry = i.users.entrySet().iterator().next();
                                                    db.collection("Item").document(task2.getResult().getId()).update("users." + mAuth.getCurrentUser().getUid(), entry.getValue());
                                                }

                                            }else {
                                                Log.d("TAG", "Error getting documents: ", task2.getException());
                                            }

                                        });


                                    }
                                } else {
                                    Log.d("TAG", "Error getting documents: ", task1.getException());
                                }


                            });

                            Intent intent = new Intent(AddListActivity.this, PantryListActivity.class);
                            intent.putExtra("ID", id);
                            intent.putExtra("SENDER", "start");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(AddListActivity.this, R.string.invalidCode, Toast.LENGTH_SHORT).show();
                            dialog.cancel();
                        }
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

                                    String newStoreId = documentReference.getId();

                                    db.collection("PantryList").whereArrayContains("users", mAuth.getCurrentUser().getUid()).get(source).addOnCompleteListener(task1 -> {
                                        if(task1.isSuccessful()){
                                            for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                                String pantryId = document1.getId();
                                                PantryList p = document1.toObject(PantryList.class);

                                                db.collection("PantryItem").whereEqualTo("pantryId", pantryId).get(source).addOnCompleteListener(task2 -> {
                                                   if(task2.isSuccessful()){
                                                       for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                           PantryItem pi = document2.toObject(PantryItem.class);

                                                           db.collection("Item").document(pi.itemId).get(source).addOnCompleteListener(task3 -> {
                                                               if(task3.isSuccessful()){
                                                                   DocumentSnapshot document3 = task3.getResult();
                                                                   Item i = document3.toObject(Item.class);
                                                                   String itemId = document3.getId();

                                                                   if(i.barcode.equals("")){
                                                                        StoreItem si = new StoreItem(newStoreId, itemId, pi.idealQuantity - pi.quantity);

                                                                        db.collection("StoreItem").add(si);
                                                                   }
                                                               }
                                                           });
                                                       }

                                                   }
                                                });

                                            }

                                            Intent intent = new Intent(AddListActivity.this, StoreListActivity.class);
                                            intent.putExtra("ID", documentReference.getId());
                                            intent.putExtra("SENDER", "start");
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();

                                        }
                                    });




                                });

                            }else{
                                Toast.makeText(AddListActivity.this, R.string.invalidCode, Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }


                        } else {
                            Toast.makeText(AddListActivity.this, R.string.invalidCode, Toast.LENGTH_SHORT).show();
                            dialog.cancel();
                        }
                    });

                    return;
                }
            }
            Toast.makeText(AddListActivity.this, R.string.invalidCode, Toast.LENGTH_SHORT).show();
            dialog.cancel();

        });

        alert.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
            // Canceled.
        });

        alert.show();
    }

    public void startQrCodeActivity() {
        Intent intent = new Intent(AddListActivity.this, QrCodeScanner.class);
        startActivity(intent);
    }

}