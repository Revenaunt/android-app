package org.tndata.android.compass.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.tndata.android.compass.CompassApplication;
import org.tndata.compass.model.CustomAction;
import org.tndata.compass.model.GcmMessage;
import org.tndata.compass.model.User;
import org.tndata.compass.model.ResultSet;
import org.tndata.compass.model.UserAction;
import org.tndata.android.compass.parser.Parser;
import org.tndata.android.compass.parser.ParserMethods;
import org.tndata.android.compass.receiver.GcmBroadcastReceiver;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.NotificationUtil;

import es.sandwatch.httprequests.HttpRequest;
import es.sandwatch.httprequests.HttpRequestError;


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

    public static final String FROM_GCM_KEY = "org.tndata.Compass.GcmIntentService.FromGcm";
    public static final String MESSAGE_KEY = "org.tndata.Compass.GcmIntentService.Message";


    public GcmIntentService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        boolean isFromGcm = intent.getBooleanExtra(FROM_GCM_KEY, false);
        String gcmMessage = intent.getStringExtra(MESSAGE_KEY);
        boolean release = true;

        //IntentServices are executed in the background, so it is safe to do this
        GcmMessage message = ParserMethods.sGson.fromJson(gcmMessage, GcmMessage.class);
        if (message.isProduction() == !API.STAGING){
            User user = ((CompassApplication)getApplicationContext()).getUser();
            if (user.getId() == message.getRecipient()){
                message.setGcmMessage(gcmMessage);
                if (message.isUserActionMessage() || message.isCustomActionMessage()){
                    new ActionFetcher(this, intent, message);
                    release = false;
                }
                else{
                    //CompassUtil.log(getApplicationContext(), "Gcm Message", "Not an action message");
                    NotificationUtil.generateNotification(this, message);
                }
            }
            else{
                //CompassUtil.log(getApplicationContext(), "Gcm Message", "targets mismatch");
                long recipient = message.getRecipient();
                long receiver = user.getId();
                Log.e(TAG, "The message was intended for " + recipient + ", received by " + receiver);
            }
        }
        else{
            //CompassUtil.log(getApplicationContext(), "Gcm Message", "environments mismatch");
            String sender = message.isProduction() ? "production" : "staging";
            String running = API.STAGING ? "staging" : "production";
            Log.e(TAG, "The message was delivered from " + sender + ", running " + running);
        }

        if (release && isFromGcm){
            GcmBroadcastReceiver.completeWakefulIntent(intent);
        }
    }


    public static Intent getIntent(Context context, String message){
        return new Intent(context, GcmIntentService.class)
                .putExtra(MESSAGE_KEY, message)
                .putExtra(FROM_GCM_KEY, false);
    }

    public static Intent populateIntent(Context context, Intent intent, String message){
        return intent.putExtra(MESSAGE_KEY, message)
                .putExtra(FROM_GCM_KEY, true)
                .setComponent(
                        new ComponentName(
                                context.getPackageName(),
                                GcmIntentService.class.getName()
                        )
                );
    }


    /**
     * Class that fetches an action whenever an instance is created and releases the
     * Receiver's wake lock on completion.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    private static class ActionFetcher implements HttpRequest.RequestCallback, Parser.ParserCallback{
        private Context mContext;
        private Intent mIntent;
        private GcmMessage mMessage;


        /**
         * Constructor. Triggers the fetching operation.
         *
         * @param context a reference to the context.
         * @param intent the intent that triggered the fetch.
         * @param message the message delivered by GCM
         */
        private ActionFetcher(Context context, Intent intent, GcmMessage message){
            mContext = context.getApplicationContext();
            mIntent = intent;
            mMessage = message;
            if (mMessage.isUserActionMessage()){
                //CompassUtil.log(mContext, "Gcm Message", "User action: " + mMessage.getUserMappingId());
                HttpRequest.get(this, API.URL.getUserAction(mMessage.getUserMappingId()));
            }
            else if (mMessage.isCustomActionMessage()){
                //CompassUtil.log(mContext, "Gcm Message", "Custom action: " + mMessage.getObjectId());
                HttpRequest.get(this, API.URL.getCustomAction(mMessage.getObjectId()));
            }
        }

        @Override
        public void onRequestComplete(int requestCode, String result){
            //CompassUtil.log(mContext, "Gcm Message", "Request completed successfully");
            //CompassUtil.log(mContext, "Gcm Message", result);
            if (mMessage.isUserActionMessage()){
                Parser.parse(result, UserAction.class, this);
            }
            else if (mMessage.isCustomActionMessage()){
                Parser.parse(result, CustomAction.class, this);
            }
        }

        @Override
        public void onRequestFailed(int requestCode, HttpRequestError error){
            //CompassUtil.log(mContext, "Gcm Message", "Request failed");
            //CompassUtil.log(mContext, "Gcm Message", error.toString());
            GcmBroadcastReceiver.completeWakefulIntent(mIntent);
        }

        @Override
        public void onProcessResult(int requestCode, ResultSet result){

        }

        @Override
        public void onParseSuccess(int requestCode, ResultSet result){
            //CompassUtil.log(mContext, "Gcm Message", "Parsing succeeded");
            //CompassUtil.log(mContext, "Gcm Message", result.toString());
            if (result instanceof UserAction){
                mMessage.setUserAction((UserAction)result);
            }
            else if (result instanceof CustomAction){
                mMessage.setCustomAction((CustomAction)result);
            }
            if (mMessage.getAction() == null){
                //CompassUtil.log(mContext, "Gcm Message", "mMessage.getAction() yields null");
            }
            else{
                //CompassUtil.log(mContext, "Gcm Message", "mMessage.getAction() yields an action:");
                //CompassUtil.log(mContext, "Gcm Message", mMessage.getAction().toString());
            }
            NotificationUtil.generateNotification(mContext, mMessage);
            GcmBroadcastReceiver.completeWakefulIntent(mIntent);
        }

        @Override
        public void onParseFailed(int requestCode){
            //CompassUtil.log(mContext, "Gcm Message", "Parsing failed");
            GcmBroadcastReceiver.completeWakefulIntent(mIntent);
        }
    }
}
