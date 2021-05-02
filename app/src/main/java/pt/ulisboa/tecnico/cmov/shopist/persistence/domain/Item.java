package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Item {
    public String barcode;
    public Map<String, String> users = new HashMap<>();
    public double average_rating;
    public long one_star_votes;
    public long two_star_votes;
    public long three_star_votes;
    public long four_star_votes;
    public long five_star_votes;
    public long user_vote;

    public Item (String name, String barcode, String userId) {
        this.barcode = barcode;
        users.put(userId, name);
    }

    public Item() {

    }
}
