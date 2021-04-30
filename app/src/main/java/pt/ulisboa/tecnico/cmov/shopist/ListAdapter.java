package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class ListAdapter extends ArrayAdapter<String> {

    public static final String LIST = "LIST";
    public static final String PANTRY = "PANTRY";
    public static final String STORE = "STORE";
    Context context;
    List<String> list_names;
    List<String> drive_times;
    List<Integer> n_items;
    List<String> pantry_item_names;
    List<Integer> pantry_item_quantities;
    String list_type;
    String tabSelected;
    List<String> pantryIds;
    List<String> storeIds;

    public ListAdapter(Context context, String list_type, List<String> list_names, List<String> drive_times, List<Integer> n_items,
                       List<String> pantry_item_names, List<Integer> pantry_item_quantities, String tabSelected, List<String> pantryIds, List<String> storeIds) {
        super(context, R.layout.list_item, R.id.list_name, list_names);
        this.context = context;
        this.list_type = list_type;

        this.list_names = list_names;
        this.drive_times = drive_times;
        this.n_items = n_items;

        this.pantry_item_names = pantry_item_names;
        this.pantry_item_quantities = pantry_item_quantities;
        this.tabSelected = tabSelected;
        this.pantryIds = pantryIds;
        this.storeIds = storeIds;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        ListViewHolder holder = null;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (list_type.equals(LIST)) {
                view = layoutInflater.inflate(R.layout.list_item, parent, false);
            } else if (list_type.equals(PANTRY)) {
                view = layoutInflater.inflate(R.layout.pantry_list_item, parent, false);
            }
            holder = new ListViewHolder(view, this.list_type);
            view.setTag(holder);
        } else {
            holder = (ListViewHolder) view.getTag();
        }

        if (list_type.equals(LIST)) {
            holder.listName.setText(list_names.get(position));
            holder.driveTime.setText("Drive time: " + drive_times.get(position));
            holder.n_items.setText(String.valueOf(n_items.get(position)));
        } else if (list_type.equals(PANTRY)) {
            holder.pantryListPosition.setText(String.valueOf(position));
            holder.pantryListItemName.setText(pantry_item_names.get(position));
            holder.pantryListItemQuantity.setText(pantry_item_quantities.get(position).toString());
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, ListActivity.class);
                intent.putExtra("TAB", tabSelected);
                //TODO - if
                intent.putExtra("ID", pantryIds.get(position));
                context.startActivity(intent);
            }
        });

        return view;
    }
}
