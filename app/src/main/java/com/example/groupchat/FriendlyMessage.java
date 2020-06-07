package com.example.groupchat;

public class FriendlyMessage  {

    private String message ;
     String name ;
    private String photoUrl ;

    public FriendlyMessage() {
    }

    public FriendlyMessage(String message, String name, String photoUrl) {
        this.message = message;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoId) {
        this.photoUrl = photoId;
    }
}
