package com.fence.airdrop.observer;

import java.io.File;
import java.util.Observable;

public class ApkPublisher extends Observable {

    private boolean mDeleteApk;

    private ApkPublisher() {
    }

    private static class SingletonFactory {
        private static ApkPublisher sInstance = new ApkPublisher();
    }

    public static ApkPublisher getInstance() {
        return SingletonFactory.sInstance;
    }

    public boolean deleteApk() {
        return mDeleteApk;
    }

    public void setDeleteApk(boolean deleteApk) {
        mDeleteApk = deleteApk;
    }

    public void notify(File apk) {
        setChanged();
        notifyObservers(apk);
    }
}
