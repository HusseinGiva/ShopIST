package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

public class Cart {
    public long number_of_items;
    public String associatedStoreId;

    public Cart() {}

    public Cart(long number_of_items, String associatedStoreId) {
        this.number_of_items = number_of_items;
        this.associatedStoreId = associatedStoreId;
    }
}
