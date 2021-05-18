package pt.ulisboa.tecnico.cmov.shopist;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class StoreListViewHolder {
    final TextView storeListPosition;
    final TextView storeListItemName;
    final TextView storeListItemQuantity;
    final TextView itemPrice;
    final TextView euro;
    final ImageView image;

    StoreListViewHolder(View v) {
        storeListPosition = v.findViewById(R.id.store_list_position);
        storeListItemName = v.findViewById(R.id.store_list_item_name);
        storeListItemQuantity = v.findViewById(R.id.store_list_item_quantity);
        itemPrice = v.findViewById(R.id.item_price);
        euro = v.findViewById(R.id.textView6);
        image = v.findViewById(R.id.store_list_item_image);
    }
}
