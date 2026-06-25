package com.example.woil.ui;

public class WageCalculator {

    public static class Result {
        public double payableHours;
        public double baseAmount;
        public double extraAmount;
        public double travelAmount;
        public double tipAmount;
        public double materialAmount;
        public double totalAmount;
        public double marketMin;
        public double marketMedian;
        public double marketMax;
        public String marketStatus;
    }

    public static class Input {
        public String category;
        public boolean fastCleaning;
        public boolean laundry;
        public boolean nearby;
        public boolean otherSelected;
        public double distanceKm;
        public double durationHours;
        public double humanHours;
        public int breakMinutes;
        public double tips;
        public double materials;
        public java.util.Map<String, Double> marketRates;
    }

    public static Result calculate(Input input) {
        Result result = new Result();

        double baseRatePerHour = getBaseRate(input.category, input.marketRates);
        double perKmRate = 15.0;

        double breakHours = input.breakMinutes / 60.0;
        result.payableHours = Math.max(0, input.humanHours - breakHours);

        result.baseAmount = baseRatePerHour * result.payableHours;

        result.extraAmount = 0;
        if (input.fastCleaning) {
            result.extraAmount += result.baseAmount * 0.10;
        }
        if (input.laundry) {
            result.extraAmount += result.baseAmount * 0.06;
        }
        if (input.nearby) {
            result.extraAmount += result.baseAmount * 0.05;
        }
        if (input.otherSelected) {
            result.extraAmount += 100.0;
        }

        result.travelAmount = input.distanceKm * perKmRate;
        result.tipAmount = input.tips;
        result.materialAmount = input.materials;

        result.totalAmount =
                result.baseAmount
                        + result.extraAmount
                        + result.travelAmount
                        + result.tipAmount
                        + result.materialAmount;

        // Simple market band for interim version
        result.marketMin = result.baseAmount * 0.90;
        result.marketMedian = result.baseAmount * 1.00 + result.extraAmount + result.travelAmount;
        result.marketMax = result.baseAmount * 1.20 + result.extraAmount + result.travelAmount;

        if (result.totalAmount < result.marketMin) {
            result.marketStatus = "BELOW RANGE";
        } else if (result.totalAmount > result.marketMax) {
            result.marketStatus = "ABOVE RANGE";
        } else {
            result.marketStatus = "WITHIN RANGE";
        }

        return result;
    }

    private static double getBaseRate(String category, java.util.Map<String, Double> marketRates) {
        if (category == null) category = "other";
        category = category.toLowerCase();

        if (marketRates != null && marketRates.containsKey(category) && marketRates.get(category) != null) {
            return marketRates.get(category);
        }

        switch (category) {
            case "cleaning":
                return 600.0;
            case "gardening":
                return 700.0;
            case "plumbing":
                return 900.0;
            case "housekeeping":
                return 650.0;
            case "laundry":
                return 550.0;
            case "caregiving":
                return 800.0;
            default:
                return 600.0;
        }
    }
}