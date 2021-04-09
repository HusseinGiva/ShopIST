package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.Index;

@Entity(primaryKeys = {"storeId", "itemId"}, indices = {@Index(value = {"itemId"})})
public class StoreItem {
    public long storeId;
    public long itemId;
    public long quantity;
}
