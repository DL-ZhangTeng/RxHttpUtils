package com.zhangteng.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.AnimationDrawable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhangteng.app.R;

import java.util.ArrayDeque;

/**
 * 将某个视图替换为正在加载、无数据、加载失败等视图
 * Created by swing on 2018/10/8.
 */
public class LoadViewHelper {
    private SparseArray<View> contentViews;
    private SparseArray<NoDataView> noDataViews;
    private Dialog mProgressDialog;
    private TextView loadView;
    private AgainRequestListener againRequestListener;
    private CancelRequestListener cancelRequestListener;
    private ArrayDeque<Dialog> showQueue;
    public final static int NETWORKNO = 0;
    public final static int CONTENTNODATA = 1;

    public LoadViewHelper() {
        contentViews = new SparseArray<>();
        noDataViews = new SparseArray<>();
        showQueue = new ArrayDeque<>();
    }

    /**
     * 网络无数据view
     *
     * @param currentView 需要替换的view
     */
    public void showNetNodataView(View currentView) {
        showNodataView(NETWORKNO, currentView, R.mipmap.wangluowu, "无网络", "点击重试");
    }

    /**
     * 内容无数据view
     *
     * @param currentView 需要替换的view
     */
    public void showContentNodataView(View currentView) {
        showNodataView(CONTENTNODATA, currentView, R.mipmap.neirongwu, "暂无内容~", "");
    }

    /**
     * 显示无数据view
     *
     * @param currentView 需要替换的view
     */
    public void showNodataView(int type, View currentView, int drawableRes, String nodataText, String nodataAgainText) {
        if (contentViews.get(type, null) == null) {
            contentViews.put(type, currentView);
        }
        if (noDataViews.get(type, null) == null) {
            noDataViews.put(type, new NoDataView(contentViews.get(type).getContext()));
        }
        noDataViews.get(type).setNoDataImageResource(drawableRes);
        noDataViews.get(type).setNoDataText(nodataText);
        if (null == nodataAgainText || "".equals(nodataAgainText)) {
            noDataViews.get(type).setNoDataAgainVisivility(View.GONE);
        } else {
            noDataViews.get(type).setNoDataAgainText(nodataAgainText);
        }
        noDataViews.get(type).setAgainRequestListener(() -> {
            if (againRequestListener != null) {
                againRequestListener.request();
            }
        });
        if (noDataViews.get(type).isNoDataViewShow()) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) contentViews.get(type).getParent();
        if (viewGroup != null) {
            viewGroup.removeView(contentViews.get(type));
            viewGroup.addView(noDataViews.get(type), contentViews.get(type).getLayoutParams());
        }
        noDataViews.get(type).setNoDataViewShow(true);
    }

    /**
     * 显示dialog
     *
     * @param mContext dialog上下文
     */
    public void showProgressDialog(Context mContext) {
        showProgressDialog(mContext, "加载中...");
    }

    /**
     * 显示dialog
     *
     * @param mContext     dialog上下文
     * @param mLoadingText dialog文本
     */
    public void showProgressDialog(Context mContext, String mLoadingText) {
        if (mContext == null) {
            return;
        }
        showProgressDialog(mContext, mLoadingText, R.layout.layout_dialog_progress);
    }

    /**
     * 显示dialog
     *
     * @param mContext     dialog上下文
     * @param mLoadingText dialog文本
     * @param layoutRes    dialog布局文件
     */
    public void showProgressDialog(Context mContext, String mLoadingText, int layoutRes) {
        if (mContext == null) {
            return;
        }
        if (mProgressDialog == null) {
            mProgressDialog = new Dialog(mContext, R.style.progress_dialog);
            View view = View.inflate(mContext, layoutRes, null);
            loadView = view.findViewById(R.id.loadView);
            ImageView mImageView = view.findViewById(R.id.progress_bar);
            ((AnimationDrawable) mImageView.getDrawable()).start();
            if (mLoadingText != null) {
                loadView.setText(mLoadingText);
            }
            mProgressDialog.setContentView(view);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnDismissListener(dialog -> {
                if (showQueue != null && !showQueue.isEmpty()) {
                    showQueue.remove(mProgressDialog);
                }
                if (cancelRequestListener != null) {
                    cancelRequestListener.cancel();
                }
            });
            final Activity activity = findActivity(mContext);
            if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
                mProgressDialog = null;
                return;
            } else {
                if (mProgressDialog.getOwnerActivity() == null)
                    mProgressDialog.setOwnerActivity(activity);
            }
            showQueue.add(mProgressDialog);
        } else if (!mProgressDialog.isShowing()) {
            if (mLoadingText != null && loadView != null) {
                loadView.setText(mLoadingText);
            }
            if (!showQueue.contains(mProgressDialog)) {
                final Activity activity = findActivity(mContext);
                if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
                    mProgressDialog = null;
                    return;
                } else {
                    if (mProgressDialog.getOwnerActivity() == null)
                        mProgressDialog.setOwnerActivity(activity);
                }
                showQueue.add(mProgressDialog);
            }
        }
        alwaysShowProgressDialog();
    }

    /**
     * 完成dialog
     */
    public void dismissProgressDialog() {
        if (!showQueue.isEmpty()) {
            Dialog first = showQueue.pollFirst();
            alwaysShowProgressDialog();
            final Activity activity = first.getOwnerActivity();
            if (activity == null || activity.isDestroyed()) {
                showQueue.remove(mProgressDialog);
                dismissProgressDialog();
                return;
            }
            first.dismiss();
        }
    }

    /**
     * 网络无数据view
     *
     * @param currentView 需要替换的view
     */
    public void hiddenNetNodataView(View currentView) {
        hiddenNodataView(NETWORKNO, currentView);
    }

    /**
     * 内容无数据view
     *
     * @param currentView 需要替换的view
     */
    public void hiddenContentNodataView(View currentView) {
        hiddenNodataView(CONTENTNODATA, currentView);
    }

    /**
     * 隐藏无数据view
     *
     * @param currentView 需要替换的view
     */
    public void hiddenNodataView(int type, View currentView) {
        if (contentViews.get(type, null) == null) {
            contentViews.put(type, currentView);
        }
        if (noDataViews.get(type, null) == null) {
            return;
        }
        if (!noDataViews.get(type).isNoDataViewShow()) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) noDataViews.get(type).getParent();
        if (viewGroup != null) {
            viewGroup.removeView(noDataViews.get(type));
            viewGroup.addView(contentViews.get(type));
        }
        noDataViews.get(type).setNoDataViewShow(false);
    }

    private void alwaysShowProgressDialog() {
        if (!showQueue.isEmpty() && !showQueue.getFirst().isShowing()) {
            final Activity activity1 = showQueue.getFirst().getOwnerActivity();
            if (activity1 == null || activity1.isDestroyed() || activity1.isFinishing()) {
                showQueue.remove(mProgressDialog);
                alwaysShowProgressDialog();
                return;
            }
            showQueue.getFirst().show();
        }
    }

    private static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof ContextWrapper) {
            ContextWrapper wrapper = (ContextWrapper) context;
            return findActivity(wrapper.getBaseContext());
        } else {
            return null;
        }
    }

    public void setAgainRequestListener(AgainRequestListener againRequestListener) {
        this.againRequestListener = againRequestListener;
    }

    public void setCancelRequestListener(CancelRequestListener cancelRequestListener) {
        this.cancelRequestListener = cancelRequestListener;
    }

    public interface CancelRequestListener {
        void cancel();
    }

    public interface AgainRequestListener {
        void request();
    }
}