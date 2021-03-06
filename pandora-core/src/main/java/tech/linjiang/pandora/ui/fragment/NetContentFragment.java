package tech.linjiang.pandora.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import tech.linjiang.pandora.core.R;
import tech.linjiang.pandora.network.CacheDbHelper;
import tech.linjiang.pandora.network.model.Content;
import tech.linjiang.pandora.util.SimpleTask;
import tech.linjiang.pandora.util.Utils;

/**
 * Created by linjiang on 2018/6/24.
 */

public class NetContentFragment extends BaseFragment {

    private boolean showResponse;
    private long id;
    private String contentType;
    private String originContent;
    private WebView webView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showResponse = getArguments().getBoolean(PARAM1, true);
        id = getArguments().getLong(PARAM2);
        contentType = getArguments().getString(PARAM3);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected View getLayoutView() {
        webView = new WebView(getContext());
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                loadData();
            }
        });
        return webView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getToolbar().setTitle("Content");
        webView.loadUrl("file:///android_asset/tmp_json.html");

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        closeSoftInput();
    }

    @Override
    protected int getLayoutId() {
        return 0;
    }

    private void setSearchView() {
        getToolbar().getMenu().add(-1, R.id.pd_menu_id_1, 0, "copy");
        getToolbar().getMenu().add(-1, R.id.pd_menu_id_3, 2, "share");
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.pd_menu_id_1) {
                    Utils.copy2ClipBoard(originContent);
                } else if (item.getItemId() == R.id.pd_menu_id_3) {
                    Utils.shareText(originContent);
                }
                return true;
            }
        });
    }

    private void loadData() {
        showLoading();
        new SimpleTask<>(new SimpleTask.Callback<Void, String>() {
            @Override
            public String doInBackground(Void[] params) {
                Content content = CacheDbHelper.getContent(id);
                String result;
                if (showResponse) {
                    result = content.responseBody;
                } else {
                    result = content.requestBody;
                }

                return result;
            }

            @Override
            public void onPostExecute(String result) {
                Log.d(TAG, "onPostExecute: " + result);
                hideLoading();
                if (TextUtils.isEmpty(result)) {
                    Utils.toast("empty");
                    return;
                }
                setSearchView();
                originContent = result;
                webView.setWebViewClient(null);

                if (contentType != null && contentType.toLowerCase().contains("json")) {
                    // help me
                    result = result.replaceAll("\n", "");
                    result = result.replace("\\", "\\\\");
                    result = result.replace("'", "\\\'");
                    // https://issuetracker.google.com/issues/36995865
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        webView.loadUrl(String.format("javascript:showJson('%s')", result));
                    } else {
                        webView.evaluateJavascript(String.format("showJson('%s')", result), null);
                    }
                } else {
                    webView.loadDataWithBaseURL(null, result,  decideMimeType(), "utf-8", null);
                }
            }
        }).execute();
    }



    private String decideMimeType() {
        if (contentType != null && contentType.toLowerCase().contains("xml")) {
            return "text/xml";
        } else {
            return "text/html";
        }
    }

}
