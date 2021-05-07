package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class PantryListAdapter extends ArrayAdapter<String> {

    Context context;
    List<String> item_names;
    List<Integer> item_quantities;
    List<Integer> item_ideal_quantities;
    ArrayList<String> itemIds;
    String pantryId;

    public PantryListAdapter(Context context, List<String> item_names, List<Integer> item_quantities, List<Integer> item_ideal_quantities, ArrayList<String> itemIds, String pantryId) {
        super(context, R.layout.pantry_list_item, R.id.pantry_list_item_name, item_names);
        this.context = context;
        this.item_names = item_names;
        this.item_quantities = item_quantities;
        this.item_ideal_quantities = item_ideal_quantities;
        this.itemIds = itemIds;
        this.pantryId = pantryId;
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

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, PantryItemActivity.class);
            intent.putExtra("ID", itemIds.get(position));
            intent.putExtra("PantryId", pantryId);
            context.startActivity(intent);
        });

        return view;
    }
}
