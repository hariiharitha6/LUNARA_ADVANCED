package com.example.lunara;

public class UserModel {

    public String userId;
    public String name;
    public String mobile;
    public String area;
    public String weight;
    public String password;
    public int lmpYear;
    public int lmpMonth;
    public int lmpDay;
    public int riskScore;

    // Required empty constructor for Firebase
    public UserModel() {}

    public UserModel(String userId, String name, String mobile, String area,
                     String weight, String password,
                     int lmpYear, int lmpMonth, int lmpDay) {
        this.userId   = userId;
        this.name     = name;
        this.mobile   = mobile;
        this.area     = area;
        this.weight   = weight;
        this.password = password;
        this.lmpYear  = lmpYear;
        this.lmpMonth = lmpMonth;
        this.lmpDay   = lmpDay;
        this.riskScore = 0;
    }
}
