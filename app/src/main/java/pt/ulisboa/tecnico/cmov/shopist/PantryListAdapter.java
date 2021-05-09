package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;

public class PantryListAdapter extends ArrayAdapter<String> {

    Context context;
    List<String> item_names;
    List<Integer> item_quantities;
    List<Integer> item_ideal_quantities;
    List<String> itemIds;
    List<String> imageIds;
    String pantryId;

    ListView list;

    private Source source;

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
        if(files.length == 0) {
            imagesRef.listAll()
                    .addOnSuccessListener(listResult -> {
                        List<StorageReference> pics = listResult.getItems();
                        if(pics.size() == 0) {
                            i.setImageResource(R.drawable.ist_logo);
                        }
                        else {
                            String currentPhotoPath;
                            File localFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + imageIds.get(position)).getAbsolutePath() + "/" + pics.get(0).getName());
                            currentPhotoPath = localFile.getAbsolutePath();
                            pics.get(0).getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                                i.setImageURI(Uri.fromFile(new File(currentPhotoPath)));

                            });
                        }
                    });
        }
        else {
            i.setImageURI(Uri.fromFile(files[0]));
        }

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, PantryItemActivity.class);
            intent.putExtra("ID", itemIds.get(position));
            intent.putExtra("PantryId", pantryId);
            context.startActivity(intent);
        });

        view.findViewById(R.id.decrement_p_item_quantity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item_quantities.get(position) == 0) return;
                db.collection("PantryItem").whereEqualTo("pantryId", pantryId)
                        .whereEqualTo("itemId", itemIds.get(position)).get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            for(QueryDocumentSnapshot document_1 : task.getResult()) {
                                db.collection("PantryItem").document(document_1.getId())
                                        .update("quantity", item_quantities.get(position) - 1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()) {
                                            item_quantities.set(position, item_quantities.get(position) - 1);
                                            list.invalidateViews();
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });

        view.findViewById(R.id.increment_p_item_quantity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("PantryItem").whereEqualTo("pantryId", pantryId)
                        .whereEqualTo("itemId", itemIds.get(position)).get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            for(QueryDocumentSnapshot document_1 : task.getResult()) {
                                db.collection("PantryItem").document(document_1.getId())
                                        .update("quantity", item_quantities.get(position) + 1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()) {

                                            if(item_quantities.get(position).equals(item_ideal_quantities.get(position))) {
                                                db.collection("PantryItem").document(document_1.getId())
                                                        .update("idealQuantity", item_quantities.get(position) + 1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()) {
                                                            item_quantities.set(position, item_quantities.get(position) + 1);
                                                            item_ideal_quantities.set(position, item_ideal_quantities.get(position) + 1);
                                                            list.invalidateViews();
                                                        }
                                                    }
                                                });
                                            }
                                            else {
                                                item_quantities.set(position, item_quantities.get(position) + 1);
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
        });

        return view;
    }
}
