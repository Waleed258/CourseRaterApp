package com.example.icourseraterproject;

public class User {
    private Boolean admin;
    private String imageURL;
    private String name;
    private String email;


    public User() {}
    public User(Boolean admin,String imageURL,String name,String email ){

        this.admin=admin;
        this.name=name;
        this.imageURL=imageURL;
        this.email=email;
    }
    public Boolean getAdmin() {
        return admin;
    }
    public String getName() {
        return name;
    }
    public String getImageURL() {
        return imageURL;
    }
    public String getEmail() {
        return email;
    }
}
