package com.alphawallet.app.viewmodel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.entity.LocaleItem;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.util.LocaleUtils;

import java.util.ArrayList;

import io.reactivex.Single;

public class NewSettingsViewModel extends BaseViewModel {

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<String> backUpMessage = new MutableLiveData<>();
    private final GenericWalletInteract genericWalletInteract;
    private final MyAddressRouter myAddressRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final ManageWalletsRouter manageWalletsRouter;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;
    private final CurrencyRepositoryType currencyRepository;

    NewSettingsViewModel(
            LocaleRepositoryType localeRepository,
            CurrencyRepositoryType currencyRepository,
            GenericWalletInteract genericWalletInteract,
            MyAddressRouter myAddressRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            PreferenceRepositoryType preferenceRepository) {
        this.localeRepository = localeRepository;
        this.currencyRepository = currencyRepository;
        this.genericWalletInteract = genericWalletInteract;
        this.myAddressRouter = myAddressRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.manageWalletsRouter = manageWalletsRouter;
        this.preferenceRepository = preferenceRepository;
    }

    public void showManageWallets(Context context, boolean clearStack) {
        manageWalletsRouter.open(context, clearStack);
    }

    public void setNetwork(String selectedRpcServer) {
        NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();
        for (NetworkInfo networkInfo : networks) {
            if (networkInfo.name.equals(selectedRpcServer)) {
                ethereumNetworkRepository.setDefaultNetworkInfo(networkInfo);
                return;
            }
        }
    }

    public boolean getNotificationState()
    {
        return preferenceRepository.getNotificationsState();
    }
    public void setNotificationState(boolean notificationState)
    {
        preferenceRepository.setNotificationState(notificationState);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<Transaction[]> transactions() {
        return transactions;
    }
    public LiveData<String> backUpMessage() { return backUpMessage; }

    public void prepare() {
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);

        TestWalletBackup();
    }

    public void TestWalletBackup()
    {
        if (defaultWallet.getValue() != null)
        {
            genericWalletInteract.getWalletNeedsBackup(defaultWallet.getValue().address)
                    .subscribe(backUpMessage::postValue).isDisposed();
        }
    }

    public void showMyAddress(Context context) {
        myAddressRouter.open(context, defaultWallet.getValue());
    }

    public Single<String> setIsDismissed(String walletAddr, boolean isDismissed)
    {
        return genericWalletInteract.setIsDismissed(walletAddr, isDismissed);
    }

    public String getUserPreferenceLocale()
    {
        return localeRepository.getUserPreferenceLocale();
    }

    public ArrayList<LocaleItem> getLocaleList(Context context) {
        return localeRepository.getLocaleList(context);
    }

    public void setLocale(Context activity) {
        String currentLocale = localeRepository.getActiveLocale();
        LocaleUtils.setLocale(activity, currentLocale);
    }

    public void updateLocale(String newLocale, Context context) {
        localeRepository.setUserPreferenceLocale(newLocale);
        localeRepository.setLocale(context, newLocale);
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public String getDefaultCurrency(){
        return currencyRepository.getDefaultCurrency();
    }

    public ArrayList<CurrencyItem> getCurrencyList() {
        return currencyRepository.getCurrencyList();
    }

    public void updateCurrency(String currencyCode){
        currencyRepository.setDefaultCurrency(currencyCode);
    }

    public String getActiveLocale()
    {
        return localeRepository.getActiveLocale();
    }
}
