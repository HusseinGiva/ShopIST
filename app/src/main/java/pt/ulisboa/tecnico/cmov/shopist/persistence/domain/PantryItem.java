package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.Index;

@Entity(primaryKeys = {"pantryId", "itemId"}, indices = {@Index(value = {"itemId"})})
public class PantryItem {
    public long pantryId;
    public long itemId;
    public long quantity;
    public long idealQuantity;
}
