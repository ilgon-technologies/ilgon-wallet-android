package com.alphawallet.app.repository;

/* Please don't add import android at this point. Later this file will be shared
 * between projects including non-Android projects */

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ChainSpec;

import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;

public abstract class EthereumNetworkBase implements EthereumNetworkRepositoryType
{
    private static final String DEFAULT_HOMEPAGE = "https://ilgonwallet.com/";
    /* constructing URLs from BuildConfig. In the below area you will see hardcoded key like da3717...
       These hardcoded keys are fallbacks used by AlphaWallet forks.

       Also note: If you are running your own node and wish to use that; currently it must be hardcoded here
       If you wish your node to be the primary node that AW checks then replace the relevant ..._RPC_URL below
       If you wish your node to be the fallback, tried in case the primary times out then add/replace in ..._FALLBACK_RPC_URL list
     */

    static {
        System.loadLibrary("keys");
    }

    public static native String getAmberDataKey();
    public static native String getInfuraKey();
    public static native String getSecondaryInfuraKey();

    public static final String MAINNET_RPC_URL = BuildConfig.MAIN_RPC_URL;
    public static final String MAINNET_FALLBACK_RPC_URL = MAINNET_RPC_URL;
    public static final String CLASSIC_RPC_URL = BuildConfig.SECONDARY_RPC_URL;

    public static final int MAINNET_ID = BuildConfig.MAIN_CHAIN_ID;
    public static final int CLASSIC_ID = BuildConfig.SECONDARY_CHAIN_ID;

    final Map<Integer, NetworkInfo> networkMap;

    final NetworkInfo[] NETWORKS;
    static final NetworkInfo[] DEFAULT_NETWORKS = new NetworkInfo[] {
            new NetworkInfo(C.ETHEREUM_NETWORK_NAME, C.ETH_SYMBOL,
                    MAINNET_RPC_URL,
                    BuildConfig.ETHERSCAN_URL_MAIN,MAINNET_ID, true,
                    MAINNET_FALLBACK_RPC_URL,
                    BuildConfig.MAIN_TX_URL),
            new NetworkInfo(C.CLASSIC_NETWORK_NAME, C.Secondary_SYMBOL,
                    CLASSIC_RPC_URL,
                    BuildConfig.ETHERSCAN_URL_SECONDRARY,CLASSIC_ID, true,
                    CLASSIC_RPC_URL, BuildConfig.SECONDARY_TX_URL)


    };

    final PreferenceRepositoryType preferences;
    NetworkInfo defaultNetwork;
    private final Set<OnNetworkChangeListener> onNetworkChangedListeners = new HashSet<>();

    EthereumNetworkBase(PreferenceRepositoryType preferenceRepository, NetworkInfo[] additionalNetworks, boolean useTestNets)
    {
        this.preferences = preferenceRepository;

        /* merging static compile time network list with runtime network list */
        List<NetworkInfo> networks = new ArrayList<>();

        /* the order is passed to the uesr interface. So if a user has a token on one
         * of the additionalNetworks, the same token on DEFAULT_NETWORKS, and on a few
         * test nets, they are displayed by that order.
         */
        addNetworks(additionalNetworks, networks, true);
        addNetworks(DEFAULT_NETWORKS, networks, true);
        addNetworks(additionalNetworks, networks, false);
        if (useTestNets) addNetworks(DEFAULT_NETWORKS, networks, false);

        /* then store the result list in a network variable */
        NETWORKS = networks.toArray(new NetworkInfo[0]);

        defaultNetwork = getByName(preferences.getDefaultNetwork());
        if (defaultNetwork == null) {
            defaultNetwork = NETWORKS[0];
        }

        networkMap = new ConcurrentHashMap<>();
        for (NetworkInfo network : NETWORKS)
        {
            networkMap.put(network.chainId, network);
        }
    }

    private void addNetworks(NetworkInfo[] networks, List<NetworkInfo> result, boolean withValue)
    {
        for (NetworkInfo network : networks)
        {
            if (EthereumNetworkRepository.hasRealValue(network.chainId) == withValue) result.add(network);
        }
    }

    private NetworkInfo getByName(String name) {
        if (name != null && !name.equals("")) {
            for (NetworkInfo NETWORK : NETWORKS) {
                if (name.equals(NETWORK.name)) {
                    return NETWORK;
                }
            }
        }
        return null;
    }

    @Override
    public String getNameById(int id)
    {
        if (networkMap.containsKey(id)) return networkMap.get(id).name;
        else return "Unknown: " + id;
    }

    @Override
    public NetworkInfo getDefaultNetwork() {
        return defaultNetwork;
    }

    @Override
    public NetworkInfo getNetworkByChain(int chainId)
    {
        return networkMap.get(chainId);
    }

    // fetches the last transaction nonce; if it's identical to the last used one then increment by one
    // to ensure we don't get transaction replacement
    @Override
    public Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress)
    {
        return Single.fromCallable(() -> {
            try
            {
                EthGetTransactionCount ethGetTransactionCount = web3j
                        .ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                        .send();
                return ethGetTransactionCount.getTransactionCount();
            }
            catch (Exception e)
            {
                return BigInteger.ZERO;
            }
        });
    }

    @Override
    public List<Integer> getFilterNetworkList()
    {
        List<Integer> networkIds = EthereumNetworkRepository.addDefaultNetworks();
        String filterList = preferences.getNetworkFilterList();
        if (filterList.length() > 0)
        {
            networkIds = Utils.intListToArray(filterList);
        }

        return networkIds;
    }

    @Override
    public void setFilterNetworkList(int[] networkList)
    {
        String store = Utils.intArrayToString(networkList);
        preferences.setNetworkFilterList(store.toString());
    }

    @Override
    public void setDefaultNetworkInfo(NetworkInfo networkInfo) {
        defaultNetwork = networkInfo;
        preferences.setDefaultNetwork(defaultNetwork.name);

        for (OnNetworkChangeListener listener : onNetworkChangedListeners) {
            listener.onNetworkChanged(networkInfo);
        }
    }

    @Override
    public NetworkInfo[] getAvailableNetworkList() {
        return NETWORKS;
    }

    @Override
    public void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged) {
        onNetworkChangedListeners.add(onNetworkChanged);
    }

    public static boolean hasRealValue(int chainId)
    {
        switch (chainId)
        {
            case EthereumNetworkRepository.MAINNET_ID:
                return true;
            case EthereumNetworkRepository.CLASSIC_ID:
            default:
                return false;
        }
    }

    public static String getSecondaryNodeURL(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return MAINNET_FALLBACK_RPC_URL;
            case CLASSIC_ID:
                return CLASSIC_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }

    public static int getChainLogo(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return R.drawable.ic_dragon;
            case CLASSIC_ID:
                return R.drawable.ic_dragon;
            default:
                return R.drawable.ic_ethereum_logo;
        }
    }

    public static String getNodeURLByNetworkId(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return MAINNET_RPC_URL;
            case CLASSIC_ID:
                return CLASSIC_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }

    /**
     * This is used so as not to leak API credentials to web3; XInfuraAPI is the backup API key checked into github
     * @param networkId
     * @return
     */
    public static String getDefaultNodeURL(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return "https://mainnet.infura.io/v3/" + BuildConfig.XInfuraAPI;
            default:
                return getSecondaryNodeURL(networkId);
        }
    }


    public static String getEtherscanURLbyNetwork(int networkId)
    {
        switch (networkId)
        {
            case MAINNET_ID:
                return BuildConfig.ETHERSCAN_URL_MAIN;
            case CLASSIC_ID:
                return BuildConfig.ETHERSCAN_URL_SECONDRARY;
            default:
                return BuildConfig.ETHERSCAN_URL_MAIN;
        }
    }

    public static boolean hasGasOverride(int chainId)
    {
        return false;
    }

    public static BigInteger gasOverrideValue(int chainId)
    {
        return BigInteger.valueOf(1);
    }

    public static List<ChainSpec> extraChains()
    {
        return null;
    }

    public static void addRequiredCredentials(int chainId, HttpService publicNodeService)
    {

    }

    public static List<Integer> addDefaultNetworks()
    {
        return new ArrayList<>(Collections.singletonList(EthereumNetworkRepository.MAINNET_ID));
    }

    public static ContractLocator getOverrideToken()
    {
        return new ContractLocator("", EthereumNetworkRepository.MAINNET_ID, ContractType.ETHEREUM);
    }

    public static boolean isPriorityToken(Token token)
    {
        return false;
    }

    public static int getPriorityOverride(Token token)
    {
        if (token.isEthereum()) return token.tokenInfo.chainId + 1;
        else return 0;
    }

    public static boolean showNetworkFilters() { return true; }

    public static int decimalOverride(String address, int chainId)
    {
        return 0;
    }

    public static String defaultDapp()
    {
        return DEFAULT_HOMEPAGE;
    }

    public Token getBlankOverrideToken(NetworkInfo networkInfo)
    {
        return createCurrencyToken(networkInfo);
    }

    public Single<Token[]> getBlankOverrideTokens(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            if (getBlankOverrideToken() == null)
            {
                return new Token[0];
            }
            else
            {
                Token[] tokens = new Token[1];
                tokens[0] = getBlankOverrideToken();
                tokens[0].setTokenWallet(wallet.address);
                return tokens;
            }
        });
    }

    private static Token createCurrencyToken(NetworkInfo network)
    {
        TokenInfo tokenInfo = new TokenInfo(Address.DEFAULT.toString(), network.name, network.symbol, 18, true, network.chainId);
        BigDecimal balance = BigDecimal.ZERO;
        Token eth = new Token(tokenInfo, balance, 0, network.getShortName(), ContractType.ETHEREUM); //create with zero time index to ensure it's updated immediately
        eth.setTokenWallet(Address.DEFAULT.toString());
        eth.setIsEthereum();
        eth.pendingBalance = balance;
        return eth;
    }

    public Token getBlankOverrideToken()
    {
        return null;
    }
}
