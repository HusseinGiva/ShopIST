package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import java.util.HashMap;
import java.util.Map;

public class Item {
    public String barcode;
    public Map<String, String> users = new HashMap<>();
    public Map<String, Float> stores = new HashMap<>();
    public Map<String, Integer> ratings = new HashMap<>();

    public Item(String name, String barcode, String userId, String id, Float price) {
        this.barcode = barcode;
        this.users.put(userId, name);
        this.stores.put(id, price);
    }

    public Item(String name, String barcode, String userId) {
        this.barcode = barcode;
        this.users.put(userId, name);
    }

    public Item() {

    }
}
