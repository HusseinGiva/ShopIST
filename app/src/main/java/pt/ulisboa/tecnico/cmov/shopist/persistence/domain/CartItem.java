package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;

@Entity(primaryKeys = {"cartId", "itemId"})
public class CartItem {
    public long cartId;
    public long itemId;
    public long quantity;
}
