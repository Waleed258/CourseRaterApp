package com.example.icourseraterproject;

public class Review {
    private String name;
    private String email;
    private String description;
    private double rating;
    private String time;
    private String imageURL;
    public Review() {}
    public Review(String description,String time,String name,double rating,String imageURL,String email) {
        this.description=description;
        this.name=name;
        this.rating=rating;
        this.imageURL=imageURL;
        this.time=time;
        this.email=email;
    }
    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
    public Double getRating() {
        return rating;
    }
    public String getDescription() {
        return description;
    }
    public String getImageURL() {
        return imageURL;
    }
    public String getTime() {return time;}
}

