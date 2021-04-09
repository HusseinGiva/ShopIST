package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;

@Entity(primaryKeys = {"storeId", "itemId", "price"})
public class StoreItemPrice {
    public long storeId;
    public long itemId;
    public double price;
}
