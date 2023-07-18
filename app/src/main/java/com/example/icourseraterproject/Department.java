package com.example.icourseraterproject;

import java.util.ArrayList;

public class Department {

    private String name;
    ArrayList<String> notificationList;
    public Department() {}
    public Department(String name) {
        this.name=name;
    }
    public String getName() {
        return name;
    }

    public ArrayList<String> getNotificationList() {
        return notificationList;
    }

    public void setNotificationList(ArrayList<String> notificationList) {
        this.notificationList = notificationList;
    }
}
