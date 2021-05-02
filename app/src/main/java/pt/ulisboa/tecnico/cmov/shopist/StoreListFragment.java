package pt.ulisboa.tecnico.cmov.shopist;

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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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

    public StoreListFragment() {}

    public static StoreListFragment newInstance(String param1) {
        StoreListFragment fragment = new StoreListFragment();
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

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_store_list, container, false);
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

                                db.collection("StoreItem").whereEqualTo("storeId", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                        if (task.isSuccessful()) {
                                            ArrayList<String> itemIds = new ArrayList<String>();
                                            List<String> store_item_names = new ArrayList<>();
                                            List<Integer> store_item_quantities = new ArrayList<>();
                                            List<Float> item_prices = new ArrayList<>();
                                            StoreListAdapter a = new StoreListAdapter(getContext(), store_item_names, store_item_quantities, item_prices, false, id, itemIds);
                                            list.setAdapter(a);
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                StoreItem si = document.toObject(StoreItem.class);
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
                                                                item_prices.add(si.price);
                                                                list.invalidateViews();
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
}
