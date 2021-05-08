package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreList {
    public String name;
    public long number_of_items;
    public String latitude;
    public String longitude;
    public String driveTime = null;
    public long queue_time;
    public List<String> users;

    public int nQueueItems;
    public Map<String, Timestamp> usersArriveTime;
    public Map<String, Integer> usersQueueItemsAtArrival;

    public List<Integer> nCartItemsAtArrival;
    public List<Long> timeInQueue;


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

        nQueueItems = 0;
        usersArriveTime = new HashMap<>();
        usersQueueItemsAtArrival = new HashMap<>();

        nCartItemsAtArrival = new ArrayList<>();
        timeInQueue = new ArrayList<>();
    }

    public StoreList(String name, String userId) {
        this.name = name;
        this.latitude = null;
        this.longitude = null;
        this.number_of_items = 0;
        this.queue_time = -1;
        users = new ArrayList<>();
        users.add(userId);

        nQueueItems = 0;
        usersArriveTime = new HashMap<>();
        usersQueueItemsAtArrival = new HashMap<>();

        nCartItemsAtArrival = new ArrayList<>();
        timeInQueue = new ArrayList<>();
    }
}
