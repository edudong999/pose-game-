package com.posegame.app.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Toast utility for quick feedback messages
 */
public class ToastUtil {

    private static Toast toast;

    public static void show(Context context, String message) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void showLong(Context context, String message) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    public static void show(Context context, int stringResId) {
        show(context, context.getString(stringResId));
    }
}