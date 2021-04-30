package pt.ulisboa.tecnico.cmov.shopist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StoreContent {
    static final List<StoreItem> ITEMS = new ArrayList<>();

    public static void addItem(StoreItem item) {
        ITEMS.add(0, item);
    }

    public static void emptyList() {
        ITEMS.clear();
    }
}
