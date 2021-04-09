package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class PantryWithItems {
    @Embedded
    public PantryList pantry;
    @Relation(
            parentColumn = "pantryId",
            entityColumn = "itemId",
            associateBy = @Junction(PantryItem.class)
    )
    public List<Item> items;
}
