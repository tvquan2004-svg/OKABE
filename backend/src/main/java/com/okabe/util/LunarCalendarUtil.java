package com.okabe.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class LunarCalendarUtil {

    @Getter
    @AllArgsConstructor
    public static class LunarDate {
        private final int day;
        private final int month;
        private final int year;
        private final boolean isLeap;
    }

    private static final double PI = Math.PI;

    public static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        return dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    public static int[] jdToDate(int jd) {
        int a = jd + 32044;
        int b = (4 * a + 3) / 146097;
        int c = a - (b * 146097) / 4;
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        int day = e - (153 * m + 2) / 5 + 1;
        int month = m + 3 - 12 * (m / 10);
        int year = b * 100 + d - 4800 + m / 10;
        return new int[]{day, month, year};
    }

    public static double sunLongitude(int jdn) {
        double T = (jdn - 2451545.0) / 36525;
        double T2 = T * T;
        double dr = PI / 180;
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
        double DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
        DL += (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M);
        DL += 0.000290 * Math.sin(dr * 3 * M);
        double L = L0 + DL;
        L = L - 360 * Math.floor(L / 360);
        return L;
    }

    public static int newMoonDay(int k) {
        double T = k / 1236.85;
        double T2 = T * T;
        double T3 = T2 * T;
        double dr = PI / 180;
        double Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
        Jd1 += 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
        double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
        double Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
        double F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
        double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
        C1 -= 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(2 * dr * Mpr);
        C1 -= 0.0004 * Math.sin(3 * dr * Mpr);
        C1 += 0.0104 * Math.sin(2 * dr * F) - 0.0051 * Math.sin(dr * (M + Mpr));
        C1 -= 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
        C1 -= 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
        C1 += 0.0010 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
        double deltat;
        if (T < -11) {
            deltat = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3;
        } else {
            deltat = -0.000278 + 0.000265 * T + 0.000262 * T2;
        }
        double JdNew = Jd1 + C1 - deltat;
        return (int) Math.floor(JdNew + 0.5);
    }

    public static int lunarMonth11(int yy) {
        int off = jdFromDate(31, 12, yy) - jdFromDate(1, 1, 1900);
        int k = (int) Math.floor(off / 29.530588853);
        int nm = newMoonDay(k);
        double sunLong = sunLongitude(nm);
        if (sunLong >= 9) {
            nm = newMoonDay(k - 1);
        }
        return nm;
    }

    public static int leapMonthOffset(int a11) {
        int k = (int) Math.floor(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int last, arc, i;
        for (i = 0; ; i++) {
            last = (int) sunLongitude(newMoonDay(k + i));
            arc = (int) sunLongitude(newMoonDay(k + i + 1));
            if (arc != last) break;
        }
        return i - 1;
    }

    public static LunarDate solarToLunar(int dd, int mm, int yy) {
        int jdn = jdFromDate(dd, mm, yy);
        return solarToLunarFromJdn(jdn);
    }

    public static LunarDate solarToLunarFromJdn(int jdn) {
        int lunarYear, lunarMonth, lunarDay;
        boolean isLeap = false;

        lunarYear = 1900;
        int a11 = lunarMonth11(lunarYear);

        while (a11 > jdn) {
            lunarYear--;
            a11 = lunarMonth11(lunarYear);
        }

        while (jdn >= lunarMonth11(lunarYear + 1)) {
            lunarYear++;
            a11 = lunarMonth11(lunarYear);
        }

        int k = (int) Math.floor(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int monthStart = a11;
        int monthNum = 11;
        int leapOff = leapMonthOffset(a11);

        while (true) {
            int nextMonthStart;
            if (leapOff >= 0 && monthNum - 11 == leapOff + 1) {
                int leapMonthStart = newMoonDay(k + monthNum - 11);
                int leapMonthEnd = newMoonDay(k + monthNum - 11 + 1);
                if (jdn >= leapMonthStart && jdn < leapMonthEnd) {
                    isLeap = true;
                    lunarDay = jdn - leapMonthStart + 1;
                    lunarMonth = monthNum > 12 ? monthNum - 12 : monthNum;
                    return new LunarDate(lunarDay, lunarMonth, lunarYear, true);
                }
                monthStart = leapMonthEnd;
                monthNum++;
                continue;
            }

            nextMonthStart = newMoonDay(k + monthNum - 11 + 1);

            if (jdn < nextMonthStart) break;

            monthStart = nextMonthStart;
            monthNum++;
        }

        lunarDay = jdn - monthStart + 1;
        lunarMonth = monthNum > 12 ? monthNum - 12 : monthNum;
        if (lunarMonth <= 0) lunarMonth = 1;

        return new LunarDate(lunarDay, lunarMonth, lunarYear, false);
    }

    public static LunarDate solarToLunar(java.time.LocalDate date) {
        return solarToLunar(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public static int findTetJdn(int gregorianYear) {
        int algoYear = gregorianYear - 1;
        int a11 = lunarMonth11(algoYear);
        int k = (int) Math.floor(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int tetJdn = newMoonDay(k + 2);
        return tetJdn;
    }

    public static java.time.LocalDate findTet(int gregorianYear) {
        int tetJdn = findTetJdn(gregorianYear);
        int[] date = jdToDate(tetJdn);
        return java.time.LocalDate.of(date[2], date[1], date[0]);
    }

    public static int getConventionalYear(java.time.LocalDate gregorianDate) {
        java.time.LocalDate tet = findTet(gregorianDate.getYear());
        if (gregorianDate.isBefore(tet)) {
            return gregorianDate.getYear() - 1;
        }
        return gregorianDate.getYear();
    }
}
