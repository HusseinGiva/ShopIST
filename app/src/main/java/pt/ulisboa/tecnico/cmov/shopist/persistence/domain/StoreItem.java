package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

public class StoreItem {
    public String storeId;
    public String itemId;
    public int quantity;
    public float price;

    public StoreItem(String storeId, String itemId, int quantity, float price) {
        this.storeId = storeId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.price = price;
    }

    public StoreItem() {
    }
}
