package pt.ulisboa.tecnico.cmov.shopist;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.shopist.persistence.GlobalClass;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryWithItems;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreWithItems;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ListFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private List<String> names = new ArrayList<>();
    private List<Integer> n_items = new ArrayList<>();
    private ListView list;

    public ListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ListFragment newInstance(String param1, String param2) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                GlobalClass globalVariable = (GlobalClass) getActivity().getApplicationContext();

                Handler timerHandler = new Handler();
                Runnable timerRunnable = new Runnable() {

                    @Override
                    public void run() {
                        if (globalVariable.getLoaded() == 0) {
                            names.clear();
                            n_items.clear();
                            if (tab.getText().equals("PANTRY")) {
                                globalVariable.setTypeSelected("PANTRY");
                                List<PantryWithItems> p = globalVariable.getPantryWithItems();
                                for (PantryWithItems pi : p) {
                                    names.add(pi.pantry.name);
                                    n_items.add((int) pi.pantry.number_of_items);
                                }
                            } else if (tab.getText().equals("SHOPPING")) {
                                globalVariable.setTypeSelected("SHOPPING");
                                List<StoreWithItems> p = globalVariable.getStoreWithItems();
                                for (StoreWithItems pi : p) {
                                    names.add(pi.store.name);
                                    n_items.add((int) pi.store.number_of_items);
                                }
                            }
                            list.invalidateViews();
                            timerHandler.removeCallbacks(this);
                        } else {
                            timerHandler.postDelayed(this, 500);
                        }
                    }
                };
                timerHandler.postDelayed(timerRunnable, 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        GlobalClass globalVariable = (GlobalClass) getActivity().getApplicationContext();

        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (globalVariable.getLoaded() == 0) {
                    List<PantryWithItems> p = globalVariable.getPantryWithItems();
                    names = new ArrayList<>();
                    n_items = new ArrayList<>();
                    for (PantryWithItems pi : p) {
                        names.add(pi.pantry.name);
                        n_items.add((int) pi.pantry.number_of_items);
                    }
                    list = view.findViewById(R.id.list);
                    ListsListAdapter a = new ListsListAdapter(getContext(), names, n_items);
                    list.setAdapter(a);
                    timerHandler.removeCallbacks(this);
                } else {
                    timerHandler.postDelayed(this, 500);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);

        return view;
    }
}