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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CartFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "ID";
    private String id;
    private ListView list;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Source source;

    public CartFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CartFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CartFragment newInstance(String param1, String param2) {
        CartFragment fragment = new CartFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
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
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        list = view.findViewById(R.id.store_list);

        db.collection("StoreList").document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                                StoreList store = document.toObject(StoreList.class);

                                db.collection("StoreItem").whereEqualTo("storeId", id).get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                        if (task.isSuccessful()) {
                                            List<String> itemIds = new ArrayList<String>();
                                            List<String> store_item_names = new ArrayList<>();
                                            List<Integer> store_item_quantities = new ArrayList<>();
                                            List<Float> item_prices = new ArrayList<>();
                                            StoreListAdapter a = new StoreListAdapter(getContext(), store_item_names, store_item_quantities, item_prices, true, id, itemIds);
                                            list.setAdapter(a);
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                StoreItem si = document.toObject(StoreItem.class);
                                                if (si.cartQuantity == 0) continue;
                                                itemIds.add(si.itemId);
                                                db.collection("Item").document(si.itemId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            DocumentSnapshot document = task.getResult();
                                                            if (document.exists()) {
                                                                Item i = document.toObject(Item.class);
                                                                store_item_names.add(i.users.get(mAuth.getCurrentUser().getUid()));
                                                                store_item_quantities.add(si.quantity);
                                                                String storeId = si.storeId;
                                                                if (i.stores.containsKey(storeId)) {
                                                                    item_prices.add(i.stores.get(storeId));
                                                                    list.invalidateViews();
                                                                } else {
                                                                    db.collection("StoreList").document(storeId).get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                            if (task.isSuccessful()) {
                                                                                DocumentSnapshot document = task.getResult();
                                                                                if (document.exists()) {
                                                                                    StoreList sl = document.toObject(StoreList.class);
                                                                                    for (String s : i.stores.keySet()) {
                                                                                        db.collection("StoreList").document(s).get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task2) {
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
                                                                                            }
                                                                                        });
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            } else {
                                                                Log.d("TAG", "No such document");
                                                            }


                                                        } else {
                                                            Log.d("TAG", "Error getting documents: ", task.getException());
                                                        }
                                                    }
                                                });
                                            }

                                        } else {
                                            Log.d("TAG", "Error getting documents: ", task.getException());
                                        }

                                    }
                                });


                            } else {
                                Log.d("TAG", "No such document");
                            }

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