package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;

@Entity(primaryKeys = {"storeId", "itemId"})
public class StoreItem {
    public long storeId;
    public long itemId;
    public long quantity;
}
