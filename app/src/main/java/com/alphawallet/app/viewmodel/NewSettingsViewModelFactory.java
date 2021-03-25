package com.alphawallet.app.viewmodel;


import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;

public class NewSettingsViewModelFactory implements ViewModelProvider.Factory {
    private final LocaleRepositoryType localeRepository;
    private final CurrencyRepositoryType currencyRepository;
    private final MyAddressRouter myAddressRouter;
    private final GenericWalletInteract genericWalletInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final ManageWalletsRouter manageWalletsRouter;
    private final PreferenceRepositoryType preferenceRepository;

    public NewSettingsViewModelFactory(
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

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new NewSettingsViewModel(
                localeRepository,
                currencyRepository,
                genericWalletInteract,
                myAddressRouter,
                ethereumNetworkRepository,
                manageWalletsRouter,
                preferenceRepository
        );
    }
}
