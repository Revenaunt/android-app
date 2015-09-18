package org.tndata.android.compass.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import org.tndata.android.compass.R;
import org.tndata.android.compass.activity.ActionActivity;
import org.tndata.android.compass.activity.BehaviorProgressActivity;
import org.tndata.android.compass.activity.PackageEnrollmentActivity;
import org.tndata.android.compass.activity.SnoozeActivity;
import org.tndata.android.compass.model.Reminder;
import org.tndata.android.compass.service.CompleteActionService;


/**
 * Utility class containing all the required methods to generate notifications.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public final class NotificationUtil{
    public static final String NOTIFICATION_TYPE_ACTION_TAG = "org.tndata.compass.ActionNotification";
    public static final String NOTIFICATION_TYPE_BEHAVIOR_TAG = "org.tndata.compass.BehaviorNotification";
    public static final String NOTIFICATION_TYPE_ENROLLMENT_TAG = "org.tndata.compass.EnrollmentNotification";

    //A behavior notification will always replace a previous one, that's why the (Tag, Id) tuple
    //  needs to be fixed
    public static final int NOTIFICATION_TYPE_BEHAVIOR_ID = 1;


    /**
     * Constructor. Should never be called. Period.
     */
    public NotificationUtil(){
        throw new RuntimeException(getClass().toString() + " is not to be instantiated");
    }

    /**
     * Private getter for a generic builder for a notification with a title and a message.
     *
     * @param context the context.
     * @param title the title of the notification.
     * @param message the message of the notification.
     * @return the builder.
     */
    private static NotificationCompat.Builder getBuilder(Context context, String title, String message){
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_action_compass_white)
                .setLargeIcon(icon)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setSound(sound)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
    }

    /**
     * Creates a behavior notification.
     *
     * @param context the context.
     * @param title the notification's title.
     * @param message the notification's message.
     */
    public static void generateBehaviorNotification(Context context, String title, String message){
        Intent intent = new Intent(context, BehaviorProgressActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = getBuilder(context, title, message)
                .setContentIntent(contentIntent)
                .build();

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_TYPE_BEHAVIOR_TAG, NOTIFICATION_TYPE_BEHAVIOR_ID, notification);
    }

    /**
     * Creates an action notification.
     *
     * @param context the context.
     * @param notificationId the id of the notification as given by the API.
     * @param title the title of the notification.
     * @param message the message of the notification.
     * @param actionId the id of the action enclosed in this notification.
     * @param userMappingId the mapping id of the action for the user.
     */
    public static void generateActionNotification(Context context, int notificationId, String title,
                                                  String message, int actionId, int userMappingId){

        Intent intent = new Intent(context, ActionActivity.class)
                .putExtra(ActionActivity.ACTION_ID_KEY, actionId);

        PendingIntent contentIntent = PendingIntent.getActivity(context,
                (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Reminder reminder = new Reminder(notificationId, -1, title, message, actionId, userMappingId);

        Intent snoozeIntent = new Intent(context, SnoozeActivity.class)
                .putExtra(SnoozeActivity.REMINDER_KEY, reminder)
                .putExtra(SnoozeActivity.PUSH_NOTIFICATION_ID_KEY, actionId)
                .putExtra(SnoozeActivity.NOTIFICATION_ID_KEY, notificationId);

        PendingIntent snoozePendingIntent = PendingIntent.getActivity(context,
                (int)System.currentTimeMillis(), snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String later = context.getString(R.string.later_title);

        Intent didItIntent = new Intent(context, CompleteActionService.class)
                .putExtra(CompleteActionService.PUSH_NOTIFICATION_ID_KEY, actionId)
                .putExtra(CompleteActionService.ACTION_MAPPING_ID_KEY, userMappingId);

        PendingIntent didItPendingIntent = PendingIntent.getService(context,
                (int)System.currentTimeMillis(), didItIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String didIt = context.getString(R.string.did_it_title);

        Notification notification = getBuilder(context, title, message)
                .addAction(R.drawable.ic_snooze, later, snoozePendingIntent)
                .addAction(R.drawable.ic_check, didIt, didItPendingIntent)
                .setContentIntent(contentIntent)
                .build();

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_TYPE_ACTION_TAG, actionId, notification);
    }

    /**
     * Creates a package enrollment notification.
     *
     * @param context an instance of the context.
     * @param packageId the package id.
     * @param title the title of the notification.
     * @param message the message of the notification.
     */
    public static void generateEnrollmentNotification(Context context, int packageId, String title,
                                                      String message){

        Intent intent = new Intent(context, PackageEnrollmentActivity.class)
                .putExtra(PackageEnrollmentActivity.PACKAGE_ID_KEY, packageId);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = getBuilder(context, title, message)
                .setContentIntent(contentIntent)
                .setAutoCancel(false)
                .build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_TYPE_ENROLLMENT_TAG, packageId, notification);
    }
}