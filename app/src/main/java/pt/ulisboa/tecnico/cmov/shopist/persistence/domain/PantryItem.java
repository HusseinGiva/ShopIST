package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;

@Entity(primaryKeys = {"pantryId", "itemId"})
public class PantryItem {
    public long pantryId;
    public long itemId;
    public long quantity;
    public long idealQuantity;
}
