package com.fence.airdrop.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fence.airdrop.R;
import com.fence.airdrop.bean.Apk;

import java.util.List;

public class ApkAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private OnInstallListener mOnInstallListener;
    private OnUninstallListener mOnUninstallListener;
    private OnItemLongClickListener mOnItemLongListener;
    private OnItemCheckListener mOnItemCheckListener;

    private boolean mCheckVisible;
    private List<Apk> mApks;

    public ApkAdapter(Context context, List<Apk> apks) {
        mContext = context;

        if (context instanceof OnInstallListener) {
            mOnInstallListener = (OnInstallListener) context;
        }

        if (context instanceof OnUninstallListener) {
            mOnUninstallListener = (OnUninstallListener) context;
        }

        if (context instanceof OnItemLongClickListener) {
            mOnItemLongListener = (OnItemLongClickListener) context;
        }

        if (context instanceof OnItemCheckListener) {
            mOnItemCheckListener = (OnItemCheckListener) context;
        }

        mApks = apks;
    }

    @Override
    public int getItemCount() {
        return mApks.size() > 0 ? mApks.size() : 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (mApks.size() == 0) {
            return 1;
        }

        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);

        if (viewType == 1) {
            View view = inflater.inflate(R.layout.emptyview, parent, false);
            return new EmptyViewHolder(view);
        } else {
            return new ViewHolder(inflater.inflate(R.layout.item_recyclerview_apk, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyViewHolder) {
            return;
        }
        ApkAdapter.ViewHolder viewHolder = (ViewHolder) holder;

        Apk apk = mApks.get(position);

        viewHolder.iconIv.setImageDrawable(apk.getIcon());
        viewHolder.appNameTv.setText(apk.getAppName());
        viewHolder.apkNameTv.setText(String.format(mContext.getString(R.string.item_apk_tv_apk_name), apk.getApkName()));
        viewHolder.versionTv.setText(String.format(mContext.getString(R.string.item_apk_tv_version), apk.getVersion()));
        viewHolder.sizeTv.setText(String.format(mContext.getString(R.string.item_apk_tv_size), apk.getSize()));
        viewHolder.channelTv.setText(String.format(mContext.getString(R.string.item_apk_tv_channel), apk.getChannel()));

        viewHolder.installTv.setEnabled(!apk.install());
        viewHolder.uninstallTv.setEnabled(apk.install());
        viewHolder.checkCb.setChecked(apk.check());

        viewHolder.installTv.setOnClickListener(view -> {
            if (mOnInstallListener != null) {
                mOnInstallListener.onInstall(apk);
            }
        });

        viewHolder.uninstallTv.setOnClickListener(view -> {
            if (mOnUninstallListener != null) {
                mOnUninstallListener.onUninstall(apk);
            }
        });

        viewHolder.containerLlyt.setOnLongClickListener(view -> {
            if (mOnItemLongListener != null) {
                mOnItemLongListener.onItemLongClick(apk, position);
            }
            return true;
        });

        if (mCheckVisible) {
            viewHolder.checkCb.setVisibility(View.VISIBLE);
        } else {
            viewHolder.checkCb.setVisibility(View.GONE);
        }

        viewHolder.checkCb.setOnClickListener(view -> {
            apk.setCheck(viewHolder.checkCb.isChecked());

            if (mOnItemCheckListener != null) {
                mOnItemCheckListener.onItemCheck(apk, position);
            }
        });
    }

    public void setCheckVisible(boolean checkVisible) {
        mCheckVisible = checkVisible;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        LinearLayout containerLlyt;
        ImageView iconIv;
        TextView appNameTv;
        TextView apkNameTv;
        TextView versionTv;
        TextView sizeTv;
        TextView channelTv;
        TextView uninstallTv;
        TextView installTv;
        CheckBox checkCb;

        public ViewHolder(View itemView) {
            super(itemView);
            containerLlyt = itemView.findViewById(R.id.item_appinfo_llyt_container);
            iconIv = itemView.findViewById(R.id.item_apk_iv_icon);
            appNameTv = itemView.findViewById(R.id.item_apk_tv_app_name);
            apkNameTv = itemView.findViewById(R.id.item_apk_tv_apk_name);
            versionTv = itemView.findViewById(R.id.item_apk_tv_version);
            sizeTv = itemView.findViewById(R.id.item_apk_tv_size);
            channelTv = itemView.findViewById(R.id.item_apk_tv_channel);
            uninstallTv = itemView.findViewById(R.id.item_apk_tv_uninstall);
            installTv = itemView.findViewById(R.id.item_apk_tv_install);
            checkCb = itemView.findViewById(R.id.item_apk_cb_check);
        }
    }

    private static class EmptyViewHolder extends RecyclerView.ViewHolder {

        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnInstallListener {
        void onInstall(Apk apk);
    }

    public interface OnUninstallListener {
        void onUninstall(Apk apk);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Apk apk, int position);
    }

    public interface OnItemCheckListener {
        void onItemCheck(Apk apk, int position);
    }
}
