package pt.ulisboa.tecnico.cmov.shopist;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StoresRecyclerViewAdapter extends RecyclerView.Adapter<StoresRecyclerViewAdapter.ViewHolder> {

    private final List<StoreViewAddItem> mValues;
    private final StoresFragment.OnListFragmentInteractionListener mListener;

    public StoresRecyclerViewAdapter(List<StoreViewAddItem> items, StoresFragment.OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_stores, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mNameView.setText(mValues.get(position).name);
        if (mValues.get(position).price == 0) {
            holder.mPriceView.setText("");
        }
        else {
            holder.mPriceView.setText(String.valueOf(mValues.get(position).price));
        }
        holder.mPriceView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mListener.onListFragmentPriceInteraction(holder.mItem, s);
            }

        });
        holder.mChecked.setChecked(mValues.get(position).isChecked);
        holder.mChecked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (null != mListener) {
                mListener.onListFragmentInteraction(holder.mItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public final TextView mPriceView;
        public final CheckBox mChecked;
        public StoreViewAddItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = view.findViewById(R.id.storeName);
            mPriceView = view.findViewById(R.id.price);
            mChecked = view.findViewById(R.id.checkBox);
        }
    }
}