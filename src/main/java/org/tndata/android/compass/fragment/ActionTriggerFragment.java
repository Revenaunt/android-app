package org.tndata.android.compass.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.tndata.android.compass.R;
import org.tndata.android.compass.model.Action;
import org.tndata.android.compass.model.Goal;
import org.tndata.android.compass.model.Trigger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ActionTriggerFragment extends Fragment {

    private ActionTriggerFragmentListener mCallback;
    private Goal mGoal;
    private Action mAction;

    TextView datePickerTextView;
    TextView timePickerTextView;
    TextView recurrencePickerTextView;

    private static final String TAG = "ActionTriggerFragment";

    public interface ActionTriggerFragmentListener {
        public void fireTimePicker();
        public void fireDatePicker();
        public void fireRecurrencePicker(String rrule);
        public void fireSaveTrigger();
        public void disableTrigger();
    }

    public static ActionTriggerFragment newInstance(Goal goal, Action action) {
        ActionTriggerFragment fragment = new ActionTriggerFragment();
        Bundle args = new Bundle();
        args.putSerializable("goal", goal);
        args.putSerializable("action", action);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (ActionTriggerFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ActionTriggerFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoal = getArguments() != null ? ((Goal) getArguments().get(
                "goal")) : new Goal();
        mAction = getArguments() != null ? ((Action) getArguments().get(
                "action")) : new Action();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // TODO: update text views with the default trigger info if there's no custom trigger
        final Trigger trigger = mAction.getTrigger();

        View v = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_action_trigger, container, false);

        TextView titleTextView = (TextView) v.findViewById(R.id.action_title_textview);
        titleTextView.setText(mAction.getTitle());

        Switch notificationSwitch = (Switch) v.findViewById(R.id.notification_option_switch);
        if(trigger != null && trigger.isDisabled()) {
            notificationSwitch.setChecked(false);
        }
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    mCallback.disableTrigger();
                }
            }
        });

        timePickerTextView = (TextView) v.findViewById(R.id.time_picker_textview);
        RelativeLayout timePickerContainer = (RelativeLayout) v.findViewById(
                R.id.time_picker_container);
        timePickerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.fireTimePicker();
            }
        });

        datePickerTextView = (TextView) v.findViewById(R.id.date_picker_textview);
        RelativeLayout datePickerContainer = (RelativeLayout) v.findViewById(R.id.date_picker_container);
        datePickerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.fireDatePicker();
            }
        });

        recurrencePickerTextView = (TextView) v.findViewById(
                R.id.recurrence_picker_textview);
        RelativeLayout recurrencePickerContainer = (RelativeLayout) v.findViewById(
                R.id.recurrence_picker_container);
        recurrencePickerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!trigger.getRecurrences().isEmpty()) {
                    mCallback.fireRecurrencePicker(trigger.getRRULE());
                } else {
                    mCallback.fireRecurrencePicker(Trigger.DEFAULT_RRULE);
                }
            }
        });

        TextView triggerUpdateTextView = (TextView) v.findViewById(
                R.id.trigger_update_textview);
        triggerUpdateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.fireSaveTrigger();
            }
        });

        // Update labels with Trigger details if applicable.
        if(trigger != null) {
            if(!trigger.getTime().isEmpty()) {
                updateTimeView(trigger.getParsedTime());
            }
            if(!trigger.getDate().isEmpty()) {
                updateDateView(trigger.getParsedDate());
            }
            if(!trigger.getRecurrences().isEmpty()) {
                updateRecurrenceView(trigger.getRecurrencesDisplay());
            }
        }

        return v;
    }

    public void updateTimeView(Date time) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        timePickerTextView.setText(sdf.format(time));
    }

    public void updateTimeView(int hourOfDay, int minute) {
        // Format the selected time and update the TextView.
        Date time = Calendar.getInstance().getTime();
        time.setHours(hourOfDay);
        time.setMinutes(minute);

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        timePickerTextView.setText(sdf.format(time));
    }

    public void updateDateView(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy", Locale.getDefault());
        datePickerTextView.setText(sdf.format(date));
    }

    public void updateDateView(int year, int monthOfYear, int dayOfMonth) {
        // Format the selected date and update the TextView
        // NOTE: the picker likes months in the range: 0 - 11
        monthOfYear = monthOfYear - 1;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);
        datePickerTextView.setText(sdf.format(calendar.getTime()));
    }

    public void updateRecurrenceView(String recurrence) {
        if(recurrence != null && !recurrence.isEmpty()) {
            recurrencePickerTextView.setText(recurrence);
        } else {
            recurrencePickerTextView.setText(getText(R.string.trigger_recurrence_picker_label));
        }
    }
}