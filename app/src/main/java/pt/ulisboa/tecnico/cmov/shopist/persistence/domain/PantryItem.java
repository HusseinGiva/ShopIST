package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

public class PantryItem {
    public String pantryId;
    public String itemId;
    public int quantity;
    public int idealQuantity;

    public PantryItem(String pantryId, String itemId, int quantity, int idealQuantity) {
        this.pantryId = pantryId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.idealQuantity = idealQuantity;
    }

    public PantryItem() {
    }
}
