package com.example.lunara;

/**
 * Lightweight model used for the admin checkup RecyclerView.
 * Combines data from users/ and checkups/ Firebase nodes.
 */
public class CheckupEntry {

    public String name;
    public String area;
    public int    riskScore;
    public String riskLevel;       // "HIGH" / "MODERATE" / "LOW"
    public long   nextVisitDate;   // epoch ms
    public String status;          // "pending" / "completed" / "overdue"
    public long   daysRemaining;   // negative = overdue

    public CheckupEntry() {}

    public CheckupEntry(String name, String area, int riskScore,
                        String riskLevel, long nextVisitDate,
                        String status, long daysRemaining) {
        this.name          = name;
        this.area          = area;
        this.riskScore     = riskScore;
        this.riskLevel     = riskLevel;
        this.nextVisitDate = nextVisitDate;
        this.status        = status;
        this.daysRemaining = daysRemaining;
    }
}
