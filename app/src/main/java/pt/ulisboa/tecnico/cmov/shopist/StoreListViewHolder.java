package pt.ulisboa.tecnico.cmov.shopist;

import android.view.View;
import android.widget.TextView;

public class StoreListViewHolder {
    TextView storeListPosition;
    TextView storeListItemName;
    TextView storeListItemQuantity;
    TextView itemPrice;

    StoreListViewHolder(View v) {
        storeListPosition = v.findViewById(R.id.store_list_position);
        storeListItemName = v.findViewById(R.id.store_list_item_name);
        storeListItemQuantity = v.findViewById(R.id.store_list_item_quantity);
        itemPrice = v.findViewById(R.id.item_price);
    }
}
