package com.alphawallet.token.entity;

/**
 * Created by James on 2/03/2019.
 * Stormbird in Singapore
 */
public class MagicLinkInfo
{
    //domains for DMZ
    public static final String mainnetMagicLinkDomain = "aw.app";
    private static final String classicMagicLinkDomain = "classic.aw.app";

    //Etherscan domains
    private static final String mainNetEtherscan = "https://ilgonexplorer.com/tx/";
    private static final String classicEtherscan = "https://testnet.ilgonexplorer.com/tx/";

    //network ids
    public static final int MAINNET_NETWORK_ID = 6909031;
    public static final int CLASSIC_NETWORK_ID = 1768712052;

    //network names
    private static final String ETHEREUM_NETWORK = "ILGON";
    private static final String CLASSIC_NETWORK = "ILGON Test";

    public static String getNetworkNameById(int networkId) {
        switch (networkId) {
            case MAINNET_NETWORK_ID:
                return ETHEREUM_NETWORK;
            case CLASSIC_NETWORK_ID:
                return CLASSIC_NETWORK;
            default:
                return ETHEREUM_NETWORK;
        }
    }

    public static String getMagicLinkDomainFromNetworkId(int networkId) {
        switch (networkId) {
            case MAINNET_NETWORK_ID:
            default:
                return mainnetMagicLinkDomain;
            case CLASSIC_NETWORK_ID:
                return classicMagicLinkDomain;
        }
    }

    //For testing you will not have the correct domain (localhost)
    //To test, alter the else statement to return the network you wish to test
    public static int getNetworkIdFromDomain(String domain) {
        switch(domain) {
            case mainnetMagicLinkDomain:
            default:
                return MAINNET_NETWORK_ID;
            case classicMagicLinkDomain:
                return CLASSIC_NETWORK_ID;
        }
    }

    public static String getEtherscanURLbyNetwork(int networkId) {
        switch (networkId) {
            case MAINNET_NETWORK_ID:
            default:
                return mainNetEtherscan;
            case CLASSIC_NETWORK_ID:
                return classicEtherscan;
        }
    }

    public static int identifyChainId(String link)
    {
        if (link == null || link.length() == 0) return 0;

        int chainId = 0;
        //split out the chainId from the magiclink
        int index = link.indexOf(mainnetMagicLinkDomain);
        int dSlash = link.indexOf("://");
        //try new style link
        if (index > 0 && dSlash > 0)
        {
            String domain = link.substring(dSlash+3, index + mainnetMagicLinkDomain.length());
            chainId = getNetworkIdFromDomain(domain);
        }

        return chainId;
    }

    public static String generatePrefix(int chainId)
    {
        return "https://" + getMagicLinkDomainFromNetworkId(chainId) + "/";
    }
}
