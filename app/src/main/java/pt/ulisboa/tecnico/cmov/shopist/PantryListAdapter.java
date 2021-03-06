package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;
import java.util.Objects;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class PantryListAdapter extends ArrayAdapter<String> {

    final Context context;
    final List<String> item_names;
    final List<Integer> item_quantities;
    final List<Integer> item_ideal_quantities;
    final List<String> itemIds;
    final List<String> imageIds;
    final String pantryId;
    final ListView list;
    private final Source source;
    private final FirebaseFirestore db;

    public PantryListAdapter(Context context, List<String> item_names, List<Integer> item_quantities,
                             List<Integer> item_ideal_quantities, List<String> itemIds, List<String> imageIds, String pantryId, ListView list) {
        super(context, R.layout.pantry_list_item, R.id.pantry_list_item_name, item_names);
        this.context = context;
        this.item_names = item_names;
        this.item_quantities = item_quantities;
        this.item_ideal_quantities = item_ideal_quantities;
        this.itemIds = itemIds;
        this.imageIds = imageIds;
        this.pantryId = pantryId;
        this.list = list;

        if (isConnected(getContext().getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
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
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        PantryListViewHolder holder;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.pantry_list_item, parent, false);
            holder = new PantryListViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (PantryListViewHolder) view.getTag();
        }

        holder.pantryListPosition.setText(String.valueOf(position + 1));
        holder.pantryListItemName.setText(item_names.get(position));
        holder.pantryListItemQuantity.setText(item_quantities.get(position).toString() + " / " + item_ideal_quantities.get(position).toString());

        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + imageIds.get(position));
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child(imageIds.get(position));

        File[] files = storageDir.listFiles();
        ImageView i = view.findViewById(R.id.pantry_list_item_image);

        assert files != null;
        if (files.length == 0) {
            imagesRef.listAll()
                    .addOnSuccessListener(listResult -> {
                        List<StorageReference> pics = listResult.getItems();
                        if (pics.size() != 0) {
                            String currentPhotoPath;
                            File localFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + imageIds.get(position)).getAbsolutePath() + "/" + pics.get(0).getName());
                            currentPhotoPath = localFile.getAbsolutePath();
                            pics.get(0).getFile(localFile).addOnSuccessListener(taskSnapshot -> i.setImageURI(Uri.fromFile(new File(currentPhotoPath))));
                        }
                    });
        } else {
            i.setImageURI(Uri.fromFile(files[0]));
        }

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, PantryItemActivity.class);
            intent.putExtra("ID", itemIds.get(position));
            intent.putExtra("PantryId", pantryId);
            context.startActivity(intent);
        });

        view.findViewById(R.id.decrement_p_item_quantity).setOnClickListener(v -> {
            if (item_quantities.get(position) == 0) return;
            db.collection("PantryItem").whereEqualTo("pantryId", pantryId)
                    .whereEqualTo("itemId", itemIds.get(position)).get(source).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document_1 : task.getResult()) {
                        db.collection("PantryItem").document(document_1.getId())
                                .update("quantity", item_quantities.get(position) - 1).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                updateStoreLists(itemIds.get(position), 1);
                                item_quantities.set(position, item_quantities.get(position) - 1);
                                list.invalidateViews();
                            }
                        });
                    }
                }
            });
        });

        view.findViewById(R.id.increment_p_item_quantity).setOnClickListener(v ->
                db.collection("PantryItem").whereEqualTo("pantryId", pantryId)
                        .whereEqualTo("itemId", itemIds.get(position)).get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document_1 : task.getResult()) {
                            db.collection("PantryItem").document(document_1.getId())
                                    .update("quantity", item_quantities.get(position) + 1).addOnCompleteListener(task12 -> {
                                if (task12.isSuccessful()) {

                                    if (item_quantities.get(position).equals(item_ideal_quantities.get(position))) {
                                        db.collection("PantryItem").document(document_1.getId())
                                                .update("idealQuantity", item_quantities.get(position) + 1).addOnCompleteListener(task121 -> {
                                            if (task121.isSuccessful()) {
                                                item_quantities.set(position, item_quantities.get(position) + 1);
                                                item_ideal_quantities.set(position, item_ideal_quantities.get(position) + 1);
                                                list.invalidateViews();
                                            }
                                        });
                                    } else {
                                        updateStoreLists(itemIds.get(position), -1);
                                        item_quantities.set(position, item_quantities.get(position) + 1);
                                        list.invalidateViews();
                                    }
                                }
                            });
                        }
                    }
                }));

        return view;
    }

    public void updateStoreLists(String itemId, int v) {
        db.collection("PantryList").document(pantryId).get(source)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot d1 = task.getResult();
                        PantryList p = d1.toObject(PantryList.class);
                        for (String user : Objects.requireNonNull(p).users) {
                            db.collection("StoreList")
                                    .whereArrayContains("users", user)
                                    .get(source)
                                    .addOnCompleteListener(task12 -> {
                                        if (task12.isSuccessful()) {
                                            for (QueryDocumentSnapshot document_1 : task12.getResult()) {
                                                StoreList s = document_1.toObject(StoreList.class);
                                                db.collection("StoreItem").whereEqualTo("storeId", document_1.getId())
                                                        .whereEqualTo("itemId", itemId).get(source).addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document_2 : task1.getResult()) {
                                                            StoreItem si = document_2.toObject(StoreItem.class);
                                                            if (!(v == -1 && si.quantity == 0)) {
                                                                db.collection("StoreItem").document(document_2.getId())
                                                                        .update("quantity", si.quantity + v);
                                                                if (si.quantity + v == 0) {
                                                                    db.collection("StoreList").document(document_1.getId())
                                                                            .update("number_of_items", s.number_of_items - 1);
                                                                } else if (si.quantity == 0 && v == 1) {
                                                                    db.collection("StoreList").document(document_1.getId())
                                                                            .update("number_of_items", s.number_of_items + 1);
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}
