package com.example.lunara;

public class AlertModel {

    public String name;
    public String mobile;
    public String area;
    public long timestamp;

    // REQUIRED empty constructor
    public AlertModel() {
    }

    public AlertModel(String name, String mobile, String area, long timestamp) {
        this.name = name;
        this.mobile = mobile;
        this.area = area;
        this.timestamp = timestamp;
    }
}