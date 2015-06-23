package org.tndata.android.compass.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Window;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;

import org.tndata.android.compass.R;
import org.tndata.android.compass.fragment.ActionTriggerFragment;
import org.tndata.android.compass.model.Action;
import org.tndata.android.compass.model.Goal;

public class ActionTriggerActivity extends BaseTriggerActivity implements
        ActionTriggerFragment.ActionTriggerFragmentListener {

    private Toolbar mToolbar;
    private Goal mGoal;
    private Action mAction;
    private ActionTriggerFragment fragment;

    private static final String TAG = "ActionTriggerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);

        mGoal = (Goal) getIntent().getSerializableExtra("goal");
        mAction = (Action) getIntent().getSerializableExtra("action");

        // Initialize the reminder data
        initializeReminders(mAction.getCustomTrigger());

        setContentView(R.layout.activity_base);

        mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        mToolbar.setTitle(mGoal.getTitle());
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        fragment = ActionTriggerFragment.newInstance(mGoal, mAction);
        if (fragment != null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.base_content, fragment).commit();
        }
    }

    public void disableTrigger() {
        Log.d(TAG, "----> REMINDERS OFF");  // TODO: figure out how to disable.
    }

    public void fireTimePicker() {
        Log.d(TAG, "----> fireTimePicker");
        showTimePicker();
    }

    public void fireDatePicker() {
        Log.d(TAG, "----> fireDatePicker");
        showDatePicker();
    }

    public void fireRecurrencePicker(String rrule) {
        showRecurrencePicker(rrule);
    }

    public void fireSaveTrigger() {
        saveActionTrigger(mAction);
        finish();
    }

    // OVER-Riding BaseTriggerActivity's event callbacks.

    @Override
    public void onTimeSet(RadialTimePickerDialog dialog, int hourOfDay, int minute) {
        String time = String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute);
        setTime(time);
        if(fragment != null) {
            fragment.updateTimeView(hourOfDay, minute);
        }
    }

    @Override
    public void onDateSet(CalendarDatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        String date = String.format("%4d", year) + "-" +
                String.format("%02d", monthOfYear) + "-" +
                String.format("%02d", dayOfMonth);

        setDate(date);
        if(fragment != null) {
            fragment.updateDateView(year, monthOfYear, dayOfMonth);
        }
    }

    @Override
    public void onRecurrenceSet(String rrule) {
        if(rrule != null) {
            setEventRecurrence(rrule );
        }

        if(fragment != null) {
            fragment.updateRecurrenceView(getFriendlyRecurrenceString());
        }
        // Our API needs the 'RRULE' prefix on the rrule string, but
        // BetterPicker's EventRecurrence should *not* have that: https://goo.gl/QhY9aC
        setRRULE(rrule);
    }

    @Override
    public boolean actionTriggerAdded(Action action) {
        Boolean success = super.actionTriggerAdded(action);
        if(success) {
            finish();
        }
        return success;
    }

}
