package com.example.woil.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NicParser {

    public static class NicParseResult {
        public boolean valid;
        public String normalizedNic;
        public String dobIso;      // yyyy-MM-dd
        public String gender;      // M / F
        public String error;
    }

    public static NicParseResult parse(String rawNic) {
        NicParseResult result = new NicParseResult();

        if (rawNic == null) {
            result.valid = false;
            result.error = "NIC is empty";
            return result;
        }

        String nic = rawNic.replaceAll("[^0-9VvXx]", "").toUpperCase(Locale.US);

        try {
            if (nic.matches("^\\d{9}[VX]$")) {
                int year = 1900 + Integer.parseInt(nic.substring(0, 2));
                int dayText = Integer.parseInt(nic.substring(2, 5));
                fillResult(result, nic, year, dayText);
                return result;
            }

            if (nic.matches("^\\d{12}$")) {
                int year = Integer.parseInt(nic.substring(0, 4));
                int dayText = Integer.parseInt(nic.substring(4, 7));
                fillResult(result, nic, year, dayText);
                return result;
            }

            result.valid = false;
            result.error = "NIC format invalid";
            return result;

        } catch (Exception e) {
            result.valid = false;
            result.error = "NIC parse failed: " + e.getMessage();
            return result;
        }
    }

    private static void fillResult(NicParseResult result, String nic, int year, int dayText) {
        boolean female = dayText > 500;
        int dayOfYear = female ? dayText - 500 : dayText;

        if (dayOfYear < 1 || dayOfYear > 366) {
            result.valid = false;
            result.error = "Invalid day-of-year in NIC";
            return;
        }

        LocalDate dob = LocalDate.ofYearDay(year, dayOfYear);

        result.valid = true;
        result.normalizedNic = nic;
        result.dobIso = dob.format(DateTimeFormatter.ISO_LOCAL_DATE);
        result.gender = female ? "F" : "M";
    }
}