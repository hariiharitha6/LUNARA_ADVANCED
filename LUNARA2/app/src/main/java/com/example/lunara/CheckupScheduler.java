package com.example.lunara;

import java.util.Calendar;

/**
 * Pure logic helper — no Android dependencies.
 * Calculates smart checkup schedules based on risk score and pregnancy week.
 *
 * Priority rules (in order):
 *  1. Week > 36 → 7 days (weekly near-term)
 *  2. riskScore > 60 → 7 days (HIGH)
 *  3. Third trimester (week 28–36) + riskScore 31–60 → 14 days
 *  4. riskScore 31–60 → 14 days (MODERATE)
 *  5. riskScore ≤ 30 → 30 days (LOW)
 */
public class CheckupScheduler {

    public static final String LEVEL_HIGH     = "HIGH";
    public static final String LEVEL_MODERATE = "MODERATE";
    public static final String LEVEL_LOW      = "LOW";

    public static final String STATUS_PENDING   = "pending";
    public static final String STATUS_OVERDUE   = "overdue";
    public static final String STATUS_COMPLETED = "completed";

    /**
     * Result of scheduling calculation.
     */
    public static class ScheduleResult {
        public long   nextVisitDateMs;
        public String riskLevel;
        public int    intervalDays;

        public ScheduleResult(long nextVisitDateMs, String riskLevel, int intervalDays) {
            this.nextVisitDateMs = nextVisitDateMs;
            this.riskLevel       = riskLevel;
            this.intervalDays    = intervalDays;
        }
    }

    /**
     * Calculate the next visit date.
     *
     * @param riskScore  0–100 from health tracking (0 if none yet)
     * @param weeks      Pregnancy weeks from LMP
     * @return ScheduleResult with nextVisitDateMs, riskLevel, intervalDays
     */
    public static ScheduleResult schedule(int riskScore, long weeks) {
        int intervalDays;
        String riskLevel;

        // Rule 1: Near-term (>36 weeks) — weekly regardless of score
        if (weeks > 36) {
            intervalDays = 7;
            riskLevel    = riskScore > 60 ? LEVEL_HIGH : LEVEL_MODERATE;
        }
        // Rule 2: High risk score → 7 days
        else if (riskScore > 60) {
            intervalDays = 7;
            riskLevel    = LEVEL_HIGH;
        }
        // Rule 3 & 4: Third trimester + moderate OR just moderate → 14 days
        else if (riskScore >= 31 || weeks >= 28) {
            intervalDays = 14;
            riskLevel    = LEVEL_MODERATE;
        }
        // Rule 5: Low risk → 30 days
        else {
            intervalDays = 30;
            riskLevel    = LEVEL_LOW;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, intervalDays);
        // Zero out time portion for clean date comparison
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return new ScheduleResult(cal.getTimeInMillis(), riskLevel, intervalDays);
    }

    /**
     * Compute days remaining until next visit.
     * Returns negative value if overdue.
     */
    public static long daysUntilVisit(long nextVisitDateMs) {
        long nowMs    = System.currentTimeMillis();
        long diffMs   = nextVisitDateMs - nowMs;
        return diffMs / (1000L * 60 * 60 * 24);
    }

    /**
     * Determine current status given a saved nextVisitDate.
     */
    public static String computeStatus(long nextVisitDateMs) {
        long days = daysUntilVisit(nextVisitDateMs);
        return days < 0 ? STATUS_OVERDUE : STATUS_PENDING;
    }

    /**
     * Format nextVisitDate as dd/MM/yyyy string.
     */
    public static String formatDate(long dateMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMs);
        return String.format("%02d/%02d/%04d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR));
    }
}
