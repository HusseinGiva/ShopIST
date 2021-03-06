package pt.ulisboa.tecnico.cmov.shopist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class StoreListAdapter extends ArrayAdapter<String> {

    final Context context;
    final List<String> item_names;
    final List<Integer> item_quantities;
    final List<Float> item_prices;
    final String storeId;
    final List<String> itemIds;
    final List<String> imageIds;
    final ListView list;
    final boolean cart;
    final TextView totalCost;
    final List<Integer> cart_item_quantities;
    private final FirebaseFirestore db;
    private final StoreListActivity activity;
    private final Source source;

    public StoreListAdapter(Context context, List<String> item_names, List<Integer> item_quantities, List<Float> item_prices,
                            boolean cart, String storeId, List<String> itemIds, List<String> imageIds, ListView list,
                            StoreListActivity activity, TextView totalCost, List<Integer> cart_item_quantities) {
        super(context, R.layout.store_list_item, R.id.store_list_item_name, item_names);
        this.context = context;

        this.item_names = item_names;
        this.item_quantities = item_quantities;
        this.item_prices = item_prices;
        this.cart = cart;
        this.storeId = storeId;
        this.itemIds = itemIds;
        this.imageIds = imageIds;

        this.list = list;

        if (isConnected(getContext().getApplicationContext()))
            source = Source.DEFAULT;
        else
            source = Source.CACHE;

        db = FirebaseFirestore.getInstance();

        this.activity = activity;
        this.totalCost = totalCost;
        this.cart_item_quantities = cart_item_quantities;
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
        DecimalFormat df = new DecimalFormat("###.##");
        double value = Math.round(item_prices.get(position) * 100.0) / 100.0;
        holder.itemPrice.setText(df.format(value));
        if (value == 0) {
            holder.itemPrice.setVisibility(View.INVISIBLE);
            holder.euro.setVisibility(View.INVISIBLE);
        } else {
            holder.itemPrice.setVisibility(View.VISIBLE);
            holder.euro.setVisibility(View.VISIBLE);
        }

        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + imageIds.get(position));
        File[] files = storageDir.listFiles();
        assert files != null;
        if (files.length != 0) {
            holder.image.setImageURI(Uri.fromFile(files[0]));
        } else {
            holder.image.setImageResource(R.drawable.ist_logo);
        }

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, StoreItemActivity.class);
            intent.putExtra("ID", itemIds.get(position));
            intent.putExtra("StoreId", storeId);
            context.startActivity(intent);
        });

        if (cart) {
            view.findViewById(R.id.moveToCart).setVisibility(View.INVISIBLE);
        } else {
            TextView t = view.findViewById(R.id.moveToCartQuantity);
            t.setText(String.valueOf(this.cart_item_quantities.get(position)));
        }

        view.findViewById(R.id.decrement_item_quantity).setOnClickListener(v -> db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                .get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document_1 : task.getResult()) {
                            String field;
                            if (cart) field = "cartQuantity";
                            else field = "quantity";
                            db.collection("StoreItem").document(document_1.getId()).update(field, item_quantities.get(position) - 1)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            if (cart) {
                                                String s_total_cost = (String) totalCost.getText();
                                                String[] a_total_cost = s_total_cost.split(" ");
                                                float total_cost = Float.parseFloat(a_total_cost[0]);
                                                total_cost -= item_prices.get(position);
                                                Double value2 = Math.round(total_cost * 100.0) / 100.0;
                                                totalCost.setText(String.valueOf(df.format(value2)));
                                                if (item_quantities.get(position) == 1) {
                                                    activity.goToCart();
                                                    return;
                                                }
                                            } else if (item_quantities.get(position) == 1) {
                                                db.collection("StoreList").document(storeId).get(source)
                                                        .addOnCompleteListener(task22 -> {
                                                            if (task22.isSuccessful()) {
                                                                DocumentSnapshot document_2 = task22.getResult();
                                                                StoreList s = document_2.toObject(StoreList.class);
                                                                assert s != null;
                                                                db.collection("StoreList").document(storeId)
                                                                        .update("number_of_items", s.number_of_items - 1)
                                                                        .addOnCompleteListener(task2 -> {
                                                                            if (task2.isSuccessful()) {
                                                                                activity.goToStore();
                                                                            }
                                                                        });
                                                            }
                                                        });
                                                return;
                                            }
                                            item_quantities.set(position, item_quantities.get(position) - 1);
                                            list.invalidateViews();
                                        }
                                    });
                            return;
                        }
                    }
                }));

        view.findViewById(R.id.increment_item_quantity).setOnClickListener(v -> db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                .get(source).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document_1 : task.getResult()) {
                            StoreItem si = document_1.toObject(StoreItem.class);
                            String field;
                            if (cart) field = "cartQuantity";
                            else field = "quantity";
                            db.collection("StoreItem").document(document_1.getId()).update(field, item_quantities.get(position) + 1)
                                    .addOnCompleteListener(task12 -> {
                                        if (task12.isSuccessful()) {
                                            if (cart) {
                                                String s_total_cost = (String) totalCost.getText();
                                                String[] a_total_cost = s_total_cost.split(" ");
                                                float total_cost = Float.parseFloat(a_total_cost[0]);
                                                total_cost += item_prices.get(position);
                                                Double value2 = Math.round(total_cost * 100.0) / 100.0;
                                                totalCost.setText(String.valueOf(df.format(value2)));
                                            }
                                            item_quantities.set(position, item_quantities.get(position) + 1);
                                            list.invalidateViews();
                                        }
                                    });
                            return;
                        }
                    }
                }));

        view.findViewById(R.id.moveToCart).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle(R.string.moveToCart);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view1 = inflater.inflate(R.layout.dialog_move_to_cart, null);

            EditText e = view1.findViewById(R.id.move_to_cart_quantity);
            if (!cart) e.setText(String.valueOf(this.cart_item_quantities.get(position)));
            e.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String str = s.toString();
                    try {
                        int n = Integer.parseInt(str);
                        if (n < 0) e.setText("0");
                    } catch (NumberFormatException exp) {
                        if (!str.equals("")) e.setText("");
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            view1.findViewById(R.id.decrement_move_to_cart_quantity).setOnClickListener(v1 -> {
                String s = e.getText().toString();
                if (s.equals("") || s.equals("0")) e.setText("0");
                else {
                    int n = Integer.parseInt(s);
                    e.setText(String.valueOf(n - 1));
                }
            });

            view1.findViewById(R.id.increment_move_to_cart_quantity).setOnClickListener(v12 -> {
                String s = e.getText().toString();
                if (s.equals("")) e.setText("0");
                else {
                    int n = Integer.parseInt(s);
                    e.setText(String.valueOf(n + 1));
                }
            });

            builder.setView(view1);
            builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                // User clicked OK button
                int quantity;
                if (e.getText().toString().equals("")) {
                    quantity = 0;
                } else {
                    quantity = Integer.parseInt(e.getText().toString());
                }
                db.collection("StoreItem").whereEqualTo("storeId", storeId).whereEqualTo("itemId", itemIds.get(position))
                        .get(source).addOnCompleteListener(task -> {
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
