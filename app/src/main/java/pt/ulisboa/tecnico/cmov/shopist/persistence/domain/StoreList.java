package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"name"}, unique = true)})
public class StoreList {
    @PrimaryKey(autoGenerate = true)
    public long storeId;
    public String storeIdServer;
    public String name;
    public long number_of_items;
    public double latitude;
    public double longitude;
    public String driveTime = null;
    public long queue_time;

    public StoreList(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.number_of_items = 0;
        this.queue_time = -1;
    }
}
