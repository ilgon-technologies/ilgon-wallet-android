package com.alphawallet.app.di;

import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.GetDefaultWalletBalance;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepository;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.viewmodel.NewSettingsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class NewSettingsModule {
    @Provides
    NewSettingsViewModelFactory provideNewSettingsViewModelFactory(
            LocaleRepositoryType localeRepository,
            CurrencyRepositoryType currencyRepository,
            GenericWalletInteract genericWalletInteract,
            MyAddressRouter myAddressRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            PreferenceRepositoryType preferenceRepository
    ) {
        return new NewSettingsViewModelFactory(
                localeRepository,
                currencyRepository,
                genericWalletInteract,
                myAddressRouter,
                ethereumNetworkRepository,
                manageWalletsRouter,
                preferenceRepository);
    }

    @Provides
    LocaleRepositoryType provideLocaleRepository(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }

    @Provides
    CurrencyRepositoryType provideCurrencyRepository(PreferenceRepositoryType preferenceRepository) {
        return new CurrencyRepository(preferenceRepository);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    GetDefaultWalletBalance provideGetDefaultWalletBalance(
            WalletRepositoryType walletRepository, EthereumNetworkRepositoryType ethereumNetworkRepository) {
        return new GetDefaultWalletBalance(walletRepository, ethereumNetworkRepository);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    ManageWalletsRouter provideManageWalletsRouter() {
        return new ManageWalletsRouter();
    }
}
