package com.fence.airdrop.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AppUtil {

    public static int getVersionCode(Context context) {
        if (context == null) {
            return 0;
        }

        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static String getVersionName(Context context) {
        if (context == null) {
            return null;
        }

        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int getIntVersionName(Context context) {
        String versionName = getVersionName(context);
        versionName = versionName.replace(".", "");
        return Integer.parseInt(versionName);
    }

    public static String getMeta(Context context, String meta) {
        ApplicationInfo appInfo = null;

        try {
            appInfo = context.getPackageManager().getApplicationInfo(
                context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (appInfo.metaData == null) {
            return "";
        } else {
            return appInfo.metaData.getString(meta);
        }
    }
}