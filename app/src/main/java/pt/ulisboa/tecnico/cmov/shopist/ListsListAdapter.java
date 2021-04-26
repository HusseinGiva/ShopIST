package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.GlobalClass;

public class ListsListAdapter extends ArrayAdapter<String> {

    Context context;
    List<String> names;
    List<String> drive_times;
    List<Integer> n_items;

    public ListsListAdapter(Context context, List<String> names, List<String> drive_times, List<Integer> n_items) {
        super(context, R.layout.list_item, R.id.list_name, names);
        this.context = context;
        this.names = names;
        this.drive_times = drive_times;
        this.n_items = n_items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        ListsListViewHolder holder = null;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.list_item, parent, false);
            holder = new ListsListViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ListsListViewHolder) view.getTag();
        }
        holder.listName.setText(names.get(position));
        holder.driveTime.setText("Drive time: " + drive_times.get(position));
        holder.n_items.setText(String.valueOf(n_items.get(position)));

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalClass globalVariable = (GlobalClass) context.getApplicationContext();
                globalVariable.setPositionSelected(position);
                Intent intent = new Intent(context, ListActivity.class);
                context.startActivity(intent);
            }
        });

        return view;
    }
}
