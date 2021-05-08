package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
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
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class StoreListFragment extends Fragment {

    private static final String ARG_PARAM1 = "ID";
    private String id;
    private ListView list;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;

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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        db.collection("StoreList").document(id)
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                            StoreList store = document.toObject(StoreList.class);

                            db.collection("StoreItem").whereEqualTo("storeId", id).get(source).addOnCompleteListener(task1 -> {

                                if (task1.isSuccessful()) {
                                    ArrayList<String> itemIds = new ArrayList<>();
                                    List<String> store_item_names = new ArrayList<>();
                                    List<Integer> store_item_quantities = new ArrayList<>();
                                    List<Float> item_prices = new ArrayList<>();
                                    for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                        StoreItem si = document1.toObject(StoreItem.class);
                                        db.collection("Item").document(si.itemId).get(source).addOnCompleteListener(task112 -> {
                                            if (task112.isSuccessful()) {
                                                DocumentSnapshot document112 = task112.getResult();
                                                if (document112.exists()) {
                                                    Item i = document112.toObject(Item.class);
                                                    store_item_names.add(i.users.get(mAuth.getCurrentUser().getUid()));
                                                    store_item_quantities.add(si.quantity);
                                                    itemIds.add(document112.getId());
                                                    String storeId = si.storeId;
                                                    if (i.stores.containsKey(storeId)) {
                                                        item_prices.add(i.stores.get(storeId));
                                                        list.invalidateViews();
                                                    } else {
                                                        db.collection("StoreList").document(storeId).get(source).addOnCompleteListener(task11 -> {
                                                            if (task11.isSuccessful()) {
                                                                DocumentSnapshot document11 = task11.getResult();
                                                                if (document11.exists()) {
                                                                    StoreList sl = document11.toObject(StoreList.class);
                                                                    for (String s : i.stores.keySet()) {
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
                                                                                        item_prices.add(i.stores.get(s));
                                                                                        list.invalidateViews();
                                                                                    }
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    Log.d("TAG", "No such document");
                                                }


                                            } else {
                                                Log.d("TAG", "Error getting documents: ", task112.getException());
                                            }
                                        });
                                    }
                                    StoreListAdapter a = new StoreListAdapter(getContext(), store_item_names, store_item_quantities, item_prices, false, id, itemIds, list, (StoreListActivity) getActivity(), null);
                                    list.setAdapter(a);

                                } else {
                                    Log.d("TAG", "Error getting documents: ", task1.getException());
                                }

                            });


                        } else {
                            Log.d("TAG", "No such document");
                        }

                    } else {
                        Log.d("TAG", "Error getting documents: ", task.getException());
                    }
                });
    }
}
