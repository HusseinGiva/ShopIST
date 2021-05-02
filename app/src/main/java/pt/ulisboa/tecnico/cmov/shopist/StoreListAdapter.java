package pt.ulisboa.tecnico.cmov.shopist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class StoreListAdapter extends ArrayAdapter<String> {

    Context context;
    List<String> item_names;
    List<Integer> item_quantities;
    List<Float> item_prices;
    String storeId;
    List<String> itemIds;

    boolean cart;

    public StoreListAdapter(Context context, List<String> item_names, List<Integer> item_quantities, List<Float> item_prices,
                            boolean cart, String storeId, List<String> itemIds) {
        super(context, R.layout.store_list_item, R.id.store_list_item_name, item_names);
        this.context = context;

        this.item_names = item_names;
        this.item_quantities = item_quantities;
        this.item_prices = item_prices;
        this.cart = cart;
        this.storeId = storeId;
        this.itemIds = itemIds;
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

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ItemActivity.class);
                intent.putExtra("ID", itemIds.get(position));
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

                }
            }
        });

        view.findViewById(R.id.increment_item_quantity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cart) {

                }
                else {

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
