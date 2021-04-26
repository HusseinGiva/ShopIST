package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"name"}, unique = true)})
public class PantryList {
    @PrimaryKey(autoGenerate = true)
    public long pantryId;
    public String name;
    public long number_of_items;
    public double latitude;
    public double longitude;
    public String driveTime = null;
    public boolean created;

    public PantryList(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.number_of_items = 0;
        this.created = true;
    }
}
