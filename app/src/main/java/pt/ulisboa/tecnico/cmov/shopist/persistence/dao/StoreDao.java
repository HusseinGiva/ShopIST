package pt.ulisboa.tecnico.cmov.shopist.persistence.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.Cart;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.CartItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.CartWithItems;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreCart;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreItemPrice;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreWithItems;

@Dao
public interface StoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertStoreList(StoreList s);

    @Update
    void updateStoreList(StoreList p);

    @Query("SELECT * FROM StoreList")
    Single<List<StoreList>> loadAllStoreLists();

    @Query("SELECT * FROM StoreList WHERE name = :name")
    Single<List<StoreList>> loadStoreListByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertCart(Cart c);

    @Update
    void updateCart(Cart c);

    @Transaction
    @Query("SELECT * FROM StoreList")
    Single<List<StoreCart>> getStoresAndCarts();

    @Transaction
    @Query("SELECT * FROM StoreList WHERE name = :name")
    Single<List<StoreCart>> getStoresAndCartsByName(String name);

    @Transaction
    @Query("SELECT * FROM StoreList")
    Single<List<StoreWithItems>> getStoreWithItems();

    @Transaction
    @Query("SELECT * FROM StoreList WHERE name = :name")
    Single<List<StoreWithItems>> getStoreWithItemsByName(String name);

    @Transaction
    @Query("SELECT * FROM Cart")
    Single<List<CartWithItems>> getCartWithItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertStoreItem(StoreItem s);

    @Update
    void updateStoreItem(StoreItem s);

    @Query("SELECT * FROM StoreItem")
    Single<List<StoreItem>> loadAllStoreItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertStoreItemPrice(StoreItemPrice s);

    @Update
    void updateStoreItemPrice(StoreItemPrice s);

    @Query("SELECT * FROM StoreItemPrice")
    Single<List<StoreItemPrice>> loadAllStoreItemPrices();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertCartItem(CartItem c);

    @Update
    void updateCartItem(CartItem c);

    @Query("SELECT * FROM CartItem")
    Single<List<CartItem>> loadAllCartItems();
}
