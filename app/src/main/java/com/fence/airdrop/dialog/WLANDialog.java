package com.fence.airdrop.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.fence.airdrop.R;
import com.fence.airdrop.service.WebService;
import com.fence.airdrop.observer.WiFiPublisher;
import com.fence.airdrop.receiver.WifiConnectChangedReceiver;
import com.fence.airdrop.util.WifiUtil;

import java.util.Observable;
import java.util.Observer;

public class WLANDialog extends DialogFragment implements Observer {

    private TextView mUrlTv;
    private TextView mWifiTv;
    private WifiConnectChangedReceiver mWifiConnectChangedReceiver;

    public static WLANDialog newInstance() {
        return new WLANDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initVariables();
        registerWifiConnectChangedReceiver();
    }

    private void initVariables() {
        WiFiPublisher.getInstance().addObserver(this);
        mWifiConnectChangedReceiver = new WifiConnectChangedReceiver();
    }

    private void registerWifiConnectChangedReceiver() {
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        getActivity().registerReceiver(mWifiConnectChangedReceiver, intentFilter);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.dialog_airdrop, null);
        mUrlTv = view.findViewById(R.id.airdrop_tv_url);
        mWifiTv = view.findViewById(R.id.airdrop_tv_wifi);

        return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.airdrop_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                unregisterWifiConnectChangedReceiver();
                dismiss();
            })
            .create();
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!(o instanceof WiFiPublisher)) {
            return;
        }

        NetworkInfo.State state = (NetworkInfo.State) arg;
        if (state == NetworkInfo.State.CONNECTED) {
            String wifiName = WifiUtil.getWifiName(getActivity());
            String ip = WifiUtil.getWifiIp(getActivity());

            if (TextUtils.isEmpty(ip)) {
                onWifiDisconnected();
            } else {
                onWifiConnected(wifiName, ip);
            }
        } else if (state == NetworkInfo.State.CONNECTING) {
            onWifiConnecting();
        } else {
            onWifiDisconnected();
        }
    }

    private void onWifiDisconnected() {
        mWifiTv.setEnabled(false);
        mWifiTv.setText("none");
        mUrlTv.setText(R.string.airdrop_wifi_disconnected);
    }

    private void onWifiConnecting() {
        mWifiTv.setEnabled(false);
        mWifiTv.setText("none");
        mUrlTv.setText(R.string.airdrop_wifi_connecting);
    }

    private void onWifiConnected(String wifiName, String ip) {
        mWifiTv.setEnabled(true);
        mWifiTv.setText(wifiName);
        mUrlTv.setText(String.format(getString(R.string.airdrop_url), ip, WebService.PORT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterWifiConnectChangedReceiver();
        WiFiPublisher.getInstance().deleteObserver(this);
    }

    private void unregisterWifiConnectChangedReceiver() {
        if (mWifiConnectChangedReceiver != null) {
            try {
                getActivity().unregisterReceiver(mWifiConnectChangedReceiver);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
