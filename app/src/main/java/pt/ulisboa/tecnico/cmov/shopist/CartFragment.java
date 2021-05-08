package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
    List<String> itemIds = new ArrayList<>();
    List<String> store_item_names = new ArrayList<>();
    List<Integer> store_item_quantities = new ArrayList<>();
    List<Float> item_prices = new ArrayList<>();
    View view;
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
        view = inflater.inflate(R.layout.fragment_cart, container, false);

        list = view.findViewById(R.id.store_list);

        StoreListAdapter a = new StoreListAdapter(getContext(), store_item_names, store_item_quantities, item_prices, true, id, itemIds, list, (StoreListActivity) getActivity(), (TextView) view.findViewById(R.id.total_cost));
        list.setAdapter(a);

        view.findViewById(R.id.checkout).setOnClickListener(v -> {
            if (store_item_names.size() == 0) {
                Toast.makeText(getContext(), R.string.youHaveNoItemsInCart, Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(getContext(), CheckoutActivity.class);
                intent.putExtra("ID", id);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        TextView textView = view.findViewById(R.id.total_cost);
        textView.setText("0");
        float[] total_cost = {0};

        int[] async_operations = {0};

        async_operations[0]++;
        db.collection("StoreList").document(id)
                .get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("TAG", "DocumentSnapshot data: " + document.getData());
                            StoreList store = document.toObject(StoreList.class);

                            async_operations[0]++;
                            db.collection("StoreItem").whereEqualTo("storeId", id).get(source).addOnCompleteListener(task13 -> {

                                if (task13.isSuccessful()) {
                                    itemIds.clear();
                                    store_item_names.clear();
                                    store_item_quantities.clear();
                                    item_prices.clear();
                                    for (QueryDocumentSnapshot document13 : task13.getResult()) {
                                        StoreItem si = document13.toObject(StoreItem.class);
                                        if (si.cartQuantity == 0) continue;

                                        async_operations[0]++;
                                        db.collection("Item").document(si.itemId).get(source).addOnCompleteListener(task12 -> {
                                            if (task12.isSuccessful()) {
                                                DocumentSnapshot document12 = task12.getResult();
                                                if (document12.exists()) {
                                                    Item i = document12.toObject(Item.class);
                                                    String storeId = si.storeId;
                                                    if (i.stores.containsKey(storeId)) {
                                                        itemIds.add(si.itemId);
                                                        store_item_names.add(i.users.get(mAuth.getCurrentUser().getUid()));
                                                        store_item_quantities.add(si.cartQuantity);
                                                        item_prices.add(i.stores.get(storeId));
                                                        total_cost[0] += si.cartQuantity * i.stores.get(storeId);
                                                        DecimalFormat df = new DecimalFormat("###.##");
                                                        Double value = Math.round(total_cost[0] * 100.0) / 100.0;
                                                        textView.setText(String.valueOf(df.format(value)));
                                                        if (value == 0) {
                                                            textView.setVisibility(View.INVISIBLE);
                                                            TextView euro = view.findViewById(R.id.textView7);
                                                            euro.setVisibility(View.INVISIBLE);
                                                        }
                                                        else {
                                                            textView.setVisibility(View.VISIBLE);
                                                            TextView euro = view.findViewById(R.id.textView7);
                                                            euro.setVisibility(View.VISIBLE);
                                                        }
                                                    } else {
                                                        async_operations[0]++;
                                                        db.collection("StoreList").document(storeId).get(source).addOnCompleteListener(task1 -> {
                                                            if (task1.isSuccessful()) {
                                                                DocumentSnapshot document1 = task1.getResult();
                                                                if (document1.exists()) {
                                                                    StoreList sl = document1.toObject(StoreList.class);
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
                                                                                        itemIds.add(si.itemId);
                                                                                        store_item_names.add(i.users.get(mAuth.getCurrentUser().getUid()));
                                                                                        store_item_quantities.add(si.cartQuantity);
                                                                                        item_prices.add(i.stores.get(s));
                                                                                        total_cost[0] += si.cartQuantity * i.stores.get(s);
                                                                                        DecimalFormat df = new DecimalFormat("###.##");
                                                                                        Double value = Math.round(total_cost[0] * 100.0) / 100.0;
                                                                                        textView.setText(String.valueOf(df.format(value)));
                                                                                        if (value == 0) {
                                                                                            textView.setVisibility(View.INVISIBLE);
                                                                                            TextView euro = view.findViewById(R.id.textView7);
                                                                                            euro.setVisibility(View.INVISIBLE);
                                                                                        }
                                                                                        else {
                                                                                            textView.setVisibility(View.VISIBLE);
                                                                                            TextView euro = view.findViewById(R.id.textView7);
                                                                                            euro.setVisibility(View.VISIBLE);
                                                                                        }
                                                                                    }
                                                                                }
                                                                                async_operations[0]--;
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
                                                Log.d("TAG", "Error getting documents: ", task12.getException());
                                            }
                                        });
                                    }
                                    async_operations[0]--;

                                } else {
                                    Log.d("TAG", "Error getting documents: ", task13.getException());
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

        List<String> ids_base = new ArrayList<>(itemIds);
        itemIds.sort(Comparator.comparing(i -> store_item_names.get(ids_base.indexOf(i)).toLowerCase()));

        List<Integer> quantities_base = new ArrayList<>(store_item_quantities);
        store_item_quantities.sort(Comparator.comparing(i -> store_item_names.get(quantities_base.indexOf(i)).toLowerCase()));

        List<Float> prices_base = new ArrayList<>(item_prices);
        item_prices.sort(Comparator.comparing(i -> store_item_names.get(prices_base.indexOf(i)).toLowerCase()));

        store_item_names.sort(Comparator.comparing(String::toLowerCase));
    }
}