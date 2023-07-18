package com.example.icourseraterproject;

public class Course {
    private String name;
    private String code;
    private String description;
    public Course() {}
    public Course(String name,String code,String description) {
        this.name=name;
        this.code=code;
        this.description=description;
    }
    public String getName() {
        return name;
    }
    public String getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }
}
