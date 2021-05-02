package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import java.util.ArrayList;
import java.util.List;

public class StoreList {
    public String name;
    public long number_of_items;
    public String latitude;
    public String longitude;
    public String driveTime = null;
    public long queue_time;
    public List<String> users;

    public StoreList() {

    }

    public StoreList(String name, String latitude, String longitude, String userId) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.number_of_items = 0;
        this.queue_time = -1;
        users = new ArrayList<>();
        users.add(userId);
    }

    public StoreList(String name,  String userId) {
        this.name = name;
        this.latitude = null;
        this.longitude = null;
        this.number_of_items = 0;
        this.queue_time = -1;
        users = new ArrayList<>();
        users.add(userId);
    }
}
