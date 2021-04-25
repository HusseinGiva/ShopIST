package pt.ulisboa.tecnico.cmov.shopist;

import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PictureContent {
    static final List<Uri> PATHS = new ArrayList<>();

    public static void loadImage(File file) {
        Uri uri = Uri.fromFile(file);
        addItem(uri);
    }

    private static void addItem(Uri uri) {
        PATHS.add(0, uri);
    }
}