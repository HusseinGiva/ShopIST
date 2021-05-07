package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class ListAdapter extends ArrayAdapter<String> {

    Context context;
    List<String> list_names;
    List<String> drive_times;
    List<Integer> n_items;
    String list_type;
    String tabSelected;
    List<String> pantryIds;
    List<String> storeIds;

    public ListAdapter(Context context, String list_type, List<String> list_names, List<String> drive_times, List<Integer> n_items,
                       String tabSelected, List<String> pantryIds, List<String> storeIds) {
        super(context, R.layout.list_item, R.id.list_name, list_names);
        this.context = context;
        this.list_type = list_type;

        this.list_names = list_names;
        this.drive_times = drive_times;
        this.n_items = n_items;

        this.tabSelected = tabSelected;
        this.pantryIds = pantryIds;
        this.storeIds = storeIds;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        ListViewHolder holder;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.list_item, parent, false);
            holder = new ListViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ListViewHolder) view.getTag();
        }

        holder.listName.setText(list_names.get(position));
        if (drive_times.get(position) != null)
            holder.driveTime.setText(drive_times.get(position));
        else
            holder.driveTime.setText(getContext().getResources().getString(R.string.no_drivetime));
        holder.n_items.setText(String.valueOf(n_items.get(position)));

        view.setOnClickListener(v -> {

            if (tabSelected.equals(getContext().getResources().getString(R.string.pantry))) {
                Intent intent = new Intent(context, PantryListActivity.class);
                intent.putExtra("ID", pantryIds.get(position));
                context.startActivity(intent);
            } else if (tabSelected.equals(getContext().getResources().getString(R.string.store))) {
                Intent intent = new Intent(context, StoreListActivity.class);
                intent.putExtra("ID", storeIds.get(position));
                context.startActivity(intent);
            }
        });

        return view;
    }
}
