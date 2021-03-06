package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CheckoutListAdapter extends ArrayAdapter<String> {

    final Context context;
    final List<String> pantryIds;
    final List<String> pantryNames;
    final List<Integer> quantitiesNeeded;
    final List<String> pantryQuantities;
    final Map<String, Map<String, String>> quantitiesPerPantry;
    final List<String> itemIds;
    final List<Integer> item_quantities;
    int item_position = 0;

    public CheckoutListAdapter(Context context, List<String> pantryIds, List<String> pantryNames, List<Integer> quantitiesNeeded, List<String> pantryQuantities,
                               Map<String, Map<String, String>> quantitiesPerPantry, List<String> itemIds, List<Integer> item_quantities) {
        super(context, R.layout.checkout_list_item, R.id.checkout_pantry_name, pantryNames);
        this.context = context;
        this.pantryIds = pantryIds;
        this.pantryNames = pantryNames;
        this.quantitiesNeeded = quantitiesNeeded;
        this.pantryQuantities = pantryQuantities;
        this.quantitiesPerPantry = quantitiesPerPantry;
        this.itemIds = itemIds;
        this.item_quantities = item_quantities;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        CheckoutListViewHolder holder;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.checkout_list_item, parent, false);
            holder = new CheckoutListViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (CheckoutListViewHolder) view.getTag();
        }

        holder.pantryName.setText(pantryNames.get(position));
        holder.quantityNeeded.setText(String.valueOf(quantitiesNeeded.get(position)));
        if (pantryQuantities.get(position).equals("")) {
            holder.pantryQuantity.setText("0");
        } else {
            holder.pantryQuantity.setText(pantryQuantities.get(position));
        }

        EditText et = view.findViewById(R.id.checkout_pantry_quantity);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Map<String, String> m = quantitiesPerPantry.get(itemIds.get(item_position));
                String str = s.toString();
                try {
                    int previous_n = Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(m).get(pantryIds.get(position))));
                    int n = Integer.parseInt(str);
                    if (n < 0) {
                        m.put(pantryIds.get(position), "0");
                        et.setText("0");
                    } else if (quantity_sum() - previous_n + n <= item_quantities.get(item_position)) {
                        m.put(pantryIds.get(position), String.valueOf(n));
                    } else {
                        int new_n = item_quantities.get(item_position) - quantity_sum() + previous_n;
                        m.put(pantryIds.get(position), String.valueOf(new_n));
                        et.setText(String.valueOf(new_n));
                    }
                } catch (NumberFormatException e) {
                    if (!str.equals("")) {
                        et.setText("");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        view.findViewById(R.id.decrement_checkout_quantity).setOnClickListener(v -> {
            Map<String, String> m = quantitiesPerPantry.get(itemIds.get(item_position));
            String s = et.getText().toString();
            if (s.equals("") || s.equals("0")) {
                et.setText("0");
                Objects.requireNonNull(m).put(pantryIds.get(position), "0");
            } else {
                int n = Integer.parseInt(s);
                et.setText(String.valueOf(n - 1));
                Objects.requireNonNull(m).put(pantryIds.get(position), String.valueOf(n - 1));
            }
        });

        view.findViewById(R.id.increment_checkout_quantity).setOnClickListener(v -> {
            Map<String, String> m = quantitiesPerPantry.get(itemIds.get(item_position));
            String s = et.getText().toString();
            if (s.equals("")) {
                et.setText("0");
                Objects.requireNonNull(m).put(pantryIds.get(position), "0");
            } else {
                int n = Integer.parseInt(s);
                if (quantity_sum() < item_quantities.get(item_position)) {
                    et.setText(String.valueOf(n + 1));
                    Objects.requireNonNull(m).put(pantryIds.get(position), String.valueOf(n + 1));
                }
            }
        });

        return view;
    }

    public int quantity_sum() {
        int sum = 0;
        for (int i = 0; i < pantryIds.size(); i++) {
            String s = Objects.requireNonNull(quantitiesPerPantry.get(itemIds.get(item_position))).get(pantryIds.get(i));
            sum += Integer.parseInt(Objects.requireNonNull(s));
        }
        return sum;
    }
}
