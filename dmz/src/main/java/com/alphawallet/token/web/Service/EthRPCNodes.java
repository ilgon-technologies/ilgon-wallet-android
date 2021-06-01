package com.alphawallet.token.web.Service;

import static com.alphawallet.token.entity.MagicLinkInfo.CLASSIC_NETWORK_ID;
import static com.alphawallet.token.entity.MagicLinkInfo.MAINNET_NETWORK_ID;

public class EthRPCNodes
{
    private static final String MAINNET_RPC_URL = "https://mainnet-rpc.ilgonwallet.com/";
    private static final String CLASSIC_RPC_URL = "https://testnet-rpc.ilgonwallet.com/";


    public static String getNodeURLByNetworkId(int networkId) {
        switch (networkId) {
            case MAINNET_NETWORK_ID:
                return MAINNET_RPC_URL;
            case CLASSIC_NETWORK_ID:
                return CLASSIC_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }
}
