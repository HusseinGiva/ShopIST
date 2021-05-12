package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class StoreListFragment extends Fragment {

    private static final String ARG_PARAM1 = "ID";
    List<String> itemIds = new ArrayList<>();
    List<String> store_item_names = new ArrayList<>();
    List<Integer> store_item_quantities = new ArrayList<>();
    List<Float> item_prices = new ArrayList<>();
    List<String> imageIds = new ArrayList<>();
    private String id;
    private ListView list;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;

    private List<Data> data = new ArrayList<>();
    private class Data {
        String itemId;
        String store_item_name;
        Integer store_item_quantity;
        Float item_price;
        String imageId;
    }

    public StoreListFragment() {
    }

    public static StoreListFragment newInstance(String param1) {
        StoreListFragment fragment = new StoreListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
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
            id = getArguments().getString(ARG_PARAM1);
        }

        if (isConnected(getActivity().getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_store_list, container, false);
        list = view.findViewById(R.id.store_list);

        StoreListAdapter a = new StoreListAdapter(getContext(), store_item_names, store_item_quantities, item_prices, false, id, itemIds, imageIds, list, (StoreListActivity) getActivity(), null);
        list.setAdapter(a);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        data.clear();

        int[] async_operations = {0};
        long[] server_n_items = {0};
        long[] real_n_items = {0};
        async_operations[0]++;
        db.collection("StoreList").document(id)
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                            StoreList store = document.toObject(StoreList.class);
                            server_n_items[0] = store.number_of_items;

                            async_operations[0]++;
                            db.collection("StoreItem").whereEqualTo("storeId", id).get(source).addOnCompleteListener(task1 -> {

                                if (task1.isSuccessful()) {
                                    for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                        StoreItem si = document1.toObject(StoreItem.class);
                                        if (si.quantity == 0) continue;
                                        real_n_items[0]++;
                                        async_operations[0]++;
                                        db.collection("Item").document(si.itemId).get(source).addOnCompleteListener(task112 -> {
                                            if (task112.isSuccessful()) {
                                                DocumentSnapshot document112 = task112.getResult();
                                                if (document112.exists()) {
                                                    Item i = document112.toObject(Item.class);
                                                    String storeId = si.storeId;
                                                    if (i.stores.containsKey(storeId)) {
                                                        Data d = new Data();
                                                        d.store_item_name = i.users.get(mAuth.getCurrentUser().getUid());
                                                        d.store_item_quantity = si.quantity;
                                                        d.itemId = document112.getId();
                                                        d.item_price = i.stores.get(storeId);
                                                        if (i.barcode.equals("")) d.imageId = si.itemId;
                                                        else d.imageId = i.barcode;
                                                        data.add(d);
                                                    } else {
                                                        async_operations[0]++;
                                                        db.collection("StoreList").document(storeId).get(source).addOnCompleteListener(task11 -> {
                                                            if (task11.isSuccessful()) {
                                                                DocumentSnapshot document11 = task11.getResult();
                                                                if (document11.exists()) {
                                                                    StoreList sl = document11.toObject(StoreList.class);
                                                                    for (String s : i.stores.keySet()) {
                                                                        async_operations[0]++;
                                                                        db.collection("StoreList").document(s).get(source).addOnCompleteListener(task2 -> {
                                                                            if (task2.isSuccessful()) {
                                                                                DocumentSnapshot document2 = task2.getResult();
                                                                                if (document2.exists()) {
                                                                                    StoreList sl2 = document2.toObject(StoreList.class);
                                                                                    float[] results = new float[1];
                                                                                    Location.distanceBetween(Double.parseDouble(sl.latitude), Double.parseDouble(sl.longitude),
                                                                                            Double.parseDouble(sl2.latitude), Double.parseDouble(sl2.longitude),
                                                                                            results);
                                                                                    //Less than 20 meters
                                                                                    if (results[0] < 20f) {
                                                                                        Data d = new Data();
                                                                                        d.store_item_name = i.users.get(mAuth.getCurrentUser().getUid());
                                                                                        d.store_item_quantity = si.quantity;
                                                                                        d.itemId = document112.getId();
                                                                                        d.item_price = i.stores.get(storeId);
                                                                                        if (i.barcode.equals("")) d.imageId = si.itemId;
                                                                                        else d.imageId = i.barcode;
                                                                                        data.add(d);
                                                                                    }
                                                                                    async_operations[0]--;
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                    async_operations[0]--;
                                                                }
                                                            }
                                                        });
                                                    }
                                                    async_operations[0]--;
                                                } else {
                                                    Log.d("TAG", "No such document");
                                                }


                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task112.getException());
                                            }
                                        });
                                    }

                                    async_operations[0]--;
                                } else {
                                    Log.d("TAG", "Error getting documents: ", task1.getException());
                                }

                            });

                            async_operations[0]--;
                        } else {
                            Log.d("TAG", "No such document");
                        }

                    } else {
                        Log.d("TAG", "Error getting documents: ", task.getException());
                    }
                });

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (async_operations[0] == 0) {
                    if(server_n_items[0] != real_n_items[0]) {
                        db.collection("StoreList").document(id).update("number_of_items", real_n_items[0]);
                    }
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

        data.sort(Comparator.comparing(i -> i.store_item_name.toLowerCase()));

        itemIds.clear();
        store_item_names.clear();
        store_item_quantities.clear();
        item_prices.clear();
        imageIds.clear();

        for(Data d : data) {
            itemIds.add(d.itemId);
            store_item_names.add(d.store_item_name);
            store_item_quantities.add(d.store_item_quantity);
            item_prices.add(d.item_price);
            imageIds.add(d.imageId);
        }
    }
}
