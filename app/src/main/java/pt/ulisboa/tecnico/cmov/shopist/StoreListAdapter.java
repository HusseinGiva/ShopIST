package pt.ulisboa.tecnico.cmov.shopist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    TextView totalCost;
    private final FirebaseFirestore db;
    private final StoreListActivity activity;

    public StoreListAdapter(Context context, List<String> item_names, List<Integer> item_quantities, List<Float> item_prices,
                            boolean cart, String storeId, List<String> itemIds, ListView list, StoreListActivity activity, TextView totalCost) {
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

        this.activity = activity;
        this.totalCost = totalCost;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        StoreListViewHolder holder;

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

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, StoreItemActivity.class);
            intent.putExtra("ID", itemIds.get(position));
            intent.putExtra("StoreId", storeId);
            context.startActivity(intent);
        });

        if (cart) {
            view.findViewById(R.id.moveToCart).setVisibility(View.INVISIBLE);
        }

        view.findViewById(R.id.decrement_item_quantity).setOnClickListener(v -> {
            final int quantity = Integer.parseInt(q);
            if (!cart && quantity == 0) return;
            db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document_1 : task.getResult()) {
                                String field;
                                if (cart) field = "cartQuantity";
                                else field = "quantity";
                                db.collection("StoreItem").document(document_1.getId()).update(field, quantity - 1)
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                if (cart) {
                                                    String s_total_cost = (String) totalCost.getText();
                                                    String[] a_total_cost = s_total_cost.split(" ");
                                                    float total_cost = Float.parseFloat(a_total_cost[0]);
                                                    total_cost -= item_prices.get(position);
                                                    totalCost.setText(String.valueOf(total_cost));
                                                    if (quantity == 1) {
                                                        activity.goToCart();
                                                        return;
                                                    }
                                                }
                                                item_quantities.set(position, quantity - 1);
                                                list.invalidateViews();
                                            }
                                        });
                                return;
                            }
                        }
                    });
        });

        view.findViewById(R.id.increment_item_quantity).setOnClickListener(v -> {
            final int quantity = Integer.parseInt(q);
            db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document_1 : task.getResult()) {
                                StoreItem si = document_1.toObject(StoreItem.class);
                                String field;
                                if (cart) {
                                    if (quantity == si.quantity) return;
                                    field = "cartQuantity";
                                } else field = "quantity";
                                db.collection("StoreItem").document(document_1.getId()).update(field, quantity + 1)
                                        .addOnCompleteListener(task12 -> {
                                            if (task12.isSuccessful()) {
                                                if (cart) {
                                                    String s_total_cost = (String) totalCost.getText();
                                                    String[] a_total_cost = s_total_cost.split(" ");
                                                    float total_cost = Float.parseFloat(a_total_cost[0]);
                                                    total_cost += item_prices.get(position);
                                                    totalCost.setText(String.valueOf(total_cost));
                                                }
                                                item_quantities.set(position, quantity + 1);
                                                list.invalidateViews();
                                            }
                                        });
                                return;
                            }
                        }
                    });
        });

        view.findViewById(R.id.moveToCart).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle("Move to cart");

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view1 = inflater.inflate(R.layout.dialog_move_to_cart, null);

            view1.findViewById(R.id.decrement_move_to_cart_quantity).setOnClickListener(v1 -> {
                EditText e = (EditText) view1.findViewById(R.id.move_to_cart_quantity);
                int quantity = Integer.parseInt(String.valueOf(e.getText()));
                if (quantity == 0) return;
                e.setText(String.valueOf(quantity - 1));
            });

            view1.findViewById(R.id.increment_move_to_cart_quantity).setOnClickListener(v12 -> {
                EditText e = (EditText) view1.findViewById(R.id.move_to_cart_quantity);
                int quantity = Integer.parseInt(String.valueOf(e.getText()));
                int max_quantity = Integer.parseInt(q);
                if (quantity == max_quantity) return;
                e.setText(String.valueOf(Integer.parseInt(String.valueOf(e.getText())) + 1));
            });

            builder.setView(view1);
            builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                // User clicked OK button
                EditText e = (EditText) view1.findViewById(R.id.move_to_cart_quantity);
                int quantity = Integer.parseInt(String.valueOf(e.getText()));
                db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document_1 : task.getResult()) {
                                    db.collection("StoreItem").document(document_1.getId()).update("cartQuantity", quantity)
                                            .addOnCompleteListener(task13 -> {
                                                if (task13.isSuccessful()) {
                                                    activity.goToCart();
                                                }
                                            });
                                    return;
                                }
                            }
                        });
            });
            builder.setNegativeButton(R.string.cancel, (dialog, id) -> {
                // User cancelled the dialog
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        });

        return view;
    }
}
