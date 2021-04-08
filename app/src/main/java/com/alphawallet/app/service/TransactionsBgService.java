package com.alphawallet.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.service.txloader.TransactionLoadTask;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.app.viewmodel.HomeViewModelFactory;
import com.google.firebase.crashlytics.internal.settings.model.SettingsData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

/**
 * Created by jbdev on 2021.04.02..
 */
public class TransactionsBgService extends JobService {
    public static final long TWO_MIN = 120000;
    public static final String TX_BG_PREFS = "tx_bg";
    public static final String APP_ACTIVE_TIMESTAMP = "app_active";
    private static final String LAST_NOTIFICATION_TIMESTAMP = "last_noti";
    public static final String TX_BG_LOAD_ADDRESSES = "tx_addresses";
    public static final String TESTNET_ON = "testnet_on";

    private static final String MAINNET_BASE_URL = "https://ilgonexplorer.com/api";
    private static final String TESTNET_BASE_URL = "https://testnet.ilgonexplorer.com/api";
    public static final int JOB_ID = 1;


    @Override
    public boolean onStartJob(JobParameters params) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(TX_BG_PREFS, 0);
        Set<String> addresses = preferences.getStringSet(TX_BG_LOAD_ADDRESSES, new HashSet<>());
        boolean testnetOn = preferences.getBoolean(TESTNET_ON, true);
        for (String address : addresses) {
            new TransactionLoadTask(getApplicationContext(), address, false)
                    .execute(getTxSourceUrl(address, false));
            if (testnetOn) new TransactionLoadTask(getApplicationContext(), address, true)
                    .execute(getTxSourceUrl(address, true));
        }
        TransactionsBgService.startJobService(getApplicationContext(), TWO_MIN);
        return false;
    }

    private String getTxSourceUrl(String addressHash, boolean testNetwork) {
        return (testNetwork ? TESTNET_BASE_URL : MAINNET_BASE_URL) + "?module=account&action=txlist&address=" +
                addressHash +
                "&offset=3";
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void cancelRunningJobService(Context context) {
        if (TransactionsBgService.isJobServiceOn(context)){
            TransactionsBgService.cancelJobService(context);
        }
    }

    public static void startJobService(Context context, long delay) {
        JobScheduler mJobScheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (mJobScheduler == null) return;

        JobInfo.Builder builder = new JobInfo.Builder( JOB_ID,
                new ComponentName(context, TransactionsBgService.class))
                .setMinimumLatency(delay)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        if (mJobScheduler.schedule(builder.build()) <= 0) {
            Log.d("DEBUG", "Error scheduling job");
        }
    }

    public static void cancelJobService(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.cancel(JOB_ID);
    }

    public static boolean isJobServiceOn(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return false;

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                hasBeenScheduled = true;
                break;
            }
        }

        return hasBeenScheduled;
    }

    public static void onTxDataLoaded(String result, Context context, String address, boolean testnet) {
        if (result != null) {
            try {
                long lastTxTimestamp = getLastTxTimestamp(context, address, testnet);
                long lastAppActiveTimestamp = getLastAppActiveTimestamp(context);
                JSONObject transactionsObject = new JSONObject(result);
                JSONArray txArray = transactionsObject.getJSONArray("result");
                int size = txArray.length();
                ArrayList<Long> newTimestamps = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    JSONObject txObject = txArray.getJSONObject(i);
                    long timestamp = Long.parseLong(txObject.getString("timeStamp"));
                    String to = txObject.getString("to");
                    String value = txObject.getString("value");
                    if (timestamp > lastAppActiveTimestamp && timestamp > lastTxTimestamp
                            && address.equalsIgnoreCase(to)) {
                        BigDecimal valueDecimal = new BigDecimal(value);
                        if (valueDecimal.equals(BigDecimal.ZERO)) return;
                        //user received ILGONCoin
                        String coin = testnet ? "ILGT" : "ILG";
                        String valueStr = BalanceUtils.getScaledValueFixed(valueDecimal, 18, 2) + " " + coin;
                        String title = String.format(context.getResources().getString(R.string.received_noti_title), coin);
                        String content = String.format(context.getResources().getString(R.string.received_noti_content), valueStr, Utils.formatAddress(address));
                        newTimestamps.add(timestamp);
                        triggerNotification(context, title, content, 0);
                    }
                }
                if (newTimestamps.size() > 0) {
                    if (newTimestamps.size() > 1) {
                        Collections.sort(newTimestamps);
                        Collections.reverse(newTimestamps);
                    }
                    saveLastTxTimestamp(context, newTimestamps.get(0), address, testnet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setAppLastActiveTimestamp(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(TX_BG_PREFS, 0);
        long ts = System.currentTimeMillis() / 1000;
        preferences.edit()
                .putLong(APP_ACTIVE_TIMESTAMP, ts)
                .apply();
    }

    private static long getLastAppActiveTimestamp(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(TX_BG_PREFS, 0);
        return preferences.getLong(APP_ACTIVE_TIMESTAMP, 0);
    }

    private static long getLastTxTimestamp(Context context, String address, boolean testnet) {
        SharedPreferences preferences = context.getSharedPreferences(TX_BG_PREFS, 0);
        String key = LAST_NOTIFICATION_TIMESTAMP + address + (testnet ? "T" : "I");
        return preferences.getLong(key, 0);
    }

    private static void saveLastTxTimestamp(Context context, long timestamp, String address, boolean testnet) {
        SharedPreferences preferences = context.getSharedPreferences(TX_BG_PREFS, 0);
        String key = LAST_NOTIFICATION_TIMESTAMP + address + (testnet ? "T" : "I");
        preferences.edit().putLong(key, timestamp).apply();
    }

    public static boolean hasSavedWalletsData(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(TransactionsBgService.TX_BG_PREFS, 0);
        return preferences.contains(TX_BG_LOAD_ADDRESSES);
    }

    public static void saveWalletsDataForBgTxLoad(Context context, Wallet[] wallets) {
        Set<String> addresses = new HashSet<>();
        for (Wallet wallet : wallets) {
            addresses.add(wallet.address);
        }
        SharedPreferences.Editor editor = context.getSharedPreferences(TransactionsBgService.TX_BG_PREFS, 0).edit();
        editor.putStringSet(TransactionsBgService.TX_BG_LOAD_ADDRESSES, addresses)
                .apply();
    }


    public static void saveNetworkDataForBgTxLoad(Context context, boolean testnetOn){
        SharedPreferences preferences = context.getSharedPreferences(TransactionsBgService.TX_BG_PREFS, 0);
        if (preferences.contains(TESTNET_ON) && preferences.getBoolean(TESTNET_ON, false) == testnetOn) return;
        preferences.edit()
                .putBoolean(TransactionsBgService.TESTNET_ON, testnetOn)
                .apply();
    }

    public static void triggerNotification(Context context, String title, String content, int index) {
        NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification.Builder notificationBuilder =  new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_noti);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "ilgon_wallet_channel";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Ilgon Wallet App Notification Channel",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder.setChannelId(channelId);
        }

        Notification notification = notificationBuilder.build();
        notificationManager.notify(String.valueOf(index), 1, notification);
    }
}
