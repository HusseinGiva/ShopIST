package pt.ulisboa.tecnico.cmov.shopist;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;

public class SimWifiP2pBroadcastReceiver extends BroadcastReceiver {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final Source source;
    private StartActivity mActivity;
    private boolean isInsideBeacon;
    private FusedLocationProviderClient fusedLocationClient;

    public static boolean isConnected(Context getApplicationContext) {
        boolean status = false;

        ConnectivityManager cm = (ConnectivityManager) getApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null) {
            // connected to the internet
            status = true;
        }


        return status;
    }

    public SimWifiP2pBroadcastReceiver(StartActivity activity) {
        super();
        this.mActivity = activity;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        if (isConnected(activity.getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Toast.makeText(mActivity, "Received action",
                Toast.LENGTH_SHORT).show();
        if (SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // This action is triggered when the Termite service changes state:
            // - creating the service generates the WIFI_P2P_STATE_ENABLED event
            // - destroying the service generates the WIFI_P2P_STATE_DISABLED event

            int state = intent.getIntExtra(SimWifiP2pBroadcast.EXTRA_WIFI_STATE, -1);
            if (state == SimWifiP2pBroadcast.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(mActivity, "WiFi Direct enabled",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mActivity, "WiFi Direct disabled",
                        Toast.LENGTH_SHORT).show();
            }

        } else if (SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // Request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()

            Toast.makeText(mActivity, "Peer list changed",
                    Toast.LENGTH_SHORT).show();


        } else if (SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION.equals(action)) {

            SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.getSerializableExtra(
                    SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
            ginfo.print();
            Toast.makeText(mActivity, "Network membership changed",
                    Toast.LENGTH_SHORT).show();

            if (ginfo.askIsConnected() && !isInsideBeacon) {
                isInsideBeacon = true;

                if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(mActivity, location -> {
                            if (location != null) {



                                db.collection("StoreList")
                                        .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                        .get(source)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {

                                                ArrayList<StoreList> stores = new ArrayList<>();
                                                ArrayList<String> storesId = new ArrayList<>();

                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    StoreList s = document.toObject(StoreList.class);

                                                    if (s.latitude != null && s.longitude != null) {
                                                        float[] results = new float[1];
                                                        Location.distanceBetween(Double.parseDouble(s.latitude), Double.parseDouble(s.longitude), location.getLatitude(), location.getLongitude(),
                                                                results);

                                                        //Less than 20 meters
                                                        if (results[0] < 20f) {
                                                            storesId.add(document.getId());
                                                            stores.add(s);
                                                        }
                                                    }


                                                }

                                                if(stores.size() == 1){

                                                    StoreList store = stores.get(0);
                                                    String storeId = storesId.get(0);



                                                    db.collection("StoreList").get(source).addOnCompleteListener(task1 -> {
                                                       if(task1.isSuccessful()){

                                                           stores.clear();
                                                           storesId.clear();

                                                           for (QueryDocumentSnapshot document : task1.getResult()) {
                                                               StoreList s = document.toObject(StoreList.class);

                                                               if (s.latitude != null && s.longitude != null) {
                                                                   float[] results = new float[1];
                                                                   Location.distanceBetween(Double.parseDouble(s.latitude), Double.parseDouble(s.longitude), Double.parseDouble(store.latitude), Double.parseDouble(store.longitude),
                                                                           results);

                                                                   //Less than 20 meters
                                                                   if (results[0] < 20f) {
                                                                       stores.add(s);
                                                                       storesId.add(document.getId());
                                                                   }
                                                               }


                                                           }

                                                           db.collection("StoreItem").whereEqualTo("storeId", storeId).get(source).addOnCompleteListener(task2 -> {
                                                               if(task2.isSuccessful()){
                                                                   int queueItemsQuantity = store.nQueueItems;
                                                                   for (QueryDocumentSnapshot document : task2.getResult()) {
                                                                       StoreItem si = document.toObject(StoreItem.class);

                                                                       queueItemsQuantity += si.cartQuantity;

                                                                   }

                                                                   db.collection("StoreList").document(storeId).update("usersArriveTime." + mAuth.getCurrentUser().getUid(), Timestamp.now());

                                                                   db.collection("StoreList").document(storeId).update("usersQueueItemsAtArrival." + mAuth.getCurrentUser().getUid(), queueItemsQuantity);

                                                                   for (int i = 0; i < stores.size(); i++) {
                                                                        db.collection("StoreList").document(storesId.get(i)).update("nQueueItems", queueItemsQuantity);
                                                                   }

                                                               }else {
                                                                   Log.d("TAG", "Error getting documents: ", task2.getException());
                                                               }

                                                           });


                                                       } else {
                                                           Log.d("TAG", "Error getting documents: ", task1.getException());
                                                       }

                                                    });

                                                }
                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task.getException());
                                            }
                                        });
                            }

                        });

            }else if(!ginfo.askIsConnected() && isInsideBeacon){

                isInsideBeacon = false;

                if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(mActivity, location -> {
                            if (location != null) {



                                db.collection("StoreList")
                                        .whereArrayContains("users", mAuth.getCurrentUser().getUid())
                                        .get(source)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {

                                                ArrayList<StoreList> stores = new ArrayList<>();
                                                ArrayList<String> storesId = new ArrayList<>();

                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    StoreList s = document.toObject(StoreList.class);

                                                    if (s.latitude != null && s.longitude != null) {
                                                        float[] results = new float[1];
                                                        Location.distanceBetween(Double.parseDouble(s.latitude), Double.parseDouble(s.longitude), location.getLatitude(), location.getLongitude(),
                                                                results);

                                                        //Less than 20 meters
                                                        if (results[0] < 20f) {
                                                            storesId.add(document.getId());
                                                            stores.add(s);
                                                        }
                                                    }


                                                }

                                                if(stores.size() == 1){

                                                    StoreList store = stores.get(0);
                                                    String storeId = storesId.get(0);



                                                    db.collection("StoreList").get(source).addOnCompleteListener(task1 -> {
                                                        if(task1.isSuccessful()){

                                                            stores.clear();
                                                            storesId.clear();

                                                            for (QueryDocumentSnapshot document : task1.getResult()) {
                                                                StoreList s = document.toObject(StoreList.class);

                                                                if (s.latitude != null && s.longitude != null) {
                                                                    float[] results = new float[1];
                                                                    Location.distanceBetween(Double.parseDouble(s.latitude), Double.parseDouble(s.longitude), Double.parseDouble(store.latitude), Double.parseDouble(store.longitude),
                                                                            results);

                                                                    //Less than 20 meters
                                                                    if (results[0] < 20f) {
                                                                        stores.add(s);
                                                                        storesId.add(document.getId());
                                                                    }
                                                                }


                                                            }

                                                            db.collection("StoreItem").whereEqualTo("storeId", storeId).get(source).addOnCompleteListener(task2 -> {
                                                                if(task2.isSuccessful()){
                                                                    int userCartItems = 0;
                                                                    for (QueryDocumentSnapshot document : task2.getResult()) {
                                                                        StoreItem si = document.toObject(StoreItem.class);

                                                                        userCartItems += si.cartQuantity;

                                                                    }

                                                                    store.nCartItemsAtArrival.add(store.usersQueueItemsAtArrival.get(mAuth.getCurrentUser().getUid()));
                                                                    store.timeInQueue.add(Timestamp.now().getSeconds() - store.usersArriveTime.get(mAuth.getCurrentUser().getUid()).getSeconds());
                                                                    db.collection("StoreList").document(storeId).update("nCartItemsAtArrival", store.nCartItemsAtArrival);
                                                                    db.collection("StoreList").document(storeId).update("timeInQueue", store.timeInQueue);

                                                                    for (int i = 0; i < stores.size(); i++) {
                                                                        db.collection("StoreList").document(storesId.get(i)).update("nQueueItems", FieldValue.increment(userCartItems * -1));
                                                                    }

                                                                }else {
                                                                    Log.d("TAG", "Error getting documents: ", task2.getException());
                                                                }

                                                            });


                                                        } else {
                                                            Log.d("TAG", "Error getting documents: ", task1.getException());
                                                        }

                                                    });

                                                }
                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task.getException());
                                            }
                                        });
                            }

                        });



            }

        } else if (SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION.equals(action)) {

            SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.getSerializableExtra(
                    SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
            ginfo.print();
            Toast.makeText(mActivity, "Group ownership changed",
                    Toast.LENGTH_SHORT).show();
        }
    }
}