package com.alphawallet.ethereum;

/* Weiwu 12 Jan 2020: This class eventually will replace the EthereumNetworkBase class in :app
 * one all inteface methods are implemented.
 */

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class EthereumNetworkBase { // implements EthereumNetworkRepositoryType
    public static final int MAINNET_ID = 6909031;
    public static final int CLASSIC_ID = 1768712052;

    public static final String MAINNET_RPC_URL = "https://mainnet-rpc.ilgonwallet.com/";
    public static final String CLASSIC_RPC_URL = "https://testnet-rpc.ilgonwallet.com/";

    static Map<Integer, NetworkInfo> networkMap = new LinkedHashMap<Integer, NetworkInfo>() {
        {
            put(MAINNET_ID, new NetworkInfo("ILGON", "ILG", MAINNET_RPC_URL, "https://ilgonexplorer.com/tx/",
                    MAINNET_ID, true));
            put(CLASSIC_ID, new NetworkInfo("ILGON Test", "ILGT", CLASSIC_RPC_URL, "https://testnet.ilgonexplorer.com/tx/",
                    CLASSIC_ID, true));
        }
    };

    public static NetworkInfo getNetworkByChain(int chainId) {
        return networkMap.get(chainId);
    }


    public static String getShortChainName(int chainId)
    {
        NetworkInfo info = networkMap.get(chainId);
        if (info != null)
        {
            int index = info.name.indexOf(" (Test)");
            if (index > 0) return info.name.substring(0, index);
            return info.name;
        }
        else
        {
            return networkMap.get(MAINNET_ID).name;
        }
    }

    public static String getChainSymbol(int chainId)
    {
        NetworkInfo info = networkMap.get(chainId);
        if (info != null)
        {
            return info.symbol;
        }
        else
        {
            return networkMap.get(MAINNET_ID).symbol;
        }
    }
}
