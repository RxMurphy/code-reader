package com.loopeer.codereader.ui.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.loopeer.codereader.Navigator;
import com.loopeer.codereader.R;
import com.loopeer.codereader.coreader.db.CoReaderDbHelper;
import com.loopeer.codereader.model.Repo;
import com.loopeer.codereader.ui.view.NestedScrollWebView;
import com.loopeer.codereader.utils.DownloadUrlParser;
import com.loopeer.codereader.utils.FileCache;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SimpleWebActivity extends BaseActivity {
    private static final String TAG = "SimpleWebActivity";

    @BindView(R.id.web_content)
    NestedScrollWebView mWebContent;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.progress_bar_web)
    ProgressBar mProgressBar;

    private String mUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_web);
        ButterKnife.bind(this);

        initWeb();
        parseIntent();
    }

    private void initWeb() {
        mWebContent.getSettings().setJavaScriptEnabled(true);
        mWebContent.getSettings().setDomStorageEnabled(true);
        mWebContent.getSettings().setGeolocationEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebContent.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        mWebContent.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                mUrl = url;
                view.loadUrl(mUrl);
                return true;
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                mUrl = String.valueOf(request.getUrl());
                view.loadUrl(mUrl);
                return true;
            }
        });
        mWebContent.setWebChromeClient(new WebChromeClient());
    }

    public class WebChromeClient extends android.webkit.WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                mProgressBar.setVisibility(View.GONE);
            } else {
                if (mProgressBar.getVisibility() == View.GONE) mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(newProgress);
            }
            super.onProgressChanged(view, newProgress);
        }

    }

    private void parseIntent() {
        Intent intent = getIntent();
        mUrl = intent.getStringExtra(Navigator.EXTRA_WEB_URL);
        String htmlString = intent.getStringExtra(Navigator.EXTRA_HTML_STRING);
        if (mUrl != null) loadUrl(mUrl);
        if (htmlString != null) loadData(htmlString);
    }

    private void loadData(String htmlString) {
        mWebContent.loadData(htmlString, "text/html", "utf-8");
    }

    private void loadUrl(String webUrl) {
        mWebContent.loadUrl(webUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            if (!TextUtils.isEmpty(mUrl)) {
                String downloadUrl = DownloadUrlParser.parseUrl(mUrl);
                String repoName = DownloadUrlParser.getRepoName(mUrl);
                Repo repo = new Repo(repoName
                        , FileCache.getInstance().getRepoAbsolutePath(repoName), mUrl, true, 0);
                long repoId = CoReaderDbHelper.getInstance(this).insertRepo(repo);
                repo.id = String.valueOf(repoId);
                Navigator.startDownloadRepoService(SimpleWebActivity.this, downloadUrl, repo);
            }
            return true;
        }
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mWebContent.canGoBack()) {
                        mWebContent.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebContent.destroy();
    }
}