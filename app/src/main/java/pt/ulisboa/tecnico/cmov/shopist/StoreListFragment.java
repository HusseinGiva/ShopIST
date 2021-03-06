package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class StoreListFragment extends Fragment {

    private static final String ARG_PARAM1 = "ID";
    final List<String> itemIds = new ArrayList<>();
    final List<String> store_item_names = new ArrayList<>();
    final List<Integer> store_item_quantities = new ArrayList<>();
    final List<Integer> cart_item_quantities = new ArrayList<>();
    final List<Float> item_prices = new ArrayList<>();
    final List<String> imageIds = new ArrayList<>();
    private final List<Data> data = new ArrayList<>();
    FirebaseStorage storage;
    StorageReference storageRef;
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

        if (isConnected(requireActivity().getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_store_list, container, false);
        list = view.findViewById(R.id.store_list);

        StoreListAdapter a = new StoreListAdapter(getContext(), store_item_names, store_item_quantities, item_prices,
                false, id, itemIds, imageIds, list, (StoreListActivity) getActivity(), null,
                cart_item_quantities);
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
                            StoreList store = document.toObject(StoreList.class);
                            server_n_items[0] = Objects.requireNonNull(store).number_of_items;

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
                                                    if (Objects.requireNonNull(i).stores.containsKey(storeId)) {
                                                        Data d = new Data();
                                                        if (i.users.containsKey(Objects.requireNonNull(mAuth.getCurrentUser()).getUid()))
                                                            d.store_item_name = i.users.get(mAuth.getCurrentUser().getUid());
                                                        else
                                                            d.store_item_name = i.users.entrySet().iterator().next().getValue();


                                                        d.store_item_quantity = si.quantity;
                                                        d.cart_item_quantity = si.cartQuantity;
                                                        d.itemId = document112.getId();
                                                        d.item_price = i.stores.get(storeId);
                                                        if (i.barcode.equals(""))
                                                            d.imageId = si.itemId;
                                                        else d.imageId = i.barcode;
                                                        data.add(d);

                                                        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + d.imageId);
                                                        File[] files = storageDir.listFiles();
                                                        if (Objects.requireNonNull(files).length == 0) {
                                                            StorageReference imagesRef = storageRef.child(d.imageId);
                                                            async_operations[0]++;
                                                            imagesRef.listAll()
                                                                    .addOnSuccessListener(listResult -> {
                                                                        List<StorageReference> pics = listResult.getItems();
                                                                        if (pics.size() != 0) {
                                                                            File localFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + d.imageId).getAbsolutePath() + "/" + pics.get(0).getName());
                                                                            async_operations[0]++;
                                                                            pics.get(0).getFile(localFile)
                                                                                    .addOnSuccessListener(taskSnapshot -> async_operations[0]--);
                                                                        }
                                                                        async_operations[0]--;
                                                                    });
                                                        }

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
                                                                                    if (Objects.requireNonNull(sl).latitude != null && sl.longitude != null && Objects.requireNonNull(sl2).latitude != null && sl2.longitude != null)
                                                                                        Location.distanceBetween(Double.parseDouble(sl.latitude), Double.parseDouble(sl.longitude),
                                                                                                Double.parseDouble(sl2.latitude), Double.parseDouble(sl2.longitude),
                                                                                                results);
                                                                                    //Less than 20 meters
                                                                                    if (results[0] < 20f) {
                                                                                        Data d = new Data();
                                                                                        if (i.users.containsKey(Objects.requireNonNull(mAuth.getCurrentUser()).getUid()))
                                                                                            d.store_item_name = i.users.get(mAuth.getCurrentUser().getUid());
                                                                                        else
                                                                                            d.store_item_name = i.users.entrySet().iterator().next().getValue();
                                                                                        d.store_item_quantity = si.quantity;
                                                                                        d.cart_item_quantity = si.cartQuantity;
                                                                                        d.itemId = document112.getId();
                                                                                        d.item_price = i.stores.get(s);
                                                                                        if (i.barcode.equals(""))
                                                                                            d.imageId = si.itemId;
                                                                                        else
                                                                                            d.imageId = i.barcode;
                                                                                        data.add(d);

                                                                                        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + d.imageId);
                                                                                        File[] files = storageDir.listFiles();
                                                                                        if (Objects.requireNonNull(files).length == 0) {
                                                                                            StorageReference imagesRef = storageRef.child(d.imageId);
                                                                                            async_operations[0]++;
                                                                                            imagesRef.listAll()
                                                                                                    .addOnSuccessListener(listResult -> {
                                                                                                        List<StorageReference> pics = listResult.getItems();
                                                                                                        if (pics.size() != 0) {
                                                                                                            File localFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + d.imageId).getAbsolutePath() + "/" + pics.get(0).getName());
                                                                                                            async_operations[0]++;
                                                                                                            pics.get(0).getFile(localFile)
                                                                                                                    .addOnSuccessListener(taskSnapshot -> async_operations[0]--);
                                                                                                        }
                                                                                                        async_operations[0]--;
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
                                                            }
                                                        });
                                                    }
                                                    async_operations[0]--;
                                                }
                                            }
                                        });
                                    }
                                    async_operations[0]--;
                                }
                            });

                            async_operations[0]--;
                        }
                    }
                });

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (async_operations[0] == 0) {
                    if (server_n_items[0] != real_n_items[0]) {
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

        data.sort(null);

        itemIds.clear();
        store_item_names.clear();
        store_item_quantities.clear();
        cart_item_quantities.clear();
        item_prices.clear();
        imageIds.clear();

        for (Data d : data) {
            itemIds.add(d.itemId);
            store_item_names.add(d.store_item_name);
            store_item_quantities.add(d.store_item_quantity);
            cart_item_quantities.add(d.cart_item_quantity);
            item_prices.add(d.item_price);
            imageIds.add(d.imageId);
        }
    }

    private static class Data implements Comparable<Data> {
        String itemId;
        String store_item_name;
        Integer store_item_quantity;
        Integer cart_item_quantity;
        Float item_price;
        String imageId;

        @Override
        public int compareTo(Data d) {
            if (this.cart_item_quantity < d.cart_item_quantity) return -1;
            else if (this.cart_item_quantity > d.cart_item_quantity) return 1;
            else return this.store_item_name.compareTo(d.store_item_name);
        }
    }
}
