package pt.ulisboa.tecnico.cmov.shopist;

import android.view.View;
import android.widget.TextView;

public class ListViewHolder {

    TextView listName;
    TextView driveTime;
    TextView n_items;
    TextView queue_wait_time_text;
    TextView queue_wait_time;

    ListViewHolder(View v) {
        listName = v.findViewById(R.id.list_name);
        driveTime = v.findViewById(R.id.drive_time);
        n_items = v.findViewById(R.id.n_items);
        queue_wait_time_text = v.findViewById(R.id.queue_wait_time);
        queue_wait_time = v.findViewById(R.id.queue_wait_time_value);
    }
}
