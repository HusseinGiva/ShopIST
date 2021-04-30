package pt.ulisboa.tecnico.cmov.shopist;

import android.os.Parcel;
import android.os.Parcelable;

public class StoreItem implements Parcelable {
    public String id;
    public String name;
    public float price;
    public Boolean isChecked;

    public StoreItem(String id, String name, Float price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.isChecked = false;
    }

    public StoreItem() {
        this.isChecked = false;
    }

    public StoreItem(String id, String name, Float price, Boolean isChecked) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.isChecked = isChecked;
    }

    private StoreItem(Parcel in) {
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

    public static final Parcelable.Creator<StoreItem> CREATOR  = new Parcelable.Creator<StoreItem>() {

        @Override
        public StoreItem createFromParcel(Parcel in) {
            return new StoreItem(in);
        }

        @Override
        public StoreItem[] newArray(int size) {
            return new StoreItem[size];
        }
    };
}