package pt.ulisboa.tecnico.cmov.shopist.persistence;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import pt.ulisboa.tecnico.cmov.shopist.persistence.dao.PantryDao;
import pt.ulisboa.tecnico.cmov.shopist.persistence.dao.StoreDao;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Cart;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.CartItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Item;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItemPrice;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

@Database(entities = {PantryList.class, StoreList.class, Cart.class, Item.class, PantryItem.class, StoreItem.class, StoreItemPrice.class, CartItem.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PantryDao pantryDao();
    public abstract StoreDao storeDao();
}
