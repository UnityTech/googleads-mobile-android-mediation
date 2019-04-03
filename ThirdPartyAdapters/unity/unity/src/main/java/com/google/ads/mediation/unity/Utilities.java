package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

class Utilities {
    private static Pattern pattern1 = Pattern.compile("Banner placement ([a-zA-Z]+) returned no fill");
    private static Pattern pattern2 = Pattern.compile("Placement ([a-zA-Z]+) is not a banner placement");

    public static final String KEY_GAME_ID = "gameId";
    public static final String KEY_PLACEMENT_ID = "zoneId";

    public static boolean isValidIds(String gameId, String placementId) {
        if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
            String ids = TextUtils.isEmpty(gameId) ? TextUtils.isEmpty(placementId)
                    ? "Game ID and Placement ID" : "Game ID" : "Placement ID";
            Log.w(TAG, ids + " cannot be empty.");

            return false;
        }

        return true;
    }

    public static boolean isValidContext(Context context) {
        if (context == null) {
            Log.w(TAG, "Context cannot be null.");
            return false;
        }

        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity. Unity Ads requires an Activity context to load "
                    + "ads.");
            return false;
        }
        return true;
    }

    public static String extractPlacementNameFromError(String errorMessage) {
        Matcher match = pattern1.matcher(errorMessage);

        if (match.matches()) {
            return match.group(1);
        }

        match = pattern2.matcher(errorMessage);

        if (match.matches()) {
            return match.group(1);
        }

        return null;
    }
}
