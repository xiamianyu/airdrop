package com.fence.airdrop.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fence.airdrop.R;
import com.fence.airdrop.adapter.ApkAdapter;
import com.fence.airdrop.bean.Apk;
import com.fence.airdrop.dialog.WLANDialog;
import com.fence.airdrop.manager.ApkManager;
import com.fence.airdrop.observer.ApkPublisher;
import com.fence.airdrop.observer.AppInstallPublisher;
import com.fence.airdrop.service.WebService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements
    SwipeRefreshLayout.OnRefreshListener, Observer,
    ApkAdapter.OnInstallListener, ApkAdapter.OnUninstallListener,
    ApkAdapter.OnItemLongClickListener, ApkAdapter.OnItemCheckListener {

    private SwipeRefreshLayout mRefreshSlyt;
    private RelativeLayout mSelectAllRlyt;
    private CheckBox mSelectAllCb;
    private CheckBox mEditCb;
    private TextView mDeleteTv;
    private RecyclerView mAppApkRlv;
    private FloatingActionButton mWLANBtn;

    private List<Apk> mApks;
    private ApkAdapter mAdapter;

    private Apk mLastInstallApk;
    private Apk mInstallApk;
    private Apk mUninstallApk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initVariables();
        findViews();
        setViews();
        loadApks();
    }

    private void initVariables() {
        WebService.start(this);
        ApkPublisher.getInstance().addObserver(this);
        AppInstallPublisher.getInstance().addObserver(this);

        mApks = new ArrayList<>();
        mAdapter = new ApkAdapter(this, mApks);
    }

    private void findViews() {
        mRefreshSlyt = findViewById(R.id.main_slyt_refresh);
        mSelectAllRlyt = findViewById(R.id.main_rlyt_select_all);
        mSelectAllCb = findViewById(R.id.main_cb_select_all);
        mEditCb = findViewById(R.id.main_cb_edit);
        mDeleteTv = findViewById(R.id.main_tv_delete);
        mAppApkRlv = findViewById(R.id.main_rlv_apk);
        mWLANBtn = findViewById(R.id.main_btn_wlan);
    }

    private void setViews() {
        setSwipeRefreshLayout();
        setRecyclerView();
        setWLANListener();
        setSelectListener();
        setEditListener();
        setDeleteListener();
    }

    private void setSwipeRefreshLayout() {
        mRefreshSlyt.setOnRefreshListener(this);
        mRefreshSlyt.setColorSchemeColors(getResources().getColor(R.color.blue));
    }

    private void setRecyclerView() {
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        mAppApkRlv.addItemDecoration(divider);

        mAppApkRlv.setItemAnimator(new DefaultItemAnimator());
        mAppApkRlv.setLayoutManager(new LinearLayoutManager(this));
        mAppApkRlv.setAdapter(mAdapter);
    }

    private void setWLANListener() {
        mWLANBtn.setOnClickListener(view -> {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

            WLANDialog dialog = WLANDialog.newInstance();
            dialog.show(transaction, "airdrop");
        });
    }

    private void setSelectListener() {
        mSelectAllCb.setOnClickListener(view -> {
            boolean checked = mSelectAllCb.isChecked();
            mDeleteTv.setEnabled(checked);

            for (Apk apk : mApks) {
                apk.setCheck(checked);
            }

            mAdapter.notifyDataSetChanged();
        });
    }

    private void setEditListener() {
        mEditCb.setOnClickListener(view -> {
            boolean checked = mEditCb.isChecked();
            if (checked) {
                mEditCb.setText(getString(R.string.cancel));
                mSelectAllRlyt.setVisibility(View.VISIBLE);
            } else {
                resetEditState();

                for (Apk apk : mApks) {
                    apk.setCheck(false);
                }
            }

            mAdapter.setCheckVisible(checked);
            mAdapter.notifyDataSetChanged();
        });
    }

    private void setDeleteListener() {
        mDeleteTv.setOnClickListener(view -> {
            String message = getString(R.string.delete_apks);

            new AlertDialog.Builder(this)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.submit, (dialog, which) -> {
                    List<Apk> apks = getDeletedApks();
                    ApkManager.delete(apks);
                    mApks.removeAll(apks);
                    mAdapter.setCheckVisible(false);
                    mAdapter.notifyDataSetChanged();

                    resetEditState();
                })
                .create()
                .show();
        });
    }

    private List<Apk> getDeletedApks() {
        List<Apk> apks = new ArrayList<>();

        for (Apk apk : mApks) {
            if (apk.check()) {
                apks.add(apk);
            }
        }

        return apks;
    }

    @Override
    public void onRefresh() {
        loadApks();
    }

    private void loadApks() {
        mRefreshSlyt.setRefreshing(true);

        new Thread(() -> {
            List<Apk> appInfos = ApkManager.getApks(MainActivity.this, WebService.APK_DIR);

            runOnUiThread(() -> {
                mApks.clear();
                mApks.addAll(appInfos);
                mAdapter.notifyDataSetChanged();
                mRefreshSlyt.setRefreshing(false);

                setEditVisibility();
            });
        }).start();
    }

    @Override
    public void onInstall(Apk apk) {
        mLastInstallApk = mInstallApk;
        mInstallApk = apk;
        ApkManager.installApp(this, apk.getPath());
    }

    @Override
    public void onUninstall(Apk apk) {
        mUninstallApk = apk;
        ApkManager.uninstallApp(this, apk.getPackage());
    }

    @Override
    public void onItemLongClick(Apk apk, int position) {
        String message = getString(R.string.delete_apk) + apk.getPath();

        new AlertDialog.Builder(this)
            .setIcon(apk.getIcon())
            .setTitle(apk.getAppName())
            .setMessage(message)
            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
            .setPositiveButton(R.string.submit, (dialog, which) -> {
                removeApk(position);
                ApkManager.delete(apk.getPath());
            })
            .create()
            .show();
    }

    private void removeApk(int position) {
        mApks.remove(position);
        mAdapter.notifyItemRemoved(position);

        if (position != mApks.size()) {
            mAdapter.notifyItemRangeChanged(position, mApks.size() - position);
        }

        if (mApks.isEmpty()) {
            resetEditState();

            mAdapter.setCheckVisible(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void resetEditState() {
        mEditCb.setChecked(false);
        mEditCb.setVisibility(mApks.isEmpty() ? View.GONE : View.VISIBLE);
        mEditCb.setText(getString(R.string.edit));

        mDeleteTv.setEnabled(false);

        mSelectAllCb.setChecked(false);
        mSelectAllRlyt.setVisibility(View.GONE);
    }

    @Override
    public void onItemCheck(Apk apk, int position) {
        boolean allChecked = true;
        boolean allUnchecked = true;

        for (Apk item : mApks) {
            if (item.check()) {
                allUnchecked = false;
            } else {
                allChecked = false;
            }
        }

        mDeleteTv.setEnabled(!allUnchecked);
        mSelectAllCb.setChecked(allChecked);
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (observable == null || arg == null) {
            return;
        }

        if (observable instanceof ApkPublisher) {
            handleApk((ApkPublisher) observable, (File) arg);
        }

        if (observable instanceof AppInstallPublisher) {
            updateInstallState((Intent) arg);
        }
    }

    private void handleApk(ApkPublisher publisher, File file) {
        if (publisher.deleteApk()) {
            for (Apk apk : mApks) {
                if (!apk.getPath().equals(file.getAbsolutePath())) {
                    continue;
                }

                int position = mApks.indexOf(apk);
                removeApk(position);
                return;
            }
        } else {
            Apk apk = ApkManager.getApk(this, file);
            apk.setCheck(mSelectAllCb.isChecked());

            mApks.add(apk);
            mAdapter.notifyDataSetChanged();

        }

        setEditVisibility();
    }

    private void setEditVisibility() {
        mEditCb.setVisibility(mApks.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * 更新 APK 的安装状态
     *
     * @param intent
     */
    private void updateInstallState(Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();

        if (invalid(packageName)) {
            return;
        }

        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) { // 安装
            if (mInstallApk != null) {
                mInstallApk.setInstall(true);
            }
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) { // 卸载
            if (mUninstallApk != null) {
                mUninstallApk.setInstall(false);
            }
        } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) { // 替换
            if (mInstallApk != null) {
                mInstallApk.setInstall(true);
            }

            if (mLastInstallApk != null) {
                mLastInstallApk.setInstall(false);
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    private boolean invalid(String packageName) {
        return !(mInstallApk != null && mInstallApk.getPackage().equals(packageName) ||
            mUninstallApk != null && mUninstallApk.getPackage().equals(packageName));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ApkPublisher.getInstance().deleteObserver(this);
    }
}