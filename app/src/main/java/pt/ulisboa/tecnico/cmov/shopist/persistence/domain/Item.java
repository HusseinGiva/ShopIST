package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"name"}, unique = true)})
public class Item {
    @PrimaryKey(autoGenerate = true)
    public long itemId;
    public String name;
    public double average_rating;
    public long one_star_votes;
    public long two_star_votes;
    public long three_star_votes;
    public long four_star_votes;
    public long five_star_votes;
    public long user_vote;
}
