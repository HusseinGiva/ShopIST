package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Cart {
    @PrimaryKey(autoGenerate = true)
    public long cartId;
    public long number_of_items;
    public long associatedStoreId;
}
