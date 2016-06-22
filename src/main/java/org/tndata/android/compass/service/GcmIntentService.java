package org.tndata.android.compass.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;
import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.model.Badge;
import org.tndata.android.compass.receiver.GcmBroadcastReceiver;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.NotificationUtil;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * <p/>
 * NOTE: Messages received from GCM will have a format like the following:
 * <p/>
 * {
 * "title":"Demo"
 * "message":"Don't forget to review your Notifications for today",
 * "object_id":32,
 * "object_type":"action",  // an Action
 * }
 */
public class GcmIntentService extends IntentService{
    private static final String TAG = "GcmIntentService";

    public static final String MESSAGE_TYPE_ACTION = "action";
    public static final String MESSAGE_TYPE_CUSTOM_ACTION = "customaction";
    public static final String MESSAGE_TYPE_ENROLLMENT = "package enrollment";
    public static final String MESSAGE_TYPE_CHECK_IN = "checkin";
    public static final String MESSAGE_TYPE_AWARD = "award";
    public static final String MESSAGE_TYPE_BADGE = "badge";


    public GcmIntentService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        String token = ((CompassApplication)getApplication()).getToken();
        if (token != null && !token.equals("")){
            Bundle extras = intent.getExtras();
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            //The getMessageType() intent parameter must be the intent you received
            //  in your BroadcastReceiver
            String messageType = gcm.getMessageType(intent);

            if (extras != null && !extras.isEmpty()){  //Has effect of un-parcelling Bundle
                Log.d(TAG, "GCM message: " + extras.get("message"));
                /*
                 * Filter messages based on message type. Since it is likely that GCM
                 * will be extended in the future with new message types, just ignore
                 * any message types you're not interested in, or that you don't
                 * recognize.
                 */
                if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)){
                    Log.d(TAG, "Send error: " + extras.toString());
                }
                else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)){
                    Log.d(TAG, "Deleted messages on server: " + extras.toString());
                }
                else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)){
                    try{
                        JSONObject jsonObject = new JSONObject(extras.getString("message"));
                        if (jsonObject.optBoolean("production") == !API.STAGING){
                            sendNotification(
                                    jsonObject.optString("id"),
                                    jsonObject.optString("message"),
                                    jsonObject.optString("title"),
                                    jsonObject.optString("object_type"),
                                    jsonObject.optString("object_id"),
                                    jsonObject.optString("user_mapping_id"),
                                    jsonObject.optString("payload")
                            );
                        }
                    }
                    catch (JSONException jsonx){
                        jsonx.printStackTrace();
                    }
                }
            }
        }
        //Release the wake lock provided by the WakefulBroadcastReceiver
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    private void sendNotification(String id, String msg, String title, String objectType,
                                  String objectId, String mappingId, String payload){

        Log.d(TAG, "object_type = " + objectType);
        Log.d(TAG, "object_id = " + objectId);

        switch (objectType.toLowerCase()){
            case MESSAGE_TYPE_ACTION:
            case MESSAGE_TYPE_CUSTOM_ACTION:
                try{
                    NotificationUtil.putActionNotification(this,Integer.valueOf(id), title, msg,
                            Integer.valueOf(objectId), Integer.valueOf(mappingId));
                }
                catch (NumberFormatException nfx){
                    nfx.printStackTrace();
                }
                break;

            case MESSAGE_TYPE_ENROLLMENT:
                try{
                    NotificationUtil.putEnrollmentNotification(this, Integer.valueOf(objectId),
                            title, msg);
                }
                catch (NumberFormatException nfx){
                    nfx.printStackTrace();
                }
                break;

            case MESSAGE_TYPE_CHECK_IN:
                NotificationUtil.putCheckInNotification(this, title, msg);
                break;

            case MESSAGE_TYPE_AWARD:
            case MESSAGE_TYPE_BADGE:
                NotificationUtil.putBadgeNotification(this, new Badge(title, msg, payload));
                break;
        }
    }
}
