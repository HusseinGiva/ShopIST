package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"name"}, unique = true)})
public class StoreList {
    @PrimaryKey(autoGenerate = true)
    public long storeId;
    public String name;
    public long number_of_items;
    public String location;
    public long queue_time;
}
