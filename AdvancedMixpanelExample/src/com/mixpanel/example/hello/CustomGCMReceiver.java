package com.mixpanel.example.hello;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class CustomGCMReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if ("com.google.android.c2dm.intent.RECEIVE".equals(action)) {
            // mp_message will always exist and contain the message you entered
            // for the push notification body in the builder.
            Bundle extras = intent.getExtras();
            final String message = extras.getString("mp_message");
            if (message == null) {
                return;
            }

            // If you entered any JSON in the custom data box in the builder, these keys will
            // also exist in the intent extras. In this example, we assume that there exists a
            // custom key named "title" that we will use as the push notification title.
            CharSequence notificationTitle = extras.getString("title", "");
            int notificationIcon = android.R.drawable.sym_def_app_icon;

            final PackageManager manager = context.getPackageManager();
            final Intent appIntent = manager.getLaunchIntentForPackage(context.getPackageName());
            try {
                final ApplicationInfo appInfo = manager.getApplicationInfo(context.getPackageName(), 0);
                if (notificationTitle.equals("")) {
                    notificationTitle = manager.getApplicationLabel(appInfo);
                }
                notificationIcon = appInfo.icon;
            } catch (final PackageManager.NameNotFoundException e) {
                // In this case, use a blank title and default icon
            }

            final PendingIntent contentIntent = PendingIntent.getActivity(
                    context.getApplicationContext(),
                    0,
                    appIntent, // add this pass null to intent
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            final NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            final Notification.Builder builder = new Notification.Builder(context).
                    setSmallIcon(notificationIcon).
                    setTicker(message).
                    setWhen(System.currentTimeMillis()).
                    setContentTitle(notificationTitle).
                    setContentText(message).
                    setContentIntent(contentIntent);
            Notification n = builder.build();

            n.flags |= Notification.FLAG_AUTO_CANCEL;
            nm.notify(0, n);
        }
    }
}