package org.tndata.android.compass.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;

import org.tndata.android.compass.R;
import org.tndata.android.compass.adapter.SnoozeAdapter;
import org.tndata.android.compass.database.CompassDbHelper;
import org.tndata.android.compass.model.Reminder;
import org.tndata.android.compass.model.UserPlace;
import org.tndata.android.compass.service.ActionReportService;
import org.tndata.android.compass.service.LocationNotificationService;
import org.tndata.android.compass.service.SnoozeService;
import org.tndata.android.compass.util.CompassUtil;
import org.tndata.android.compass.util.NotificationUtil;

import java.util.Calendar;
import java.util.List;


/**
 * Activity with the appearance of a dialog that lets the user pick a snoozing
 * option for a notification.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class SnoozeActivity
        extends AppCompatActivity
        implements
                AdapterView.OnItemClickListener,
                CalendarDatePickerDialog.OnDateSetListener,
                RadialTimePickerDialog.OnTimeSetListener,
                DialogInterface.OnClickListener{

    private static final String TAG = "SnoozeActivity";

    //Request codes
    private static final int PLACES_REQUEST_CODE = 600;
    private static final int LOCATION_PERMISSION_RC = 1;


    private Reminder mReminder;
    private int mYear, mMonth, mDay;

    private List<UserPlace> mPlaces;
    private UserPlace mPickedPlace;

    private AlertDialog mRationaleDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze);
        setTitle(R.string.later_title);

        mReminder = getIntent().getParcelableExtra(NotificationUtil.REMINDER_KEY);

        ListView list = (ListView)findViewById(R.id.snooze_list);
        list.setAdapter(new SnoozeAdapter(this));
        list.setOnItemClickListener(this);

        CompassDbHelper dbHelper = new CompassDbHelper(this);
        mPlaces = dbHelper.getPlaces();
        dbHelper.close();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        if (position == 0){
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(calendar.getTimeInMillis()+3600*1000);
            snooze(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
            reportSnooze(ActionReportService.LENGTH_HOUR);
        }
        else if (position == 1){
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(calendar.getTimeInMillis()+24*3600*1000);
            snooze(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
            reportSnooze(ActionReportService.LENGTH_DAY);
        }
        else if (position == 2){
            //Start the date picker
            Calendar calendar = Calendar.getInstance();
            CalendarDatePickerDialog datePickerDialog = CalendarDatePickerDialog.newInstance(this,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));

            datePickerDialog.show(getSupportFragmentManager(), "SnoozeDate");
        }
        else if (position == 3){
            displayPlacesDialog();
        }
        else if (position == 4){
            if (mReminder.isUserAction()){
                NotificationUtil.cancel(this, NotificationUtil.USER_ACTION_TAG, mReminder.getUserMappingId());
            }
            else if (mReminder.isCustomAction()){
                NotificationUtil.cancel(this, NotificationUtil.CUSTOM_ACTION_TAG, mReminder.getObjectId());
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    /**
     * Opens a dialog with a list of places or an item telling the user to pick places.
     */
    private void displayPlacesDialog(){
        Log.d(TAG, mPlaces.size() + " places found");
        CharSequence[] placeNames = new CharSequence[mPlaces.size()+1];
        for (int i = 0; i < mPlaces.size(); i++){
            placeNames[i] = mPlaces.get(i).getName();
        }
        placeNames[mPlaces.size()] = getString(R.string.later_create_place);

        new AlertDialog.Builder(this)
                .setTitle(R.string.later_pick_place)
                .setItems(placeNames, this)
                .create().show();
    }

    @Override
    public void onDateSet(CalendarDatePickerDialog calendarDatePickerDialog, int year, int month, int day){
        //Set the data and log it
        mYear = year;
        mMonth = month+1;
        mDay = day;
        Log.d(TAG, mYear + "-" + mMonth + "-" + mDay);

        //Start the time picker
        Calendar calendar = Calendar.getInstance();
        RadialTimePickerDialog timePickerDialog = RadialTimePickerDialog.newInstance(this,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(this));

        timePickerDialog.show(getSupportFragmentManager(), "SnoozeTime");
    }

    @Override
    public void onTimeSet(RadialTimePickerDialog radialTimePickerDialog, int hour, int minute){
        Log.d(TAG, hour + ":" + minute);
        snooze(mYear, mMonth, mDay, hour, minute);
        reportSnooze(ActionReportService.LENGTH_CUSTOM);
    }

    /**
     * Snoozes a notification time-wise.
     *
     * @param year the year to snooze to.
     * @param month the month to snooze to.
     * @param day the day to snooze to.
     * @param hour the hour to snooze to.
     * @param minute the minute to snooze to.
     */
    private void snooze(int year, int month, int day, int hour, int minute){
        //Translate to strings
        String monthString = month + "";
        String dayString = day + "";
        String hourString = hour + "";
        String minuteString = minute + "";

        //Process the data
        if (monthString.length() == 1){
            monthString = "0" + monthString;
        }
        if (dayString.length() == 1){
            dayString = "0" + dayString;
        }
        if (hourString.length() == 1){
            hourString = "0" + hourString;
        }
        if (minuteString.length() == 1){
            minuteString = "0" + minuteString;
        }

        //Prepare the strings
        String date = year + "-" + monthString + "-" + dayString;
        String time = hourString + ":" + minuteString;

        //Log the data to verify format
        Log.d(TAG, date + " " + time);

        //Fire up the service with the appropriate parameters
        Intent snooze = new Intent(this, SnoozeService.class);
        snooze.putExtra(SnoozeService.NOTIFICATION_ID_KEY, mReminder.getNotificationId());
        snooze.putExtra(SnoozeService.PUSH_NOTIFICATION_ID_KEY, mReminder.getObjectId());
        snooze.putExtra(SnoozeService.DATE_KEY, date);
        snooze.putExtra(SnoozeService.TIME_KEY, time);
        startService(snooze);

        //Kill the activity
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which){
        //If the caller was the rationale dialog fire the permission request
        if (dialog == mRationaleDialog){
            firePermissionRequest();
        }
        //Otherwise, the user tapped one of the places in the place list dialog
        else{
            //If the user tapped the add a place item, fire the place picker activity
            if (which == mPlaces.size()){
                startActivityForResult(new Intent(this, PlacesActivity.class), PLACES_REQUEST_CODE);
            }
            else{
                //Otherwise, save the tapped place
                mPickedPlace = mPlaces.get(which);
                Log.d(TAG, mPickedPlace.getName() + " selected");
                //If the app doesn't have the location permission, request it, otherwise snooze
                if (!CompassUtil.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    firePermissionRequest();
                }
                else{
                    snooze(mPickedPlace);
                }
            }
        }
    }

    /**
     * Fires the permission rationale dialog or the permission request dialog.
     */
    private void firePermissionRequest(){
        String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;
        boolean rationale = ActivityCompat.shouldShowRequestPermissionRationale(this, locationPermission);
        if (rationale && mRationaleDialog == null){
            //Show the rationale dialog
            mRationaleDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.later_location_rationale_title)
                    .setMessage(R.string.later_location_rationale)
                    .setPositiveButton(R.string.later_location_rationale_button, this)
                    .create();
            mRationaleDialog.show();
        }
        else{
            //Request permissions
            ActivityCompat.requestPermissions(this, new String[]{locationPermission},
                    LOCATION_PERMISSION_RC);
            mRationaleDialog = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        switch (requestCode){
            case LOCATION_PERMISSION_RC:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    snooze(mPickedPlace);
                }
                else{
                    displayPlacesDialog();
                }
                break;
        }
    }

    /**
     * Snoozes the reminder to a place.
     *
     * @param userPlace the user place to snooze the reminder to.
     */
    private void snooze(@NonNull UserPlace userPlace){
        //Prepare and save the reminder
        mReminder.setPlaceId(userPlace.getId());
        mReminder.setSnoozed(true);
        CompassDbHelper dbHelper = new CompassDbHelper(this);
        dbHelper.saveReminder(mReminder);
        dbHelper.close();

        //Cancel the notification
        NotificationUtil.cancel(this, NotificationUtil.USER_ACTION_TAG, mReminder.getObjectId());

        //Start the location service and report the snooze
        LocationNotificationService.start(this);
        reportSnooze(ActionReportService.LENGTH_LOCATION);

        //Kill the activity
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == PLACES_REQUEST_CODE){
            //Reload the list of places
            CompassDbHelper dbHelper = new CompassDbHelper(this);
            mPlaces = dbHelper.getPlaces();
            dbHelper.close();

            //Create the dialog
            displayPlacesDialog();
        }
    }

    /**
     * Reports the snooze operation to the backend.
     *
     * @param length the length of the snooze.
     */
    private void reportSnooze(String length){
        Intent report = new Intent(this, ActionReportService.class)
                .putExtra(NotificationUtil.REMINDER_KEY, mReminder)
                .putExtra(ActionReportService.STATE_KEY, ActionReportService.STATE_SNOOZED)
                .putExtra(ActionReportService.LENGTH_KEY, length);
        startService(report);
    }
}
