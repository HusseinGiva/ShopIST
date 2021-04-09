package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.Index;

@Entity(primaryKeys = {"cartId", "itemId"}, indices = {@Index(value = {"itemId"})})
public class CartItem {
    public long cartId;
    public long itemId;
    public long quantity;
}
