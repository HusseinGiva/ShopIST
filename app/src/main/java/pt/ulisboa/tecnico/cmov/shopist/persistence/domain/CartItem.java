package pt.ulisboa.tecnico.cmov.shopist.persistence.domain;

public class CartItem {
    public String cartId;
    public String itemId;
    public long quantity;

    public CartItem() {}

    public CartItem(String cartId, String itemId, long quantity) {
        this.cartId = cartId;
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
