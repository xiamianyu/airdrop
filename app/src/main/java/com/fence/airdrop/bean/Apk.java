package com.fence.airdrop.bean;

import android.graphics.drawable.Drawable;

public class Apk {

    private String mAppName;

    private String mApkName;

    private String mVersion;

    private String mChannel;

    private String mPackage;

    private String mPath;

    private Drawable mIcon;

    private String mSize;

    private boolean mInstall;

    private boolean mCheck;

    public String getAppName() {
        return mAppName;
    }

    public void setAppName(String appName) {
        mAppName = appName;
    }

    public String getApkName() {
        return mApkName;
    }

    public void setApkName(String apkName) {
        mApkName = apkName;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getChannel() {
        return mChannel;
    }

    public void setChannel(String channel) {
        mChannel = channel;
    }

    public String getPackage() {
        return mPackage;
    }

    public void setPackage(String aPackage) {
        mPackage = aPackage;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public String getSize() {
        return mSize;
    }

    public void setSize(String size) {
        mSize = size;
    }

    public boolean install() {
        return mInstall;
    }

    public void setInstall(boolean install) {
        mInstall = install;
    }

    public boolean check() {
        return mCheck;
    }

    public void setCheck(boolean check) {
        mCheck = check;
    }
}
