package com.example.lunara;

public class MotherModel {

    public String name;
    public String area;
    public String mobile;
    public int riskScore;
    public String trimester;

    public MotherModel() {}

    public MotherModel(String name, String area, String mobile,
                       int riskScore, String trimester) {
        this.name      = name;
        this.area      = area;
        this.mobile    = mobile;
        this.riskScore = riskScore;
        this.trimester = trimester;
    }
}
