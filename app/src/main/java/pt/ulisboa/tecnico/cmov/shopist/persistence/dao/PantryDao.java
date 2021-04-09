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
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryItem;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryWithItems;

@Dao
public interface PantryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertPantryList(PantryList p);

    @Update
    void updatePantryList(PantryList p);

    @Query("SELECT * FROM PantryList")
    Single<List<PantryList>> loadAllPantryLists();

    @Query("SELECT * FROM PantryList WHERE name = :name")
    Single<List<PantryList>> loadPantryListByName(String name);

    @Transaction
    @Query("SELECT * FROM PantryList")
    Single<List<PantryWithItems>> getPantryWithItems();

    @Transaction
    @Query("SELECT * FROM PantryList WHERE name = :name")
    Single<List<PantryWithItems>> getPantryWithItemsByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertPantryItem(PantryItem p);

    @Update
    void updatePantryItem(PantryItem p);

    @Query("SELECT * FROM PantryItem")
    Single<List<PantryItem>> loadAllPantryItems();
}
