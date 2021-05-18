package pt.ulisboa.tecnico.cmov.shopist;

import android.view.View;
import android.widget.TextView;

public class CheckoutListViewHolder {

    final TextView pantryName;
    final TextView quantityNeeded;
    final TextView pantryQuantity;

    public CheckoutListViewHolder(View v) {
        pantryName = v.findViewById(R.id.checkout_pantry_name);
        quantityNeeded = v.findViewById(R.id.checkout_quantity_needed);
        pantryQuantity = v.findViewById(R.id.checkout_pantry_quantity);
    }
}
