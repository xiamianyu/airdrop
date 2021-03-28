package com.fence.airdrop.manager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import com.fence.airdrop.bean.Apk;
import com.fence.airdrop.bean.ChannelPlatform;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ApkManager {

    /**
     * 获取 APK 列表
     *
     * @param context
     * @param path
     * @return
     */
    public static List<Apk> getApks(Context context, String path) {
        List<Apk> apks = new ArrayList<>();
        if (context == null || TextUtils.isEmpty(path)) {
            return apks;
        }

        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null) {
                return null;
            }

            for (File file : files) {
                Apk apk = getApk(context, file);

                if (apk != null) {
                    apks.add(apk);
                }
            }
        }

        return apks;
    }

    /**
     * 获取 APK
     *
     * @param context
     * @param file
     * @return
     */
    public static Apk getApk(Context context, File file) {
        if (context == null || file == null) {
            return null;
        }

        long size = file.length();
        String apkName = file.getName();
        String path = file.getAbsolutePath();

        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_META_DATA);

        Apk appInfo = null;

        if (packageInfo != null) {
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            applicationInfo.sourceDir = path;
            applicationInfo.publicSourceDir = path;
            String version = packageInfo.versionName;
            String packageName = applicationInfo.packageName;
            String channel = getChannel(applicationInfo.metaData);
            channel = channel == null ? "none" : channel;
            Drawable icon = packageManager.getApplicationIcon(applicationInfo);
            String appName = packageManager.getApplicationLabel(applicationInfo).toString();
            boolean install = apkInstalled(context, packageName, channel);

            appInfo = new Apk();
            appInfo.setIcon(icon);
            appInfo.setAppName(appName);
            appInfo.setApkName(apkName);
            appInfo.setVersion(version);
            appInfo.setChannel(channel);
            appInfo.setInstall(install);
            appInfo.setPackage(packageName);
            appInfo.setSize(getFileSize(size));
            appInfo.setPath(path);
        }

        return appInfo;
    }

    private static String getFileSize(long size) {
        DecimalFormat df = new DecimalFormat("#0.00");

        size = size / 1000; // KB
        if (size < 1024) {
            return df.format(size) + "K";
        } else if (size < 1024 * 1024.f) {
            return df.format((size / 1024.f)) + "M";
        }

        return df.format(size / 1024.f / 1024.f) + "G";
    }

    /**
     * 删除 APK 列表
     *
     * @param apks
     */
    public static void delete(List<Apk> apks) {
        if (apks == null || apks.isEmpty()) {
            return;
        }

        for (Apk apk : apks) {
            delete(apk.getPath());
        }
    }

    /**
     * 删除 APK
     *
     * @param path
     */
    public static void delete(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }

        new File(path).delete();
    }

    /**
     * Apk 已安装校验
     *
     * @param packageName
     * @param channel
     * @return
     */
    public static boolean apkInstalled(Context context, String packageName, String channel) {
        PackageManager packageManager = context.getPackageManager();

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            if (packageInfo == null) {
                return false;
            }

            String installedChannel = getChannel(packageInfo.applicationInfo.metaData);
            if (packageInfo.packageName.equalsIgnoreCase(packageName) && channel.equals(installedChannel)) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取渠道
     *
     * @param metaData
     * @return
     */
    public static String getChannel(Bundle metaData) {
        if (metaData == null) {
            return null;
        }

        List<String> channelPlatforms = getChannelPlatforms();
        for (String channelPlatform : channelPlatforms) {
            String channel = metaData.getString(channelPlatform);
            if (!TextUtils.isEmpty(channel)) {
                return channel;
            }
        }

        return null;
    }

    /**
     * 获取渠道统计平台
     *
     * @return
     */
    public static List<String> getChannelPlatforms() {
        List<String> platforms = new ArrayList<>();
        platforms.add(ChannelPlatform.BAIDU);
        platforms.add(ChannelPlatform.JPUSH);
        platforms.add(ChannelPlatform.TD);
        platforms.add(ChannelPlatform.UMENG);
        return platforms;
    }

    /**
     * 安装 App
     *
     * @param path
     */
    public static void installApp(Context context, String path) {
        if (context == null || TextUtils.isEmpty(path)) {
            return;
        }

        File file = new File(path);

        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // 兼容7.0
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }

        if (context.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
            context.startActivity(intent);
        }
    }

    /**
     * 卸载 App
     *
     * @param packageName
     */
    public static void uninstallApp(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return;
        }

        Uri uri = Uri.fromParts("package", packageName, null);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        context.startActivity(intent);
    }

}
