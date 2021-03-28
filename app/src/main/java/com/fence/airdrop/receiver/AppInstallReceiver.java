package com.fence.airdrop.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fence.airdrop.observer.AppInstallPublisher;

public class AppInstallReceiver extends BroadcastReceiver  {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppInstallPublisher.getInstance().notify(intent);
    }
}
