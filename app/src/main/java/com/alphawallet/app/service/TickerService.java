package com.alphawallet.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.token.entity.EthereumReadBuffer;
import com.alphawallet.token.tools.Numeric;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TickerService
{
    private static final int UPDATE_TICKER_CYCLE = 1; //1 Minute
    public static final String ILGON_PRICES_URL = "https://priceapi.ilgonwallet.com/prices";
    public static final String ILGON_CURRENCY_RATES_URL = "https://priceapi.ilgonwallet.com/prices?module=fxrates";
    public static final String ILGON_PRICE_JSON_ROOT = "data";
    public static final String ILGON_PRICE_USD = "ILG_USD";

    private static final String MEDIANIZER = "0x729D19f657BD0614b4985Cf1D82531c67569197B";

    public static final long TICKER_TIMEOUT = DateUtils.HOUR_IN_MILLIS; //remove ticker if not seen in one hour

    private final OkHttpClient httpClient;
    private final Context context;
    private final TokenLocalSource localSource;
    private Disposable tickerUpdateTimer;
    private double currentConversionRate = 0.0;
    private static String currentCurrencySymbolTxt = "USD";
    private static String currentCurrencySymbol = "$";
    private TokenTicker ilgonTicker;

    public static native String getCMCKey();
    public static native String getAmberDataKey();

    static {
        System.loadLibrary("keys");
    }

    public TickerService(OkHttpClient httpClient, Gson gson, Context ctx, TokenLocalSource localSource)
    {
        this.httpClient = httpClient;
        //this.gson = gson;
        this.context = ctx;
        this.localSource = localSource;

        initCurrency();
    }

    public void updateTickers()
    {
        if (tickerUpdateTimer != null && !tickerUpdateTimer.isDisposed()) tickerUpdateTimer.dispose();

        tickerUpdateTimer = Observable.interval(0, UPDATE_TICKER_CYCLE, TimeUnit.MINUTES)
                    .doOnNext(l -> tickerUpdate())
                    .subscribe();
    }

    private void tickerUpdate()
    {
        updateCurrencyConversion()
                .flatMap(this::fetchIlgonTicker)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkTickers, this::onTickersError).isDisposed();
    }

    public TokenTicker getIlgonTicker() {
        return ilgonTicker;
    }

    private Single<Integer> fetchIlgonTicker(double conversionRate)
    {
        currentConversionRate = conversionRate;
        return Single.fromCallable(
                () -> {
                    try
                    {
                        Request request = new Request.Builder()
                                .url(ILGON_PRICES_URL)
                                .get()
                                .build();
                        okhttp3.Response response = httpClient.newCall(request)
                                .execute();
                        if (response.code() / 200 == 1)
                        {
                            String result = response.body()
                                    .string();
                            JSONObject stateData = new JSONObject(result);
                            JSONObject data = stateData.getJSONObject(ILGON_PRICE_JSON_ROOT);
                            ilgonTicker = decodeIlgonTicker(data);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return 1;
                }
        );

    }

    private TokenTicker decodeIlgonTicker(JSONObject obj)
    {
        TokenTicker ticker;
        try
        {
            //double usdPrice = eth.getDouble("ethusd");// getString("price");
            //String localePrice = String.valueOf(usdPrice * currentConversionRate);
           // ticker = new TokenTicker(localePrice, "0.00", currentCurrencySymbolTxt, "", System.currentTimeMillis());

            String usdPriceStr = obj.getString(ILGON_PRICE_USD);
            double usdPrice = Double.parseDouble(usdPriceStr);
            String localePrice = String.valueOf(usdPrice * currentConversionRate);
            ticker = new TokenTicker(localePrice, "0.00", currentCurrencySymbolTxt, "", System.currentTimeMillis());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ticker = new TokenTicker();
        }

        return ticker;
    }

    private Single<Double> updateCurrencyConversion()
    {
        initCurrency();
        return convertPair("USD", currentCurrencySymbolTxt);
    }

    private void checkTickers(int tickerSize)
    {
        System.out.println("Tickers received: " + tickerSize);
        Map<Integer, TokenTicker> map = new HashMap<>();
        map.put(MAINNET_ID, ilgonTicker);
        localSource.updateEthTickers(map);
        localSource.removeOutdatedTickers();
    }

    public TokenTicker getEthTicker(int chainId)
    {
        if (chainId == MAINNET_ID) {
            return ilgonTicker;
        } else {
            return null;
        }
    }

    public Single<Token[]> getTokensOnNetwork(NetworkInfo info, String address, TokensService tokensService)
    {
        //TODO: find tokens on other networks
        String netName = "ethereum-mainnet";
        if (info.chainId != MAINNET_ID) return Single.fromCallable(() -> { return new Token[0]; });
        List<Token> tokenList = new ArrayList<>();
        final String keyAPI = getAmberDataKey();
        return Single.fromCallable(() -> {
            try
            {
                String url = "https://web3api.io/api/v2/addresses/" + address + "/balances";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("x-api-key", keyAPI)
                        .addHeader("x-amberdata-blockchain-id", netName)
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();
                if (response.code() / 200 == 1)
                {
                    String result = response.body().string();
                    handleTokenList(info, tokenList, result, address, tokensService);
                }
            }
            catch (InterruptedIOException e)
            {
                // silent fail, expected
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return tokenList.toArray(new Token[0]);
        });
    }

    private void handleTokenList(NetworkInfo network, List<Token> tokenList, String result, String currentAddress, TokensService tokensService)
    {
        if (result.contains("NOTOK")) return;

        try
        {
            JSONObject   json    = new JSONObject(result);
            JSONObject   res     = json.getJSONObject("payload");
            JSONArray    tokens  = res.getJSONArray("tokens");

            TokenFactory tf      = new TokenFactory();

            for (int i = 0; i < tokens.length(); i++)
            {
                ContractType cType = ContractType.ERC20;
                JSONObject t          = (JSONObject) tokens.get(i);
                String     balanceStr = t.getString("amount");
                if (balanceStr.length() == 0 || balanceStr.equals("0")) continue;
                String decimalsStr   = t.getString("decimals");
                int    decimals      = (decimalsStr.length() > 0) ? Integer.parseInt(decimalsStr) : 0;
                Token  existingToken = tokensService.getToken(network.chainId, t.getString("address"));
                if (existingToken == null)
                {
                    cType = ContractType.OTHER; //if we haven't seen this token before mark as needing contract type check
                }
                else if (!existingToken.isERC20() && existingToken.getInterfaceSpec() != ContractType.OTHER) //allow tokens still classified as 'OTHER' to be updated.
                {                                                                                            //we may be able to categorise them later
                    continue;
                }
                else if (isDynamicBalanceToken(existingToken))
                {
                    continue;
                }

                TokenInfo info = new TokenInfo(t.getString("address"), t.getString("name"), t.getString("symbol"), decimals, true, network.chainId);
                //now create token with balance info, only for ERC20 for now
                BigDecimal balance  = new BigDecimal(balanceStr);
                Token      newToken = tf.createToken(info, balance, null, System.currentTimeMillis(), cType, network.getShortName(), System.currentTimeMillis());
                newToken.setTokenWallet(currentAddress);
                tokenList.add(newToken);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private boolean isDynamicBalanceToken(Token existingToken)
    {
        for (String dynamicTokenAddress : DYNAMIC_BALANCE_TOKENS)
        {
            if (existingToken.getAddress().equalsIgnoreCase(dynamicTokenAddress)) return true;
        }

        return false;
    }

    public Single<Double> convertPair(String currency1, String currency2)
    {
        return Single.fromCallable(() -> {
            if (currency1 == null || currency2 == null || currency1.equals(currency2)) return (Double)1.0;
            okhttp3.Response response = null;

            try
            {
                Request request = new Request.Builder()
                        .url(ILGON_CURRENCY_RATES_URL)
                        .addHeader("Connection","close")
                        .get()
                        .build();
                response = httpClient.newCall(request)
                        .execute();

                int resultCode = response.code();
                if ((resultCode / 100) == 2 && response.body() != null)
                {
                    String responseBody = response.body().string();
                    JSONObject rates = new JSONObject(responseBody).getJSONObject("data").getJSONObject("rates");
                    return Double.parseDouble(rates.getString(currency2));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (response != null) response.close();
            }

            return (Double)1.0;
        });
    }

    private String callSmartContractFunction(Web3j web3j,
                                             Function function, String contractAddress) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        try
        {
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(ZERO_ADDRESS, contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (IOException e)
        {
            //Connection error. Use cached value
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static Function read() {
        return new Function(
                "read",
                Arrays.<Type>asList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private static Function getTickers() {
        return new Function(
                "getTickers",
                Arrays.<Type>asList(),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    private Single<TokenTicker> getSigmaTicker(double rate)
    {
        return Single.fromCallable(() -> {
            String percentageChange = "0.00";
            double conversion = (1.0 / 13.7603) * rate; //13.7603 ATS = 1 EUR
            String price_usd = String.valueOf(conversion);
            String image = "https://artis.eco/i/favicon.png";
            return new TokenTicker(price_usd, percentageChange, currentCurrencySymbolTxt, image, System.currentTimeMillis());
        });
    }

    private void onTickersError(Throwable throwable)
    {
        throwable.printStackTrace();
    }




    private void initCurrency()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        currentCurrencySymbolTxt = pref.getString("currency_locale", "USD");
        currentCurrencySymbol = pref.getString("currency_symbol", "$");
    }
    /**
     * Returns the current ISO currency string eg EUR, AUD etc.
     * @return 3 character currency ISO text
     */
    //TODO: Refactor this as required
    public static String getCurrencyString(double price)
    {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        df.setRoundingMode(RoundingMode.CEILING);
        return currentCurrencySymbol + df.format(price);
    }

    public static String getCurrencyWithoutSymbol(double price)
    {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        df.setRoundingMode(RoundingMode.DOWN);
        return df.format(price);
    }

    public static String getCurrencySymbolTxt()
    {
        return currentCurrencySymbolTxt;
    }

    public static String getCurrencySymbol()
    {
        return currentCurrencySymbol;
    }

    public double getCurrentConversionRate()
    {
        /*if (ilgonTicker == null) {
            return 0;
        } else {
            return Double.parseDouble(ilgonTicker.price);
        }*/
        return currentConversionRate;
    }

    // These ERC20 can't have balance updated from the market service
    private static final String[] DYNAMIC_BALANCE_TOKENS = {
            "0x3a3A65aAb0dd2A17E3F1947bA16138cd37d08c04", //AAVE
            "0x71fc860f7d3a592a4a98740e39db31d25db65ae8"  //AAVE
    };
}
