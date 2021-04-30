package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

public class Item {
    public String name;
    public String barcode;
    public double average_rating;
    public long one_star_votes;
    public long two_star_votes;
    public long three_star_votes;
    public long four_star_votes;
    public long five_star_votes;
    public long user_vote;

    public Item (String name, String barcode) {
        this.name = name;
        this.barcode = barcode;
    }

    public Item() {

    }
}
