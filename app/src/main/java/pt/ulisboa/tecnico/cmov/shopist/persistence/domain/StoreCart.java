package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Embedded;
import androidx.room.Relation;

public class StoreCart {
    @Embedded
    public StoreList store;
    @Relation(
            parentColumn = "storeId",
            entityColumn = "associatedStoreId"
    )
    public Cart cart;
}
