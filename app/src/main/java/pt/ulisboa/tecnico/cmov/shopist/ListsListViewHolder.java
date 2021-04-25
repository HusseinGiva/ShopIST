package pt.ulisboa.tecnico.cmov.shopist;

import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

public class ListsListViewHolder {

    TextView listName;
    TextView driveTime;
    TextView n_items;

    ListsListViewHolder(View v) {
        listName = v.findViewById(R.id.list_name);
        driveTime = v.findViewById(R.id.drive_time);
        n_items = v.findViewById(R.id.n_items);
    }
}
