package pt.ulisboa.tecnico.cmov.shopist;

import java.util.ArrayList;
import java.util.List;

public class StoreContent {
    static final List<StoreViewAddItem> ITEMS = new ArrayList<>();

    public static void addItem(StoreViewAddItem item) {
        ITEMS.add(0, item);
    }

    public static void emptyList() {
        ITEMS.clear();
    }
}
