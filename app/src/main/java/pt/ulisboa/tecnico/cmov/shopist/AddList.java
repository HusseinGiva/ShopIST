package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import pt.ulisboa.tecnico.cmov.shopist.persistence.AppDatabase;
import pt.ulisboa.tecnico.cmov.shopist.persistence.GlobalClass;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class AddList extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap map;

    private Marker m;

    private String list_type = "";

    private AppDatabase db;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

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

        GlobalClass globalVariable = (GlobalClass) getApplicationContext();

        if(this.list_type.equals("pantry")) {
            PantryList l = new PantryList(e.getText().toString(), "Paradise");
            globalVariable.addPantry(l);
            mDisposable.add(db.pantryDao().insertPantryList(l)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe());
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        }
        else if(this.list_type.equals("store")) {
            StoreList l = new StoreList(e.getText().toString(), "Hell");
            globalVariable.addStore(l);
            mDisposable.add(db.storeDao().insertStoreList(l)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe());
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
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
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}