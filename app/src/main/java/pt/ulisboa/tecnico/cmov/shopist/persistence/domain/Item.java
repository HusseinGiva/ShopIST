package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import java.util.ArrayList;
import java.util.List;

public class Item {
    public String name;
    public String barcode;
    public List<String> users;
    public double average_rating;
    public long one_star_votes;
    public long two_star_votes;
    public long three_star_votes;
    public long four_star_votes;
    public long five_star_votes;
    public long user_vote;

    public Item (String name, String barcode, String userId) {
        this.name = name;
        this.barcode = barcode;
        users = new ArrayList<>();
        users.add(userId);
    }

    public Item() {

    }
}
