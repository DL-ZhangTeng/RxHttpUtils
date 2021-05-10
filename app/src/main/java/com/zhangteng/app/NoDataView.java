package com.zhangteng.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.zhangteng.app.R;

/**
 * 请求无数据显示view
 *
 * @author swing
 * @date 2018/1/23
 */

public class NoDataView extends LinearLayout {
    private ConstraintLayout llNoData;
    private TextView tvNoData;
    private ImageView ivNoData;
    private Button btnNoData;
    private boolean isNoDataViewShow = false;

    public NoDataView(Context context) {
        super(context);
        initView(context);
    }

    public NoDataView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        setAttrs(context, attrs);
    }

    public NoDataView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        setAttrs(context, attrs);
    }

    private void setAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.NoDataView);
        final int indexCount = a.getIndexCount();
        for (int i = 0; i < indexCount; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.NoDataView_nodatatext) {
                String str = a.getString(attr);
                setNoDataText(str);
            } else if (attr == R.styleable.NoDataView_nodataimage) {
                int id = a.getResourceId(attr, R.mipmap.wangluowu);
                setNoDataImageResource(id);
            } else if (attr == R.styleable.NoDataView_nodatavisibility) {
                int visibility = a.getInt(attr, View.VISIBLE);
                setNoDataVisibility(visibility);
            }
        }
        a.recycle();
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_no_data_view, this, true);
        llNoData = findViewById(R.id.ll_no_data);
        tvNoData = findViewById(R.id.tv_no_data);
        ivNoData = findViewById(R.id.iv_no_data);
        btnNoData = findViewById(R.id.btn_no_data);
    }

    public void setNoDataVisibility(int visibility) {
        llNoData.setVisibility(visibility);
    }

    public void setNoDataText(String noDataText) {
        tvNoData.setText(noDataText);
    }

    public void setNoDataText(int resourceId) {
        tvNoData.setText(resourceId);
    }

    public void setNoDataDrawable(Drawable dataDrawable) {
        ivNoData.setImageDrawable(dataDrawable);
    }

    public void setNoDataImageResource(int resourceId) {
        ivNoData.setImageResource(resourceId);
    }

    public boolean isNoDataViewShow() {
        return isNoDataViewShow;
    }

    public void setNoDataViewShow(boolean noDataViewShow) {
        isNoDataViewShow = noDataViewShow;
    }

    public void setNoDataAgainText(String noDataAgainText) {
        btnNoData.setText(noDataAgainText);
    }

    public void setNoDataAgainVisivility(int visivility) {
        btnNoData.setVisibility(visivility);
    }

    public void setAgainRequestListener(final AgainRequestListener againRequestListener) {
        btnNoData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (againRequestListener != null) againRequestListener.request();
            }
        });
    }

    public interface AgainRequestListener {
        void request();
    }
}
