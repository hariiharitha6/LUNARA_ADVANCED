package com.example.lunara;

public class CheckupModel {

    public long nextVisitDate;    // epoch ms
    public String riskLevel;      // "HIGH" / "MODERATE" / "LOW"
    public String status;         // "pending" / "completed" / "overdue"
    public long timestamp;        // when it was scheduled

    // Required empty constructor for Firebase
    public CheckupModel() {}

    public CheckupModel(long nextVisitDate, String riskLevel,
                        String status, long timestamp) {
        this.nextVisitDate = nextVisitDate;
        this.riskLevel     = riskLevel;
        this.status        = status;
        this.timestamp     = timestamp;
    }
}
