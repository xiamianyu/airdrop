package com.fence.airdrop.observer;

import android.net.NetworkInfo;

import java.util.Observable;

public class WiFiPublisher extends Observable {

    private WiFiPublisher() {
    }

    private static class SingletonFactory {
        private static WiFiPublisher sInstance = new WiFiPublisher();
    }

    public static WiFiPublisher getInstance() {
        return SingletonFactory.sInstance;
    }

    public void notify(NetworkInfo.State mState) {
        setChanged();
        notifyObservers(mState);
    }
}
