package com.alphawallet.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import com.alphawallet.app.di.DaggerAppComponent;
import javax.inject.Inject;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.realm.Realm;

public class App extends Application implements HasActivityInjector, HasSupportFragmentInjector, Application.ActivityLifecycleCallbacks {

	@Inject
	DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

	@Inject
	DispatchingAndroidInjector<Fragment> dispatchingAndroidSupportInjector;

	@Override
	public void onCreate() {
		super.onCreate();
        Realm.init(this);
        DaggerAppComponent
				.builder()
				.application(this)
				.build()
				.inject(this);
		registerActivityLifecycleCallbacks(this);

		// enable pin code for the application
//		LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
//		lockManager.enableAppLock(this, CustomPinActivity.class);
//		lockManager.getAppLock().setShouldShowForgot(false);
	}

	@Override
	public AndroidInjector<Activity> activityInjector() {
		return dispatchingAndroidInjector;
	}

	@Override
	public AndroidInjector<Fragment> supportFragmentInjector() {
		return dispatchingAndroidSupportInjector;
	}

	@Override
	public void onActivityStopped(Activity activity) {
		Log.i("Activity Stopped", activity.getLocalClassName());

	}

	@Override
	public void onActivityStarted(Activity activity) {
		Log.i("Activity Started", activity.getLocalClassName());

	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		Log.i("Activity SaveInstance", activity.getLocalClassName());
	}

	@Override
	public void onActivityResumed(Activity activity) {
		Log.i("Activity Resumed", activity.getLocalClassName());
	}

	@Override
	public void onActivityPaused(Activity activity) {
		Log.i("Activity Paused", activity.getLocalClassName());
	}

	@Override
	public void onActivityDestroyed(Activity activity) {
		Log.i("Activity Destroyed", activity.getLocalClassName());
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		Log.i("Activity Created", activity.getLocalClassName());
	}

}
