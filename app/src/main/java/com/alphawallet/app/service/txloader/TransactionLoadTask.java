package com.alphawallet.app.service.txloader;

import android.content.Context;
import android.os.AsyncTask;

import com.alphawallet.app.service.TransactionsBgService;

import java.lang.ref.WeakReference;

/**
 * Created by jbdev on 2021.04.04..
 */
public class TransactionLoadTask extends AsyncTask<String, String, String> {
    WeakReference<Context> contextRef;
    String address;
    boolean testnet;

    public TransactionLoadTask(Context context, String address, boolean testnet) {
        this.contextRef = new WeakReference<>(context);
        this.address = address;
        this.testnet = testnet;
    }

    @Override
    protected String doInBackground(String... params) {
        return HTMLReader.getStringFromUrl(params[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        Context context = contextRef.get();
        if (context != null) {
            TransactionsBgService.onTxDataLoaded(result, context, address, testnet);
        }
    }
}
