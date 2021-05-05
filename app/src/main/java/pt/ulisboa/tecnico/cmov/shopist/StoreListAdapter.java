package pt.ulisboa.tecnico.cmov.shopist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;

public class StoreListAdapter extends ArrayAdapter<String> {

    Context context;
    List<String> item_names;
    List<Integer> item_quantities;
    List<Float> item_prices;
    String storeId;
    List<String> itemIds;

    ListView list;

    boolean cart;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public StoreListAdapter(Context context, List<String> item_names, List<Integer> item_quantities, List<Float> item_prices,
                            boolean cart, String storeId, List<String> itemIds, ListView list) {
        super(context, R.layout.store_list_item, R.id.store_list_item_name, item_names);
        this.context = context;

        this.item_names = item_names;
        this.item_quantities = item_quantities;
        this.item_prices = item_prices;
        this.cart = cart;
        this.storeId = storeId;
        this.itemIds = itemIds;

        this.list = list;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        StoreListViewHolder holder = null;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.store_list_item, parent, false);
            holder = new StoreListViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (StoreListViewHolder) view.getTag();
        }

        holder.storeListPosition.setText(String.valueOf(position + 1));
        holder.storeListItemName.setText(item_names.get(position));
        holder.storeListItemQuantity.setText(item_quantities.get(position).toString());
        holder.itemPrice.setText(item_prices.get(position).toString() + " €");

        final String q = (String) holder.storeListItemQuantity.getText();

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, StoreItemActivity.class);
                intent.putExtra("ID", itemIds.get(position));
                intent.putExtra("StoreId", storeId);
                context.startActivity(intent);
            }
        });

        if(cart) {
            view.findViewById(R.id.moveToCart).setVisibility(View.INVISIBLE);
        }

        view.findViewById(R.id.decrement_item_quantity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cart) {

                }
                else {
                    final int quantity = Integer.parseInt(q);
                    if(quantity == 0) return;
                    db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()) {
                                for(QueryDocumentSnapshot document_1 : task.getResult()) {
                                    db.collection("StoreItem").document(document_1.getId()).update("quantity", quantity - 1)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        item_quantities.set(position, quantity - 1);
                                                        list.invalidateViews();
                                                    }
                                                }
                                            });
                                    return;
                                }
                            }
                        }
                    });
                }
            }
        });

        view.findViewById(R.id.increment_item_quantity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cart) {

                }
                else {
                    final int quantity = Integer.parseInt(q);
                    db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()) {
                                for(QueryDocumentSnapshot document_1 : task.getResult()) {
                                    db.collection("StoreItem").document(document_1.getId()).update("quantity", quantity + 1)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        item_quantities.set(position, quantity + 1);
                                                        list.invalidateViews();
                                                    }
                                                }
                                            });
                                    return;
                                }
                            }
                        }
                    });
                }
            }
        });

        view.findViewById(R.id.moveToCart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setTitle("Move to cart");

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        return view;
    }
}
