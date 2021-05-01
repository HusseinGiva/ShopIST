package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class PantryListAdapter extends ArrayAdapter<String> {

    public static final String LIST = "LIST";
    public static final String PANTRY = "PANTRY";
    public static final String STORE = "STORE";
    Context context;
    List<String> item_names;
    List<Integer> item_quantities;

    public PantryListAdapter(Context context, List<String> item_names, List<Integer> item_quantities) {
        super(context, R.layout.pantry_list_item, R.id.pantry_list_item_name, item_names);
        this.context = context;

        this.item_names = item_names;
        this.item_quantities = item_quantities;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        PantryListViewHolder holder = null;

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
        holder.pantryListItemQuantity.setText(item_quantities.get(position).toString());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ItemActivity.class);
                context.startActivity(intent);
            }
        });

        return view;
    }
}
