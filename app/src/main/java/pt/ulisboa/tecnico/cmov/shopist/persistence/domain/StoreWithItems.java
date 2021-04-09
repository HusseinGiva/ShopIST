package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class StoreWithItems {
    @Embedded
    public StoreList store;
    @Relation(
            parentColumn = "storeId",
            entityColumn = "itemId",
            associateBy = @Junction(StoreItem.class)
    )
    public List<Item> items;
}
