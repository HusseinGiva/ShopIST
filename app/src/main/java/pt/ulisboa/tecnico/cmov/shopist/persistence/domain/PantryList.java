package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import java.util.ArrayList;
import java.util.List;

public class PantryList {
    public String pantryId;
    public String name;
    public long number_of_items;
    public double latitude;
    public double longitude;
    public String driveTime = null;
    public boolean created;
    public List<String> users;

    public PantryList() {

    }

    public PantryList(String name, double latitude, double longitude, String userId) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.number_of_items = 0;
        this.created = true;
        users = new ArrayList<>();
        users.add(userId);
    }
}
