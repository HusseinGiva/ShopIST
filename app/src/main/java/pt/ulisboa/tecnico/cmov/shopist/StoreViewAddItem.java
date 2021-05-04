package pt.ulisboa.tecnico.cmov.shopist;

import android.os.Parcel;
import android.os.Parcelable;

public class StoreViewAddItem implements Parcelable {
    public String storeId;
    public String name;
    public float price;
    public String latitude;
    public String longitude;
    public Boolean isChecked;

    public StoreViewAddItem(String storeId, String name, Float price) {
        this.storeId = storeId;
        this.name = name;
        this.price = price;
        this.isChecked = false;
    }

    public StoreViewAddItem() {
        this.isChecked = false;
    }

    public StoreViewAddItem(String storeId, String name, Float price, Boolean isChecked) {
        this.storeId = storeId;
        this.name = name;
        this.price = price;
        this.isChecked = isChecked;
    }

    public StoreViewAddItem(String storeId, String name, Float price, Boolean isChecked, String latitude, String longitude) {
        this.storeId = storeId;
        this.name = name;
        this.price = price;
        this.isChecked = isChecked;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private StoreViewAddItem(Parcel in) {
        this.storeId = in.readString();
        this.name = in.readString();
        this.price = in.readFloat();
        this.isChecked = in.readBoolean();
        this.latitude = in.readString();
        this.longitude = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(storeId);
        dest.writeString(name);
        dest.writeFloat(price);
        dest.writeBoolean(isChecked);
        dest.writeString(latitude);
        dest.writeString(longitude);
    }

    public static final Parcelable.Creator<StoreViewAddItem> CREATOR = new Parcelable.Creator<StoreViewAddItem>() {

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