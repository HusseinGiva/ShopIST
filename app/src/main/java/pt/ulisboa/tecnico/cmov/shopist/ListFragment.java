package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ListFragment extends Fragment {

    public static final String LIST = "LIST";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private List<String> pantryIds = new ArrayList<>();
    private List<String> storeIds = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private List<String> drive_times = new ArrayList<>();
    private List<Integer> n_items = new ArrayList<>();
    private ListView list;
    ListAdapter listAdapter = null;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;

    private Location lastKnownLocation = null;
    private FusedLocationProviderClient fusedLocationProviderClient;



    public ListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ListFragment newInstance(String param1, String param2) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if(isConnected(getActivity().getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());


        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            Log.d("ADD_LIST", "Latitude : " + lastKnownLocation.getLatitude() + ", Longitude : " +
                                    lastKnownLocation.getLongitude());
                        }else {
                            Log.d("ADD_LIST", "Current location is null. Using defaults.");
                        }
                    } else {
                        Log.d("ADD_LIST", "Current location is null. Using defaults.");
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                listAdapter.tabSelected = tab.getText().toString();
                HomeActivity ha = (HomeActivity) getActivity();
                ha.setTypeSelected(tab.getText().toString());

                names.clear();
                drive_times.clear();
                n_items.clear();
                pantryIds.clear();
                storeIds.clear();
                if (tab.getText().equals(getResources().getString(R.string.pantry))) {

                    db.collection("PantryList")
                            .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                            .get(source)
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            PantryList pantry = document.toObject(PantryList.class);
                                            names.add(pantry.name);
                                            n_items.add((int) pantry.number_of_items);
                                            pantryIds.add(document.getId());

                                            pantry.driveTime = null;
                                            drive_times.add(pantry.driveTime);

                                            if(lastKnownLocation != null && pantry.latitude != null && pantry.longitude != null){
                                                String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + lastKnownLocation.getLatitude() + "," +
                                                        lastKnownLocation.getLongitude() + "&destinations=" + pantry.latitude + "," + pantry.longitude +
                                                        "&key=AIzaSyCMZvnATlqHjaigRVtypLf06ukJxanwXl8";

                                                Object[] dataTransfer = new Object[]{pantry, url};
                                                new DownloadUrl().execute(dataTransfer);

                                                Handler timerHandler = new Handler();
                                                Runnable timerRunnable = new Runnable() {

                                                    @Override
                                                    public void run() {
                                                        if (pantry.driveTime != null) {
                                                            Log.d("LIST", String.valueOf(pantry.driveTime));
                                                            try{
                                                                drive_times.set(pantryIds.indexOf(document.getId()), pantry.driveTime);
                                                                list.invalidateViews();
                                                            }catch (ArrayIndexOutOfBoundsException e){

                                                            }

                                                            timerHandler.removeCallbacks(this);
                                                        } else {
                                                            timerHandler.postDelayed(this, 100);
                                                        }
                                                    }
                                                };
                                                timerHandler.postDelayed(timerRunnable, 0);
                                            }





                                        }

                                        list.invalidateViews();
                                    } else {
                                        Log.d("TAG", "Error getting documents: ", task.getException());
                                    }
                                }
                            });
                } else if (tab.getText().equals(getResources().getString(R.string.store))) {

                    db.collection("StoreList")
                            .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                            .get(source)
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            StoreList store = document.toObject(StoreList.class);
                                            names.add(store.name);
                                            n_items.add((int) store.number_of_items);
                                            storeIds.add(document.getId());

                                            store.driveTime = null;
                                            drive_times.add(store.driveTime);

                                            if(lastKnownLocation != null && store.latitude != null && store.longitude != null){
                                                String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + lastKnownLocation.getLatitude() + "," +
                                                        lastKnownLocation.getLongitude() + "&destinations=" + store.latitude + "," + store.longitude +
                                                        "&key=AIzaSyCMZvnATlqHjaigRVtypLf06ukJxanwXl8";

                                                Object[] dataTransfer = new Object[]{store, url};
                                                new DownloadUrl().execute(dataTransfer);

                                                Handler timerHandler = new Handler();
                                                Runnable timerRunnable = new Runnable() {

                                                    @Override
                                                    public void run() {
                                                        if (store.driveTime != null) {
                                                            Log.d("LIST", String.valueOf(store.driveTime));
                                                            try{
                                                                drive_times.set(storeIds.indexOf(document.getId()), store.driveTime);
                                                                list.invalidateViews();
                                                            }catch (ArrayIndexOutOfBoundsException e){

                                                            }


                                                            timerHandler.removeCallbacks(this);
                                                        } else {
                                                            timerHandler.postDelayed(this, 100);
                                                        }
                                                    }
                                                };
                                                timerHandler.postDelayed(timerRunnable, 0);
                                            }





                                        }
                                        list.invalidateViews();
                                    } else {
                                        Log.d("TAG", "Error getting documents: ", task.getException());
                                    }
                                }
                            });

                }


            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        db.collection("PantryList")
                .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                .get(source)
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                PantryList pantry = document.toObject(PantryList.class);
                                names.add(pantry.name);
                                n_items.add((int) pantry.number_of_items);
                                pantryIds.add(document.getId());

                                pantry.driveTime = null;
                                drive_times.add(pantry.driveTime);

                                if(lastKnownLocation != null && pantry.latitude != null && pantry.longitude != null){
                                    String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + lastKnownLocation.getLatitude() + "," +
                                            lastKnownLocation.getLongitude() + "&destinations=" + pantry.latitude + "," + pantry.longitude +
                                            "&key=AIzaSyCMZvnATlqHjaigRVtypLf06ukJxanwXl8";

                                    Object[] dataTransfer = new Object[]{pantry, url};
                                    new DownloadUrl().execute(dataTransfer);

                                    Handler timerHandler = new Handler();
                                    Runnable timerRunnable = new Runnable() {

                                        @Override
                                        public void run() {
                                            if (pantry.driveTime != null) {
                                                Log.d("LIST", String.valueOf(pantry.driveTime));
                                                drive_times.set(pantryIds.indexOf(document.getId()), pantry.driveTime);
                                                list.invalidateViews();
                                                timerHandler.removeCallbacks(this);
                                            } else {
                                                timerHandler.postDelayed(this, 100);
                                            }
                                        }
                                    };
                                    timerHandler.postDelayed(timerRunnable, 0);
                                }






                            }

                            list = view.findViewById(R.id.list);
                            listAdapter = new ListAdapter(getContext(), LIST, names, drive_times, n_items, getResources().getString(R.string.pantry), pantryIds, storeIds);
                            list.setAdapter(listAdapter);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });

        return view;
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