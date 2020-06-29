package com.famousindiasocialnetwork.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.famousindiasocialnetwork.network.response.UserResponse;

import io.realm.RealmModel;
import io.realm.annotations.RealmClass;

@RealmClass
public class UserRealm implements RealmModel, Parcelable {
    private Integer id;
    private String userId, name, image;

    public UserRealm() {
    }

    public UserRealm(Integer id, String userId, String name, String image) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.image = image;
    }

    protected UserRealm(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readInt();
        }
        userId = in.readString();
        name = in.readString();
        image = in.readString();
    }

    public static final Creator<UserRealm> CREATOR = new Creator<UserRealm>() {
        @Override
        public UserRealm createFromParcel(Parcel in) {
            return new UserRealm(in);
        }

        @Override
        public UserRealm[] newArray(int size) {
            return new UserRealm[size];
        }
    };

    public Integer getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public static UserRealm fromUserResponse(UserResponse userResponse) {
        return userResponse == null ? null : new UserRealm(userResponse.getId(), userResponse.getUserId(), userResponse.getName(), userResponse.getImage());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(id);
        }
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeString(image);
    }
}
