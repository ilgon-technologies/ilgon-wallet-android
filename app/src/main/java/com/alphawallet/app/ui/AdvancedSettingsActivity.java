package com.alphawallet.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.viewmodel.AdvancedSettingsViewModel;
import com.alphawallet.app.viewmodel.AdvancedSettingsViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.SettingsItemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.CHANGED_LOCALE;
import static com.alphawallet.app.C.CHANGE_CURRENCY;
import static com.alphawallet.app.C.EXTRA_CURRENCY;
import static com.alphawallet.app.C.EXTRA_LOCALE;
import static com.alphawallet.app.C.EXTRA_STATE;

public class AdvancedSettingsActivity extends BaseActivity {
    @Inject
    AdvancedSettingsViewModelFactory viewModelFactory;
    private AdvancedSettingsViewModel viewModel;

    private SettingsItemView clearBrowserCache;
    private SettingsItemView changeLanguage;
    private SettingsItemView changeCurrency;
    private SettingsItemView fullScreenSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(AdvancedSettingsViewModel.class);

        setContentView(R.layout.activity_generic_settings);
        toolbar();
        setTitle(getString(R.string.title_advanced));

        viewModel.setLocale(this);

        initializeSettings();

        addSettingsToLayout();
    }

    private void initializeSettings() {
        clearBrowserCache = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_cache)
                .withTitle(R.string.title_clear_browser_cache)
                .withListener(this::onClearBrowserCacheClicked)
                .build();

        changeLanguage = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_language)
                .withTitle(R.string.title_change_language)
                .withListener(this::onChangeLanguageClicked)
                .build();

        changeCurrency = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_currency)
                .withTitle(R.string.settings_locale_currency)
                .withListener(this::onChangeCurrencyClicked)
                .build();

        fullScreenSettings = new SettingsItemView.Builder(this)
                        .withType(SettingsItemView.Type.TOGGLE)
                        .withIcon(R.drawable.ic_phoneicon)
                        .withTitle(R.string.fullscreen)
                        .withListener(this::onFullScreenClicked)
                        .build();

        changeLanguage.setSubtitle(LocaleUtils.getDisplayLanguage(viewModel.getActiveLocale(), viewModel.getActiveLocale()));
        fullScreenSettings.setToggleState(viewModel.getFullScreenState());
    }

    private void onFullScreenClicked()
    {
        viewModel.setFullScreenState(fullScreenSettings.getToggleState());
    }

    private void addSettingsToLayout() {
        LinearLayout advancedSettingsLayout = findViewById(R.id.layout);

        advancedSettingsLayout.addView(clearBrowserCache);
        advancedSettingsLayout.addView(changeLanguage);
        advancedSettingsLayout.addView(changeCurrency);
        advancedSettingsLayout.addView(fullScreenSettings);
    }

    private void onClearBrowserCacheClicked() {
        WebView webView = new WebView(this);
        webView.clearCache(true);
        Toast.makeText(this, getString(R.string.toast_browser_cache_cleared), Toast.LENGTH_SHORT).show();
    }


    private void onChangeLanguageClicked() {
        Intent intent = new Intent(this, SelectLocaleActivity.class);
        String selectedLocale = viewModel.getActiveLocale();
        intent.putExtra(EXTRA_LOCALE, selectedLocale);
        intent.putParcelableArrayListExtra(EXTRA_STATE, viewModel.getLocaleList(this));
        startActivityForResult(intent, C.UPDATE_LOCALE);
    }

    private void onChangeCurrencyClicked() {
        Intent intent = new Intent(this, SelectCurrencyActivity.class);
        String currentLocale = viewModel.getDefaultCurrency();
        intent.putExtra(EXTRA_CURRENCY, currentLocale);
        intent.putParcelableArrayListExtra(EXTRA_STATE, viewModel.getCurrencyList());
        startActivityForResult(intent, C.UPDATE_CURRENCY);
    }

    public void updateLocale(Intent data) {
        if (data != null)
        {
            String newLocale = data.getStringExtra(C.EXTRA_LOCALE);
            String oldLocale = viewModel.getActiveLocale();
            if (!TextUtils.isEmpty(newLocale) && !newLocale.equals(oldLocale))
            {
                sendBroadcast(new Intent(CHANGED_LOCALE));
                viewModel.updateLocale(newLocale, this);
            }
        }
    }

    public void updateCurrency(Intent data)
    {
        if (data == null) return;
        String currencyCode = data.getStringExtra(C.EXTRA_CURRENCY);

        //Check if selected currency code is previous selected one then don't update
        if(viewModel.getDefaultCurrency().equals(currencyCode)) return;

        viewModel.updateCurrency(currencyCode);

        //send broadcast to HomeActivity about change
        sendBroadcast(new Intent(CHANGE_CURRENCY));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case C.UPDATE_LOCALE: {
                updateLocale(data);
                break;
            }
            case C.UPDATE_CURRENCY: {
                updateCurrency(data);
                break;
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        changeCurrency.setSubtitle(viewModel.getDefaultCurrency());
    }
}
