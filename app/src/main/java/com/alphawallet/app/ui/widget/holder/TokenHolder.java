package com.alphawallet.app.ui.widget.holder;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.TokenIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

public class TokenHolder extends BinderViewHolder<TokenCardMeta> implements View.OnClickListener, View.OnLongClickListener {

    public static final int VIEW_TYPE = 1005;
    public static final String EMPTY_BALANCE = "\u2014\u2014";//"$0 USD";
    private final TokenIcon tokenIcon;
    private final TextView balanceEth;
    private final TextView balanceCurrency;
    private final TextView stakingBalance;
    private final TextView compensationBalance;
    private final TextView issuer;
    private final TextView issuerPlaceholder;
    private final TextView contractType;
    private final View contractSeparator;
    private final LinearLayout extendedInfo;
    private final LinearLayout stakingLinearLayout;
    private final LinearLayout compensationLinearLayout;
    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor
    private final TokensService tokensService;
    private final TextView pendingText;
    private final RelativeLayout tokenLayout;
    private final ChainName testnet;
    private RealmResults<RealmTokenTicker> realmUpdate = null;
    private boolean primaryElement;
    private final Realm realm;

    private final Handler handler = new Handler();

    public Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(ViewGroup parent, AssetDefinitionService assetService, TokensService tSvs, Realm r)
    {
        super(R.layout.item_token, parent);

        tokenIcon = findViewById(R.id.token_icon);
        balanceEth = findViewById(R.id.eth_data);
        stakingBalance = findViewById(R.id.staking_balance_text);
        stakingLinearLayout = findViewById(R.id.staking_linear_layout);
        compensationBalance = findViewById(R.id.compensation_balance_text);
        compensationLinearLayout = findViewById(R.id.compensation_linear_layout);
        balanceCurrency = findViewById(R.id.balance_currency);
        balanceCurrency.setVisibility(View.GONE);
        issuer = findViewById(R.id.issuer);
        issuerPlaceholder = findViewById(R.id.issuerPlaceholder);
        contractType = findViewById(R.id.contract_type);
        contractSeparator = findViewById(R.id.contract_seperator);
        pendingText = findViewById(R.id.balance_eth_pending);
        tokenLayout = findViewById(R.id.token_layout);
        extendedInfo = findViewById(R.id.layout_extended_info);
        testnet = findViewById(R.id.chain_name);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
        tokensService = tSvs;
        realm = r;
    }

    private void showInformPopUp()
    {
        /*
        AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext(),R.style.MyDialogStyle);
        builder1.setTitle("Title");
        builder1.setMessage("my message");
        builder1.setCancelable(true);
        builder1.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();*/
    }


    @Override
    public void bind(@Nullable TokenCardMeta data, @NonNull Bundle addition) {

        try
        {
            this.balanceCurrency.setText("");
            token = tokensService.getToken(data.getChain(), data.getAddress());
            if (token == null)
            {
                fillEmpty();
                return;
            }
            else if (data.nameWeight < 1000 && !token.isEthereum())
            {
                //edge condition - looking at a contract as an account
                Token backupChain = tokensService.getToken(data.getChain(), "eth");
                if (backupChain != null) token = backupChain;
            }

            if (realmUpdate != null)
            {
                realmUpdate.removeAllChangeListeners();
                realmUpdate = null;
            }

            tokenLayout.setBackgroundResource(R.drawable.background_marketplace_event);
            if (EthereumNetworkRepository.isPriorityToken(token)) extendedInfo.setVisibility(View.GONE);
            contractSeparator.setVisibility(View.GONE);

            if(token.isEthereum() && token.showStakingBalance()) {
                String text = getString(R.string.staking_balance) + ": " + token.getStringStakingBalance() + " " + token.getSymbol();
                stakingBalance.setText(text);
                stakingLinearLayout.setVisibility(View.VISIBLE);
            } else {
                stakingLinearLayout.setVisibility(View.GONE);
            }

            if(token.isEthereum() && token.showCompensationBalance()) {
                String text = getString(R.string.compensation_balance) + ": " + token.getStringCompensationBalance() + " " + token.getSymbol();
                compensationBalance.setText(text);
                compensationLinearLayout.setVisibility(View.VISIBLE);
            } else {
                compensationLinearLayout.setVisibility(View.GONE);
            }


            //setup name and value (put these together on a single string to make wrap-around text appear better).
            String nameValue = token.getStringBalance() + " " + token.getFullName(assetDefinition, token.getTicketCount());
            balanceEth.setText(nameValue);

            primaryElement = false;

            tokenIcon.bindData(token, assetDefinition);
            tokenIcon.setOnTokenClickListener(onTokenClickListener);

            populateTicker();

            setContractType();

            setPendingAmount();

        } catch (Exception ex) {
            fillEmpty();
        }
    }

   // @Override
    //public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
    //}

    @Override
    public void onDestroyView()
    {
        if (realmUpdate != null)
        {
            realmUpdate.removeAllChangeListeners();
            realmUpdate = null;
        }
    }

    private void setPendingAmount()
    {
        String pendingDiff = token.getPendingDiff();
        if (pendingDiff != null)
        {
            pendingText.setText(pendingDiff);
            pendingText.setTextColor(ContextCompat.getColor(getContext(), (pendingDiff.startsWith("-")) ? R.color.red : R.color.green));
        }
        else
        {
            pendingText.setText("");
        }
    }

    private void populateTicker()
    {
        TokenTicker ticker = tokensService.getTokenTicker(token);
        if (ticker != null || (token.isEthereum() && EthereumNetworkRepository.hasRealValue(token.tokenInfo.chainId)))
        {
            balanceCurrency.setVisibility(View.VISIBLE);
            handleTicker();
        }
        else
        {
            balanceCurrency.setVisibility(View.GONE);
            setIssuerDetails();
        }

        if (!token.isEthereum() && token.tokenInfo.chainId != MAINNET_ID)
        {
            showNetworkLabel();
        }
        else
        {
            hideNetworkLabel();
        }
    }

    private void handleTicker()
    {
        primaryElement = true;
        hideIssuerViews();
        balanceCurrency.setVisibility(View.VISIBLE);
        startTickerRealmListener();
    }

    private void showNetworkLabel() {
        testnet.setVisibility(View.VISIBLE);
        testnet.setChainID(token.tokenInfo.chainId);
    }

    private void hideNetworkLabel() {
        testnet.setVisibility(View.GONE);
    }

    private void fillEmpty() {
        balanceEth.setText(R.string.NA);
        balanceCurrency.setText(EMPTY_BALANCE);
    }

    private Runnable clearElevation = new Runnable()
    {
        @Override
        public void run()
        {
            tokenLayout.setElevation(0.0f);
        }
    };

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null) {
            tokenLayout.setElevation(-10.0f);
            onTokenClickListener.onTokenClick(v, token, null, true);
            handler.postDelayed(clearElevation, 800);
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if (onTokenClickListener != null) {
            onTokenClickListener.onLongTokenClick(v, token, null);
        }

        return true;
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    public void setOnLongClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    private void setIssuerDetails()
    {
        if (token.isEthereum())     // If token is eth and we get here, it's a testnet chain, show testnet
        {
            issuer.setVisibility(View.VISIBLE);
            issuer.setText(R.string.testnet);
            issuerPlaceholder.setVisibility(View.GONE);
            primaryElement = true;
        }
        else
        {
            String chainName = getString(R.string.chain)+": " + (token.tokenInfo.chainId == BuildConfig.MAIN_CHAIN_ID ? "ILGON" : "ILGON Test");
            String issuerAddress = Utils.formatAddress(token.getAddress());
            boolean onlyMainnetActive = (getContext() instanceof HomeActivity) &&
                    ((HomeActivity) getContext()).onlyMainnetActive();
            if (chainName != null && issuerAddress != null)
            {
                issuer.setVisibility(View.VISIBLE);
                issuerPlaceholder.setVisibility(View.VISIBLE);
                primaryElement = true;
                String issuerText = onlyMainnetActive ?
                        " " + issuerAddress :
                        " " + issuerAddress + " | "+chainName;
                issuer.setText(issuerText);
            }
            else
            {
                hideIssuerViews();
            }
        }
    }

    private void hideIssuerViews() {
        issuer.setVisibility(View.GONE);
        issuerPlaceholder.setVisibility(View.GONE);
        contractSeparator.setVisibility(View.GONE);
    }

    private void setContractType()
    {
        //Display contract type if required
        int contractStringId = token.getContractType();
        if (contractStringId > 0)
        {
            contractType.setText(contractStringId);
            contractType.setVisibility(View.VISIBLE);
            if (primaryElement) contractSeparator.setVisibility(View.VISIBLE);
        }
        else
        {
            contractType.setVisibility(View.GONE);
        }
    }

    private void emptyTicker()
    {
        balanceCurrency.setText("");//R.string.unknown_balance_without_symbol);
    }

    private void startTickerRealmListener()
    {
        realmUpdate = realm.where(RealmTokenTicker.class)
                .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()))
                .findAllAsync();
        realmUpdate.addChangeListener(realmTicker -> {
            //update balance
            if (realmTicker.size() == 0) return;
            RealmTokenTicker rawTicker = realmTicker.first();
            if (rawTicker == null) return;
            //update ticker info
            final TokenTicker tt = new TokenTicker(rawTicker.getPrice(), rawTicker.getPercentChange24h(), rawTicker.getCurrencySymbol(),
                    rawTicker.getImage(), rawTicker.getUpdatedTime());
            handler.post(() -> {
                setTickerInfo(tt);
            });
        });
    }

    private void setTickerInfo(TokenTicker ticker)
    {
        if (((Activity)getContext()).isFinishing() || ((Activity) getContext()).isDestroyed()) { return; }

        //Set the fiat equivalent (leftmost value)
        BigDecimal correctedBalance = token.getCorrectedBalance(18);
        BigDecimal fiatBalance = correctedBalance.multiply(new BigDecimal(ticker.price)).setScale(18, RoundingMode.DOWN);
        String converted = TickerService.getCurrencyString(fiatBalance.doubleValue());
        String formattedPercents = "";
        int color = Color.RED;

        String lbl = getString(R.string.token_balance, "", converted);
        lbl += " " + ticker.priceSymbol;
        Spannable spannable;
        if (correctedBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            spannable = new SpannableString(lbl);
            spannable.setSpan(new ForegroundColorSpan(color),
                    converted.length(), lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.balanceCurrency.setText(lbl);
            this.issuer.setVisibility(View.GONE);
        }
        else
        {
            this.balanceCurrency.setText(EMPTY_BALANCE);
        }

        //This sets the 24hr percentage change (rightmost value)
        double percentage = 0;
        try {
            percentage = Double.parseDouble(ticker.percentChange24h);
            color = ContextCompat.getColor(getContext(), percentage < 0 ? R.color.red : R.color.green);
            formattedPercents = (percentage < 0 ? "(" : "(+") + ticker.percentChange24h + "%)";
        } catch (Exception ex) { /* Quietly */ }

        //This sets the crypto price value (middle amount)
        String formattedValue = TickerService.getCurrencyWithoutSymbol(new BigDecimal(ticker.price).doubleValue());

        lbl = getString(R.string.token_balance, "", formattedValue);
        lbl += " " + ticker.priceSymbol;
        spannable = new SpannableString(lbl);
        spannable.setSpan(new ForegroundColorSpan(color),
                lbl.length(), lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        tokensService.addTokenValue(token.tokenInfo.chainId, token.getAddress(), fiatBalance.floatValue());
    }
}