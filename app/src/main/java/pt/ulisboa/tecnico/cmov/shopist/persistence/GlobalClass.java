package pt.ulisboa.tecnico.cmov.shopist.persistence;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryWithItems;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreWithItems;

public class GlobalClass extends Application {

    private List<PantryWithItems> pantryWithItems = new ArrayList<>();
    private List<StoreWithItems> storeWithItems = new ArrayList<>();

    // PANTRY OR SHOPPING
    private String typeSelected = "";
    private Integer positionSelected = 0;

    private int loaded = 2;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public void setUp(AppDatabase db) {

        if(this.loaded == 0) return;

        typeSelected = "PANTRY";

        mDisposable.add(db.pantryDao().getPantryWithItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pantryWithItems -> {
                    this.pantryWithItems = pantryWithItems;
                    loaded--;
                }));

        mDisposable.add(db.storeDao().getStoreWithItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(storeWithItems -> {
                    this.storeWithItems = storeWithItems;
                    loaded--;
                }));


    }

    public int getLoaded() {
        return this.loaded;
    }

    public void setTypeSelected(String typeSelected) {
        this.typeSelected = typeSelected;
    }

    public String getTypeSelected() {
        return typeSelected;
    }

    public void setPositionSelected(Integer positionSelected) {
        this.positionSelected = positionSelected;
    }

    public Integer getPositionSelected() {
        return positionSelected;
    }

    public void addPantry(PantryList p) {
        PantryWithItems pi = new PantryWithItems();
        pi.pantry = p;
        pi.items = new ArrayList<>();
        this.pantryWithItems.add(pi);
    }

    public List<PantryWithItems> getPantryWithItems() {
        return pantryWithItems;
    }

    public void addItemToPantryList(String name, Item i) {
        for(PantryWithItems p : this.pantryWithItems) {
            if(p.pantry.name.equals(name)) {
                p.items.add(i);
                return;
            }
        }
    }

    public List<StoreWithItems> getStoreWithItems() {
        return storeWithItems;
    }

    public void addStore(StoreList s) {
        StoreWithItems si = new StoreWithItems();
        si.store = s;
        si.items = new ArrayList<>();
        this.storeWithItems.add(si);
    }

    public void addItemToStoreList(String name, Item i) {
        for(StoreWithItems s : this.storeWithItems) {
            if(s.store.name.equals(name)) {
                s.items.add(i);
                return;
            }
        }
    }

    public void clearData(){
        pantryWithItems.clear();
        storeWithItems.clear();
    }
}
