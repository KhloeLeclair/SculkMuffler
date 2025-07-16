package dev.khloeleclair.skulkmuffler.common.utilities;

public class MathHelpers {

    public static float linearToDb(double linear) {
        if (linear <= 0)
            return Float.NEGATIVE_INFINITY;
        return 20.0f * (float) Math.log10(linear);
    }

    public static double dBtoLinear(float db) {
        if (db == Float.NEGATIVE_INFINITY)
            return 0;
        return Math.pow(10.0, db / 20);
    }

    public static double linearToLog(double linear) {
        return Math.sqrt(linear);
    }

    public static double logToLinear(double log) {
        return Math.pow(log, 2.0);
    }

}
