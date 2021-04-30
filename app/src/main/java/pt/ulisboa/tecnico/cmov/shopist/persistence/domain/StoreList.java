package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import java.util.ArrayList;
import java.util.List;

public class StoreList {
    public String name;
    public long number_of_items;
    public double latitude;
    public double longitude;
    public String driveTime = null;
    public long queue_time;
    public List<String> users;

    public StoreList() {

    }

    public StoreList(String name, double latitude, double longitude, String userId) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.number_of_items = 0;
        this.queue_time = -1;
        users = new ArrayList<>();
        users.add(userId);
    }
}
