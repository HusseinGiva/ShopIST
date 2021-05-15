package pt.ulisboa.tecnico.cmov.shopist;

import android.Manifest;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final List<String> pantryIds = new ArrayList<>();
    private final List<String> storeIds = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<String> drive_times = new ArrayList<>();
    private final List<Integer> n_items = new ArrayList<>();
    private final List<Double> queue_times = new ArrayList<>();
    ListAdapter listAdapter = null;
    private ListView list;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;

    private Location lastKnownLocation = null;
    private FirebaseFunctions mFunctions;

    private List<Data> data = new ArrayList<>();
    private class Data {
        String pantryId = null;
        String storeId = null;
        String name = null;
        String drive_time = null;
        Integer n_items = null;
        Double queue_time = null;
    }

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if (isConnected(getActivity().getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFunctions = FirebaseFunctions.getInstance();

        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());


        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(getActivity(), task -> {
                if (task.isSuccessful()) {
                    lastKnownLocation = task.getResult();
                    if (lastKnownLocation != null) {
                        Log.d("ADD_LIST", "Latitude : " + lastKnownLocation.getLatitude() + ", Longitude : " +
                                lastKnownLocation.getLongitude());
                    } else {
                        Log.d("ADD_LIST", "Current location is null. Using defaults.");
                    }
                } else {
                    Log.d("ADD_LIST", "Current location is null. Using defaults.");
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

        list = view.findViewById(R.id.list);
        listAdapter = new ListAdapter(getContext(), LIST, names, drive_times, n_items, queue_times, getResources().getString(R.string.pantry), pantryIds, storeIds);
        list.setAdapter(listAdapter);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                listAdapter.tabSelected = tab.getText().toString();
                HomeActivity ha = (HomeActivity) getActivity();
                ha.setTypeSelected(tab.getText().toString());

                if (tab.getText().equals(getResources().getString(R.string.pantry))) {
                    loadPantryLists();
                } else if (tab.getText().equals(getResources().getString(R.string.store))) {
                    loadStoreLists();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        HomeActivity ha = (HomeActivity) getActivity();

        if (ha.getTypeSelected().equals(getResources().getString(R.string.pantry))) {
            loadPantryLists();
        } else if (ha.getTypeSelected().equals(getResources().getString(R.string.store))) {
            loadStoreLists();
        }
    }

    public void loadPantryLists() {
        data.clear();

        int[] async_operations = {0};

        async_operations[0]++;
        db.collection("PantryList")
                .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                .get(source)
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                PantryList pantry = document.toObject(PantryList.class);

                                Data d = new Data();
                                d.name = pantry.name;
                                d.n_items = (int) pantry.number_of_items;
                                d.pantryId = document.getId();
                                data.add(d);

                                pantry.driveTime = null;

                                if (lastKnownLocation != null && pantry.latitude != null && pantry.longitude != null) {
                                    String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + lastKnownLocation.getLatitude() + "," +
                                            lastKnownLocation.getLongitude() + "&destinations=" + pantry.latitude + "," + pantry.longitude +
                                            "&key=AIzaSyCMZvnATlqHjaigRVtypLf06ukJxanwXl8";

                                    Object[] dataTransfer = new Object[]{pantry, url};
                                    async_operations[0]++;
                                    new DownloadUrl().execute(dataTransfer);

                                    Handler timerHandler = new Handler();
                                    Runnable timerRunnable = new Runnable() {

                                        @Override
                                        public void run() {
                                            if (pantry.driveTime != null) {
                                                d.drive_time = pantry.driveTime;
                                                async_operations[0]--;
                                            } else {
                                                timerHandler.postDelayed(this, 100);
                                            }
                                        }
                                    };
                                    timerHandler.postDelayed(timerRunnable, 0);
                                }
                            }
                            async_operations[0]--;
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (async_operations[0] == 0) {
                    sort();
                    list.invalidateViews();
                } else {
                    timerHandler.postDelayed(this, 100);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void loadStoreLists() {
        data.clear();

        int[] async_operations = {0};

        async_operations[0]++;
        db.collection("StoreList")
                .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                .get(source)
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                StoreList store = document.toObject(StoreList.class);

                                Data d = new Data();
                                d.name = store.name;
                                d.n_items = (int) store.number_of_items;
                                d.storeId = document.getId();
                                data.add(d);

                                //add queue time
                                async_operations[0]++;
                                computeQueueWaitTime(document.getId())
                                        .addOnCompleteListener(task1 -> {
                                            if (!task1.isSuccessful()) {
                                                Exception e = task1.getException();
                                                if (e instanceof FirebaseFunctionsException) {
                                                    FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                                    FirebaseFunctionsException.Code code = ffe.getCode();
                                                    Object details = ffe.getDetails();
                                                }
                                                d.queue_time = -1.0;
                                            } else {
                                                d.queue_time = Double.parseDouble(task1.getResult());
                                            }
                                            async_operations[0]--;
                                        });

                                store.driveTime = null;

                                if (lastKnownLocation != null && store.latitude != null && store.longitude != null) {
                                    String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + lastKnownLocation.getLatitude() + "," +
                                            lastKnownLocation.getLongitude() + "&destinations=" + store.latitude + "," + store.longitude +
                                            "&key=AIzaSyCMZvnATlqHjaigRVtypLf06ukJxanwXl8";

                                    Object[] dataTransfer = new Object[]{store, url};
                                    async_operations[0]++;
                                    new DownloadUrl().execute(dataTransfer);

                                    Handler timerHandler = new Handler();
                                    Runnable timerRunnable = new Runnable() {

                                        @Override
                                        public void run() {
                                            if (store.driveTime != null) {
                                                d.drive_time = store.driveTime;
                                                async_operations[0]--;

                                                timerHandler.removeCallbacks(this);
                                            } else {
                                                timerHandler.postDelayed(this, 100);
                                            }
                                        }
                                    };
                                    timerHandler.postDelayed(timerRunnable, 0);
                                }
                            }
                            async_operations[0]--;
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (async_operations[0] == 0) {
                    sort();
                    list.invalidateViews();
                } else {
                    timerHandler.postDelayed(this, 100);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void sort() {

        data.sort(Comparator.comparing(i -> i.name.toLowerCase()));

        names.clear();
        drive_times.clear();
        n_items.clear();
        queue_times.clear();
        pantryIds.clear();
        storeIds.clear();

        for(Data d : data) {
            pantryIds.add(d.pantryId);
            storeIds.add(d.storeId);
            names.add(d.name);
            drive_times.add(d.drive_time);
            n_items.add(d.n_items);
            queue_times.add(d.queue_time);
        }
    }

    private Task<String> computeQueueWaitTime(String storeId) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("store", storeId);

        return mFunctions
                .getHttpsCallable("computeQueueWaitTime")
                .call(data)
                .continueWith(task -> {
                    // This continuation runs on either success or failure, but if the task
                    // has failed then getResult() will throw an Exception which will be
                    // propagated down.
                    HashMap result = (HashMap) task.getResult().getData();
                    return String.valueOf(result.get("result"));
                });
    }

}