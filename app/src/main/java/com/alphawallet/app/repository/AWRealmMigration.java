package com.alphawallet.app.repository;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;


/**
 * Important! If you make a change to any of the realm objects (eg RealmToken) you need to perform a DataBase migration
 * NB: Ensure the primitive types match up. EG if you used long timeObject; in the DataBase class then use long.class here, don't use Long.class!
 */
public class AWRealmMigration implements RealmMigration
{
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion)
    {
        RealmSchema schema = realm.getSchema();
        if (oldVersion == 4)
        {
            RealmObjectSchema realmTicker = schema.get("RealmTokenTicker");
            if (!realmTicker.hasField("currencySymbol")) realmTicker.addField("currencySymbol", String.class);
            oldVersion++;
        }
        //Note: these version updates drop through; eg if oldVersion was 4, then the above code AND this code will execute
        if (oldVersion == 5)
        {
            RealmObjectSchema realmToken = schema.get("RealmToken");
            if (!realmToken.hasField("lastTxTime")) realmToken.addField("lastTxTime", long.class); //add the last transaction update time, used to check tokenscript cached result validity
            oldVersion++;
        }

        //Version 6
        if (oldVersion == 6)
        {
            schema.create("RealmCertificateData")
                    .addField("instanceKey", String.class, FieldAttribute.PRIMARY_KEY)
                    .addField("result", String.class)
                    .addField("subject", String.class)
                    .addField("keyName", String.class)
                    .addField("keyType", String.class)
                    .addField("issuer", String.class)
                    .addField("certificateName", String.class)
                    .addField("type", int.class);
            oldVersion++;
        }

        if (oldVersion == 7)
        {
            RealmObjectSchema realmData = schema.get("RealmAuxData");
            if (realmData == null)
            {
                schema.create("RealmAuxData")
                        .addField("instanceKey", String.class, FieldAttribute.PRIMARY_KEY)
                        .addField("chainId", int.class)
                        .addField("tokenAddress", String.class)
                        .addField("tokenId", String.class)
                        .addField("functionId", String.class)
                        .addField("result", String.class)
                        .addField("resultTime", long.class)
                        .addField("resultReceivedTime", long.class);
            }
            else
            {
                if (!realmData.hasField("tokenAddress"))
                    realmData.addField("tokenAddress", String.class);
                if (!realmData.hasField("resultReceivedTime"))
                    realmData.addField("resultReceivedTime", long.class);
            }

            realmData = schema.get("RealmKeyType");
            if (realmData == null)
            {
                schema.create("RealmKeyType")
                        .addField("address", String.class, FieldAttribute.PRIMARY_KEY)
                        .addField("type", byte.class)
                        .addField("authLevel", String.class)
                        .addField("lastBackup", long.class)
                        .addField("dateAdded", long.class)
                        .addField("modulus", String.class);
            }

            realmData = schema.get("RealmWalletData");
            if (realmData == null)
            {
                schema.create("RealmWalletData")
                        .addField("address", String.class, FieldAttribute.PRIMARY_KEY)
                        .addField("ENSName", String.class)
                        .addField("balance", String.class)
                        .addField("name", String.class)
                        .addField("lastWarning", long.class);
            }

            oldVersion += 2;
        }
        else if (oldVersion == 8)
        {
            RealmObjectSchema realmData = schema.get("RealmAuxData");
            if (!realmData.hasField("resultReceivedTime")) realmData.addField("resultReceivedTime", long.class)
                                    .transform(obj -> obj.set("resultReceivedTime", 0L));
            oldVersion++;
        }

        if (oldVersion == 9)
        {
            RealmObjectSchema realmToken = schema.get("RealmToken");
            if (!realmToken.hasField("earliestTxBlock")) realmToken.addField("earliestTxBlock", long.class);
            oldVersion++;
        }

        if (oldVersion == 10)
        {
            schema.create("RealmTokenScriptData")
                    .addField("instanceKey", String.class, FieldAttribute.PRIMARY_KEY)
                    .addField("fileHash", String.class)
                    .addField("filePath", String.class)
                    .addField("names", String.class)
                    .addField("viewList", String.class);
            oldVersion++;
        }

        if (oldVersion == 11)
        {
            RealmObjectSchema realmToken = schema.get("RealmTokenScriptData");
            if (!realmToken.hasField("hasEvents")) realmToken.addField("hasEvents", boolean.class);
            oldVersion++;
        }

        if (oldVersion == 12)
        {
            schema.create("RealmWCSession")
                    .addField("sessionId", String.class, FieldAttribute.PRIMARY_KEY)
                    .addField("peerId", String.class)
                    .addField("sessionData", String.class)
                    .addField("remotePeerData", String.class)
                    .addField("remotePeerId", String.class)
                    .addField("usageCount", int.class)
                    .addField("lastUsageTime", long.class)
                    .addField("walletAccount", String.class);
            oldVersion++;
        }

        if (oldVersion == 13)
        {
            schema.create("RealmWCSignElement")
                    .addField("sessionId", String.class)
                    .addField("signMessage", byte[].class)
                    .addField("signType", String.class)
                    .addField("signTime", long.class);
            oldVersion++;
        }

        if (oldVersion == 14)
        {
            RealmObjectSchema realmToken = schema.get("RealmToken");
            if (!realmToken.hasField("visibilityChanged")) realmToken.addField("visibilityChanged", boolean.class);
            oldVersion++;
        }

        if (oldVersion == 15)
        {
            RealmObjectSchema realmGasSpread = schema.get("RealmGasSpread");
            if (realmGasSpread == null) {
                schema.create("RealmGasSpread")
                        .addField("timeStamp", long.class, FieldAttribute.PRIMARY_KEY)
                        .addField("chainId", int.class)
                        .addField("rapid", String.class)
                        .addField("fast", String.class)
                        .addField("standard", String.class)
                        .addField("slow", String.class);
            }
            oldVersion++;
        }

        if (oldVersion == 16)
        {
            RealmObjectSchema realmToken = schema.get("RealmTransaction");
            if (!realmToken.hasField("expectedCompletion")) realmToken.addField("expectedCompletion", long.class);
            if (realmToken.hasField("token")) realmToken.removeField("token");
            oldVersion++;
        }

        if (oldVersion == 17)
        {
            RealmObjectSchema realmData = schema.get("RealmTransfer");
            if (realmData == null)
            {
                schema.create("RealmTransfer")
                        .addField("hash", String.class)
                        .addField("tokenAddress", String.class)
                        .addField("eventName", String.class)
                        .addField("transferDetail", String.class);
            }
            oldVersion++;
        }

        if (oldVersion == 18)
        {
            RealmObjectSchema realmData = schema.get("RealmTransaction");
            if (realmData != null && realmData.hasField("operations")) realmData.removeField("operations");
            oldVersion++;
        }

        if (oldVersion == 19)
        {
            RealmObjectSchema realmData = schema.get("RealmTransactionOperation");
            if (realmData != null)
            {
                realmData.removeField("viewType");
                realmData.removeField("from");
                realmData.removeField("to");
                realmData.removeField("value");
                realmData.removeField("contract");
            }

            realmData = schema.get("RealmTransactionContract");
            if (realmData != null)
            {
                realmData.removeField("name");
                realmData.removeField("totalSupply");
                realmData.removeField("decimals");
                realmData.removeField("symbol");
                realmData.removeField("balance");
                realmData.removeField("operation");
                realmData.removeField("otherParty");
                realmData.removeField("indices");
                realmData.removeField("type");
                realmData.removeField("contractType");
            }

            oldVersion++;
        }

        if (oldVersion == 20)
        {
            if (schema.get("RealmTransactionOperation") != null) {
                schema.remove("RealmTransactionOperation");
            }
            if (schema.get("RealmTransactionContract") != null) {
                schema.remove("RealmTransactionContract");
            }
            oldVersion++;
        }

        if (oldVersion == 21)
        {
            RealmObjectSchema realmData = schema.get("RealmWCSession");
            if (realmData != null && !realmData.hasField("chainId")) realmData.addField("chainId", int.class);
            oldVersion++;
        }

        if (oldVersion == 22)
        {
            RealmObjectSchema realmToken = schema.get("RealmToken");
            if (realmToken != null && !realmToken.hasField("stakingBalance")) realmToken.addField("stakingBalance", String.class);
            if (realmToken != null && !realmToken.hasField("compensationBalance")) realmToken.addField("compensationBalance", String.class);
            oldVersion++;
        }
    }

    @Override
    public int hashCode()
    {
        return AWRealmMigration.class.hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof AWRealmMigration;
    }
}