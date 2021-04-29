package pt.ulisboa.tecnico.cmov.shopist;

import android.view.View;
import android.widget.TextView;

public class ListViewHolder {

    public static final String LIST = "LIST";
    public static final String PANTRY = "PANTRY";
    public static final String STORE = "STORE";
    TextView listName;
    TextView driveTime;
    TextView n_items;
    TextView pantryListPosition;
    TextView pantryListItemName;
    TextView pantryListItemQuantity;

    ListViewHolder(View v, String list_type) {

        if (list_type.equals(LIST)) {
            listName = v.findViewById(R.id.list_name);
            driveTime = v.findViewById(R.id.drive_time);
            n_items = v.findViewById(R.id.n_items);
        } else if (list_type.equals(PANTRY)) {
            pantryListPosition = v.findViewById(R.id.pantry_list_position);
            pantryListItemName = v.findViewById(R.id.pantry_list_item_name);
            pantryListItemQuantity = v.findViewById(R.id.pantry_list_item_quantity);
        }
    }
}
