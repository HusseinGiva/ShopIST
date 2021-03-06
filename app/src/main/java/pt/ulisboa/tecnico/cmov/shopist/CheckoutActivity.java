package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class CheckoutActivity extends AppCompatActivity {

    final List<String> item_names = new ArrayList<>();
    final List<Integer> item_quantities = new ArrayList<>();
    final List<Float> item_prices = new ArrayList<>();
    final List<String> item_ids = new ArrayList<>();
    final List<String> imageIds = new ArrayList<>();
    final List<String> pantryIds = new ArrayList<>();
    final List<String> pantryNames = new ArrayList<>();
    final List<Integer> quantitiesNeeded = new ArrayList<>();
    final List<String> pantryQuantities = new ArrayList<>();
    // { itemId -> { pantryId -> quantity, ... }, ... }
    private final Map<String, Map<String, String>> quantitiesPerPantry = new HashMap<>();
    private final List<ItemData> itemData = new ArrayList<>();
    private final List<PantryData> pantryData = new ArrayList<>();
    CheckoutListAdapter adapter;
    FirebaseStorage storage;
    StorageReference storageRef;
    private int current_item = 0;
    private String id;
    private TextView pos;
    private ListView list;
    private TextView name;
    private ImageView image;
    private TextView quantity;
    private TextView price;
    private ImageView next;
    private ImageView previous;
    private Button confirm;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean everything_loaded = false;
    private Source source;

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
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        Toolbar myToolbar = findViewById(R.id.checkout_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

        if (isConnected(getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        list = findViewById(R.id.checkout_list);
        pos = findViewById(R.id.checkout_position);
        name = findViewById(R.id.checkout_name);
        image = findViewById(R.id.checkout_item_image);
        quantity = findViewById(R.id.checkout_quantity);
        price = findViewById(R.id.checkout_price);
        next = findViewById(R.id.checkout_next);
        previous = findViewById(R.id.checkout_previous);
        confirm = findViewById(R.id.checkout_confirm);

        id = getIntent().getStringExtra("ID");

        adapter = new CheckoutListAdapter(this, pantryIds, pantryNames, quantitiesNeeded, pantryQuantities, quantitiesPerPantry, item_ids, item_quantities);
        list.setAdapter(adapter);

        int[] async_operations = {0};

        async_operations[0]++;
        db.collection("StoreItem").whereEqualTo("storeId", id).get(source).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document_1 : task.getResult()) {
                    StoreItem si = document_1.toObject(StoreItem.class);
                    if (si.cartQuantity > 0) {
                        async_operations[0]++;
                        db.collection("Item").document(si.itemId).get(source).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                DocumentSnapshot document_2 = task1.getResult();
                                if (document_2.exists()) {
                                    Item i = document_2.toObject(Item.class);

                                    float p = 0;
                                    if (Objects.requireNonNull(i).stores.containsKey(id))
                                        p = i.stores.get(id);

                                    ItemData iData = new ItemData();
                                    if (i.users.containsKey(Objects.requireNonNull(mAuth.getCurrentUser()).getUid()))
                                        iData.item_name = i.users.get(mAuth.getCurrentUser().getUid());
                                    else {
                                        Map.Entry<String, String> entry = i.users.entrySet().iterator().next();
                                        iData.item_name = entry.getValue();
                                    }
                                    iData.item_quantity = si.cartQuantity;
                                    iData.item_price = p;
                                    iData.itemId = si.itemId;
                                    if (i.barcode.equals("")) iData.imageId = si.itemId;
                                    else iData.imageId = i.barcode;
                                    itemData.add(iData);
                                    async_operations[0]--;
                                }
                            }
                        });
                    }
                }
                async_operations[0]--;
            }
        });

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (async_operations[0] == 0) {
                    everything_loaded = true;

                    itemData.sort(Comparator.comparing(i -> i.item_name.toLowerCase()));
                    for (ItemData i : itemData) {
                        item_names.add(i.item_name);
                        item_quantities.add(i.item_quantity);
                        item_prices.add(i.item_price);
                        item_ids.add(i.itemId);
                        imageIds.add(i.imageId);
                    }

                    ab.setTitle(getResources().getString(R.string.checkout) + " " + (current_item + 1) + " / " + item_names.size());

                    if (current_item == item_names.size() - 1) {
                        confirm.setVisibility(View.VISIBLE);
                    } else {
                        next.setVisibility(View.VISIBLE);
                    }

                    pos.setText(String.valueOf(current_item + 1));
                    name.setText(item_names.get(current_item));
                    quantity.setText(String.valueOf(item_quantities.get(current_item)));
                    price.setText(String.valueOf(item_prices.get(current_item)));
                    if (item_prices.get(current_item) == 0) {
                        price.setVisibility(View.INVISIBLE);
                        TextView euro = findViewById(R.id.euroCheckout);
                        TextView priceCheckout = findViewById(R.id.priceCheckout);
                        euro.setVisibility(View.INVISIBLE);
                        priceCheckout.setVisibility(View.INVISIBLE);
                    } else {
                        price.setVisibility(View.VISIBLE);
                        TextView euro = findViewById(R.id.euroCheckout);
                        TextView priceCheckout = findViewById(R.id.priceCheckout);
                        euro.setVisibility(View.VISIBLE);
                        priceCheckout.setVisibility(View.VISIBLE);
                    }
                    updateItemImage();
                    updateList();
                } else {
                    timerHandler.postDelayed(this, 100);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);

        next.setOnClickListener(v -> {
            if (!everything_loaded) return;
            previous.setVisibility(View.VISIBLE);

            current_item++;
            adapter.item_position++;
            pos.setText(String.valueOf(current_item + 1));
            name.setText(item_names.get(current_item));
            quantity.setText(String.valueOf(item_quantities.get(current_item)));
            price.setText(String.valueOf(item_prices.get(current_item)));
            if (item_prices.get(current_item) == 0) {
                price.setVisibility(View.INVISIBLE);
                TextView euro = findViewById(R.id.euroCheckout);
                TextView priceCheckout = findViewById(R.id.priceCheckout);
                euro.setVisibility(View.INVISIBLE);
                priceCheckout.setVisibility(View.INVISIBLE);
            } else {
                price.setVisibility(View.VISIBLE);
                TextView euro = findViewById(R.id.euroCheckout);
                TextView priceCheckout = findViewById(R.id.priceCheckout);
                euro.setVisibility(View.VISIBLE);
                priceCheckout.setVisibility(View.VISIBLE);
            }

            if (current_item == item_names.size() - 1) {
                next.setVisibility(View.INVISIBLE);
                confirm.setVisibility(View.VISIBLE);
            }

            ab.setTitle(getResources().getString(R.string.checkout) + " " + (current_item + 1) + " / " + item_names.size());
            updateItemImage();
            updateList();
        });

        previous.setOnClickListener(v -> {
            if (!everything_loaded) return;
            next.setVisibility(View.VISIBLE);
            confirm.setVisibility(View.INVISIBLE);

            current_item--;
            adapter.item_position--;
            pos.setText(String.valueOf(current_item + 1));
            name.setText(item_names.get(current_item));
            quantity.setText(String.valueOf(item_quantities.get(current_item)));
            price.setText(String.valueOf(item_prices.get(current_item)));
            if (item_prices.get(current_item) == 0) {
                price.setVisibility(View.INVISIBLE);
                TextView euro = findViewById(R.id.euroCheckout);
                TextView priceCheckout = findViewById(R.id.priceCheckout);
                euro.setVisibility(View.INVISIBLE);
                priceCheckout.setVisibility(View.INVISIBLE);
            } else {
                price.setVisibility(View.VISIBLE);
                TextView euro = findViewById(R.id.euroCheckout);
                TextView priceCheckout = findViewById(R.id.priceCheckout);
                euro.setVisibility(View.VISIBLE);
                priceCheckout.setVisibility(View.VISIBLE);
            }

            if (current_item == 0) {
                previous.setVisibility(View.INVISIBLE);
            }

            ab.setTitle(getResources().getString(R.string.checkout) + " " + (current_item + 1) + " / " + item_names.size());
            updateItemImage();
            updateList();
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!everything_loaded) return;

                int[] async_operations = {0};
                Map<String, PantryList> pantryLists = new HashMap<>();

                for (Map.Entry<String, Map<String, String>> entry_1 : quantitiesPerPantry.entrySet()) {
                    String itemId = entry_1.getKey();

                    for (Map.Entry<String, String> entry_2 : entry_1.getValue().entrySet()) {
                        String pantryId = entry_2.getKey();
                        int quantity = Integer.parseInt(entry_2.getValue());
                        if (quantity > 0) {
                            async_operations[0]++;
                            db.collection("PantryItem").whereEqualTo("pantryId", pantryId)
                                    .whereEqualTo("itemId", itemId).get(source).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (task.getResult().size() == 0) {
                                        PantryItem pi = new PantryItem(pantryId, itemId, quantity, quantity);
                                        async_operations[0]++;
                                        db.collection("PantryItem").add(pi).addOnCompleteListener(task12 -> {
                                            if (task12.isSuccessful()) async_operations[0]--;
                                        });

                                        async_operations[0]++;
                                        db.collection("PantryList").document(pantryId).get(source).addOnCompleteListener(task13 -> {
                                            if (task13.isSuccessful()) {
                                                DocumentSnapshot document_1 = task13.getResult();
                                                PantryList p = document_1.toObject(PantryList.class);
                                                async_operations[0]++;
                                                db.collection("PantryList").document(pantryId)
                                                        .update("number_of_items", Objects.requireNonNull(p).number_of_items + 1).addOnCompleteListener(task131 -> {
                                                    if (task131.isSuccessful())
                                                        async_operations[0]--;
                                                });
                                                async_operations[0]--;
                                            }
                                        });
                                    } else {
                                        for (QueryDocumentSnapshot document_1 : task.getResult()) {
                                            PantryItem pi = document_1.toObject(PantryItem.class);
                                            async_operations[0]++;
                                            db.collection("PantryItem").document(document_1.getId())
                                                    .update("quantity", pi.quantity + quantity).addOnCompleteListener(task14 -> {
                                                if (task14.isSuccessful()) async_operations[0]--;
                                            });

                                            if (pi.quantity + quantity > pi.idealQuantity) {
                                                async_operations[0]++;
                                                db.collection("PantryItem").document(document_1.getId())
                                                        .update("idealQuantity", pi.quantity + quantity).addOnCompleteListener(task15 -> {
                                                    if (task15.isSuccessful())
                                                        async_operations[0]--;
                                                });
                                            }
                                        }
                                    }
                                    async_operations[0]--;
                                }
                            });

                            async_operations[0]++;
                            db.collection("PantryList").document(pantryId).get(source)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document_1 = task.getResult();
                                            PantryList p = document_1.toObject(PantryList.class);
                                            if (!pantryLists.containsKey(document_1.getId()))
                                                pantryLists.put(document_1.getId(), p);
                                            async_operations[0]--;
                                        }
                                    });
                        }
                    }
                }

                Map<String, Integer> completeItemsPerStoreList = new HashMap<>();
                Boolean[] entered_once = {false};
                Boolean[] entered_twice = {false};

                Handler timerHandler = new Handler();
                Runnable timerRunnable = new Runnable() {

                    @Override
                    public void run() {
                        if (async_operations[0] == 0 && !entered_once[0]) {
                            entered_once[0] = true;

                            for (Map.Entry<String, Map<String, String>> entry_1 : quantitiesPerPantry.entrySet()) {
                                String itemId = entry_1.getKey();
                                Map<String, Integer> totalQuantitiesPerUser = new HashMap<>();
                                for (Map.Entry<String, String> entry_2 : entry_1.getValue().entrySet()) {
                                    String pantryId = entry_2.getKey();
                                    int quantity = Integer.parseInt(entry_2.getValue());
                                    if (quantity > 0) {
                                        PantryList p = pantryLists.get(pantryId);
                                        for (String user : Objects.requireNonNull(p).users) {
                                            int q = 0;
                                            if (totalQuantitiesPerUser.containsKey(user))
                                                q = totalQuantitiesPerUser.get(user);
                                            totalQuantitiesPerUser.put(user, q + quantity);
                                        }
                                    }
                                }

                                for (Map.Entry<String, Integer> entry_2 : totalQuantitiesPerUser.entrySet()) {
                                    String user = entry_2.getKey();
                                    int quantity = entry_2.getValue();

                                    async_operations[0]++;
                                    db.collection("StoreList").whereArrayContains("users", user)
                                            .get(source).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document_2 : task.getResult()) {
                                                async_operations[0]++;
                                                db.collection("StoreItem").whereEqualTo("storeId", document_2.getId())
                                                        .whereEqualTo("itemId", itemId).get(source).addOnCompleteListener(task19 -> {
                                                    if (task19.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document_3 : task19.getResult()) {
                                                            StoreItem si = document_3.toObject(StoreItem.class);

                                                            if (si.storeId.equals(id)) {
                                                                async_operations[0]++;
                                                                db.collection("StoreItem").document(document_3.getId())
                                                                        .update("cartQuantity", si.cartQuantity - quantity).addOnCompleteListener(task16 -> {
                                                                    if (task16.isSuccessful())
                                                                        async_operations[0]--;
                                                                });
                                                            }
                                                            int sq = si.quantity - quantity;
                                                            if (sq <= 0) {
                                                                sq = 0;
                                                                int ci = 1;
                                                                if (completeItemsPerStoreList.containsKey(si.storeId))
                                                                    ci = completeItemsPerStoreList.get(si.storeId);
                                                                completeItemsPerStoreList.put(si.storeId, ci);
                                                            }
                                                            async_operations[0]++;
                                                            db.collection("StoreItem").document(document_3.getId())
                                                                    .update("quantity", sq).addOnCompleteListener(task17 -> {
                                                                if (task17.isSuccessful())
                                                                    async_operations[0]--;
                                                            });
                                                        }
                                                        async_operations[0]--;
                                                    }
                                                });
                                            }
                                            async_operations[0]--;
                                        }
                                    });
                                }
                            }
                            timerHandler.postDelayed(this, 100);
                        } else if (async_operations[0] == 0 && !entered_twice[0]) {
                            entered_twice[0] = true;

                            for (Map.Entry<String, Integer> entry : completeItemsPerStoreList.entrySet()) {
                                String storeId = entry.getKey();
                                int ci = entry.getValue();

                                async_operations[0]++;
                                db.collection("StoreList").document(storeId).get(source)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot d = task.getResult();
                                                StoreList s = d.toObject(StoreList.class);
                                                async_operations[0]++;
                                                db.collection("StoreList").document(storeId)
                                                        .update("number_of_items", Objects.requireNonNull(s).number_of_items - ci)
                                                        .addOnCompleteListener(task18 -> {
                                                            if (task18.isSuccessful()) {
                                                                async_operations[0]--;
                                                            }
                                                        });
                                                async_operations[0]--;
                                            }
                                        });
                            }

                            timerHandler.postDelayed(this, 100);
                        } else if (async_operations[0] == 0) {
                            finish();
                        } else {
                            timerHandler.postDelayed(this, 100);
                        }
                    }
                };
                timerHandler.postDelayed(timerRunnable, 0);
            }
        });
    }

    public void updateItemImage() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + imageIds.get(current_item));
        StorageReference imagesRef = storageRef.child(imageIds.get(current_item));

        File[] files = storageDir.listFiles();

        assert files != null;
        if (files.length == 0) {
            imagesRef.listAll()
                    .addOnSuccessListener(listResult -> {
                        List<StorageReference> pics = listResult.getItems();
                        if (pics.size() != 0) {
                            String currentPhotoPath;
                            File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + imageIds.get(current_item)).getAbsolutePath() + "/" + pics.get(0).getName());
                            currentPhotoPath = localFile.getAbsolutePath();
                            pics.get(0).getFile(localFile).addOnSuccessListener(taskSnapshot -> image.setImageURI(Uri.fromFile(new File(currentPhotoPath))));
                        }
                    });
        } else {
            image.setImageURI(Uri.fromFile(files[0]));
        }
    }

    public void updateList() {
        pantryData.clear();

        boolean first_time_for_item = !quantitiesPerPantry.containsKey(item_ids.get(current_item));
        if (first_time_for_item) {
            Map<String, String> m = new HashMap<>();
            quantitiesPerPantry.put(item_ids.get(current_item), m);
        }

        int[] async_operations = {0};

        async_operations[0]++;
        db.collection("PantryList").get(source).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document_1 : task.getResult()) {
                    PantryList pantry = document_1.toObject(PantryList.class);
                    if (!pantry.users.contains(mAuth.getUid())) continue;
                    async_operations[0]++;
                    db.collection("PantryItem").whereEqualTo("pantryId", document_1.getId())
                            .whereEqualTo("itemId", item_ids.get(current_item)).get(source).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            if (first_time_for_item) {
                                Objects.requireNonNull(quantitiesPerPantry.get(item_ids.get(current_item))).put(document_1.getId(), "0");
                            }
                            if (task1.getResult().size() == 0) {
                                PantryData pd = new PantryData();
                                pd.pantryId = document_1.getId();
                                pd.pantry_name = pantry.name;
                                pd.quantity_needed = 0;
                                pd.pantry_quantity = Objects.requireNonNull(quantitiesPerPantry.get(item_ids.get(current_item))).get(document_1.getId());
                                pantryData.add(pd);
                            } else {
                                for (QueryDocumentSnapshot document_2 : task1.getResult()) {
                                    PantryItem pi = document_2.toObject(PantryItem.class);
                                    PantryData pd = new PantryData();
                                    pd.pantryId = document_1.getId();
                                    pd.pantry_name = pantry.name;
                                    pd.quantity_needed = pi.idealQuantity - pi.quantity;
                                    pd.pantry_quantity = Objects.requireNonNull(quantitiesPerPantry.get(item_ids.get(current_item))).get(document_1.getId());
                                    pantryData.add(pd);
                                }
                            }
                            async_operations[0]--;
                        }
                    });
                }
                async_operations[0]--;
            }
        });

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (async_operations[0] == 0) {

                    pantryIds.clear();
                    pantryNames.clear();
                    quantitiesNeeded.clear();
                    pantryQuantities.clear();

                    pantryData.sort(Comparator.comparing(i -> i.pantry_name.toLowerCase()));
                    for (PantryData d : pantryData) {
                        pantryIds.add(d.pantryId);
                        pantryNames.add(d.pantry_name);
                        quantitiesNeeded.add(d.quantity_needed);
                        pantryQuantities.add(d.pantry_quantity);
                    }

                    list.invalidateViews();
                } else {
                    timerHandler.postDelayed(this, 100);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();    //Call the back button's method
            return true;
        }// If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    private static class ItemData {
        String itemId;
        String item_name;
        Integer item_quantity;
        Float item_price;
        String imageId;
    }

    private static class PantryData {
        String pantryId;
        String pantry_name;
        Integer quantity_needed;
        String pantry_quantity;
    }
}
