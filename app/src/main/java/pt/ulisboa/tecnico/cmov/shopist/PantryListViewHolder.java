package pt.ulisboa.tecnico.cmov.shopist;

import android.view.View;
import android.widget.TextView;

public class PantryListViewHolder {

    final TextView pantryListPosition;
    final TextView pantryListItemName;
    final TextView pantryListItemQuantity;

    PantryListViewHolder(View v) {
        pantryListPosition = v.findViewById(R.id.pantry_list_position);
        pantryListItemName = v.findViewById(R.id.pantry_list_item_name);
        pantryListItemQuantity = v.findViewById(R.id.pantry_list_item_quantity);
    }
}
