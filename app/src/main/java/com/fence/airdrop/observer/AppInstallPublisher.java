package com.fence.airdrop.observer;

import android.content.Intent;

import java.util.Observable;

public class AppInstallPublisher extends Observable {

    private AppInstallPublisher() {
    }

    private static class SingletonFactory {
        private static AppInstallPublisher sInstance = new AppInstallPublisher();
    }

    public static AppInstallPublisher getInstance() {
        return SingletonFactory.sInstance;
    }

    public void notify(Intent intent) {
        setChanged();
        notifyObservers(intent);
    }
}
