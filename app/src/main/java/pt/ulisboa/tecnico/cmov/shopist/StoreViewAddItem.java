package pt.ulisboa.tecnico.cmov.shopist;

import android.os.Parcel;
import android.os.Parcelable;

public class StoreViewAddItem implements Parcelable {
    public String id;
    public String name;
    public float price;
    public Boolean isChecked;

    public StoreViewAddItem(String id, String name, Float price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.isChecked = false;
    }

    public StoreViewAddItem() {
        this.isChecked = false;
    }

    public StoreViewAddItem(String id, String name, Float price, Boolean isChecked) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.isChecked = isChecked;
    }

    private StoreViewAddItem(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.price = in.readFloat();
        this.isChecked = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeFloat(price);
        dest.writeBoolean(isChecked);
    }

    public static final Parcelable.Creator<StoreViewAddItem> CREATOR  = new Parcelable.Creator<StoreViewAddItem>() {

        @Override
        public StoreViewAddItem createFromParcel(Parcel in) {
            return new StoreViewAddItem(in);
        }

        @Override
        public StoreViewAddItem[] newArray(int size) {
            return new StoreViewAddItem[size];
        }
    };
}