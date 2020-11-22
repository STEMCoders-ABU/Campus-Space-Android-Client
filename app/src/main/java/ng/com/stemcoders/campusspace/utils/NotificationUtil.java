package ng.com.stemcoders.campusspace.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import java.io.File;

import ng.com.stemcoders.campusspace.R;

public class NotificationUtil
{
    public static final String NOTIFICATION_CHANNEL_ID = "Campus Space Notification Channel ID";
    public static final String NOTIFICATION_CHANNEL_NAME = "Campus Space Notifications";
    public static final String NOTIFICATION_CHANNEL_DESC = "Notifications from Campus Space Application";

    public static final int REQUEST_CODE_VIEW_RESOURCE = 1002;

    public static NotificationCompat.Builder buildNotification (Context context)
    {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManagerCompat.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null)
        {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            notificationManagerCompat.createNotificationChannel(channel);
        }

        return builder;
    }

    public static NotificationCompat.Builder buildProgressNotification(Context context, String title, String text)
    {
        return buildNotification(context)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_download);
    }

    public static NotificationCompat.Builder buildResourceDownloadingNotification(Context context, String fileName)
    {
        return buildProgressNotification(context, context.getString(R.string.downloading_resource), fileName);
    }

    public static NotificationCompat.Builder buildResourceDownloadedNotification(Context context, File resourceFile)
    {
        Intent intent = FileUtil.buildViewFileIntent(context, resourceFile);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_VIEW_RESOURCE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return buildNotification(context)
                .setContentTitle(context.getString(R.string.downloaded))
                .setContentText(resourceFile.getName())
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
    }
}




























