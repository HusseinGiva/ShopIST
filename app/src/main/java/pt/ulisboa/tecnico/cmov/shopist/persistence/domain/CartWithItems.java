package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class CartWithItems {
    @Embedded
    public Cart cart;
    @Relation(
            parentColumn = "cartId",
            entityColumn = "itemId",
            associateBy = @Junction(CartItem.class)
    )
    public List<Item> items;
}
