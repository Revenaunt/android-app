package org.tndata.android.compass.fragment;

import org.tndata.android.compass.BuildConfig;
import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;


/**
 * Fragment that contains the UI for settings. Triggers settings events.
 */
public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener{
    private static final String NOTIFICATIONS_KEY = "settings_notifications";
    private static final String LOGOUT_KEY = "settings_logout";
    private static final String TOS_KEY = "settings_tos";
    private static final String PRIVACY_KEY = "settings_privacy";
    private static final String SOURCES_KEY = "settings_sources";
    private static final String VERSION_KEY = "settings_version";


    //The listener interface
    private OnSettingsClickListener mListener;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference logOut = findPreference(LOGOUT_KEY);
        logOut.setOnPreferenceClickListener(this);
        String displayName = "";
        try{
            displayName = ((CompassApplication)getActivity().getApplication()).getUser().getFullName();
        }
        catch (Exception x){
            x.printStackTrace();
        }
        logOut.setSummary(getActivity().getResources().getString(
                R.string.settings_logout_summary, displayName));


        findPreference(NOTIFICATIONS_KEY).setOnPreferenceClickListener(this);
        findPreference(TOS_KEY).setOnPreferenceClickListener(this);
        findPreference(PRIVACY_KEY).setOnPreferenceClickListener(this);
        findPreference(SOURCES_KEY).setOnPreferenceClickListener(this);
        findPreference(VERSION_KEY).setSummary(BuildConfig.VERSION_NAME);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        //This makes sure that the container activity has implemented the callback
        //  interface. If not, it throws an exception.
        try{
            mListener = (OnSettingsClickListener)context;
        }
        catch (ClassCastException ccx){
            throw new ClassCastException(context.toString()
                    + " must implement OnSettingsClickListener");
        }
    }

    //This method is deprecated, but the method above won't get called in API < 23. The
    //  absence of this one would render settings useless for anything not M. This is a
    //  quick and dirty fix, I might do something smarter some day.
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        //This makes sure that the container activity has implemented the callback
        //  interface. If not, it throws an exception.
        try{
            mListener = (OnSettingsClickListener)activity;
        }
        catch (ClassCastException ccx){
            throw new ClassCastException(activity.toString()
                    + " must implement OnSettingsClickListener");
        }
    }

    @Override
    public void onDetach(){
        mListener = null;
        super.onDetach();
    }

    @Override
    public boolean onPreferenceClick(Preference preference){
        if (mListener != null){
            switch (preference.getKey()){
                case NOTIFICATIONS_KEY:
                    mListener.notifications();
                    return true;

                case LOGOUT_KEY:
                    mListener.logOut();
                    return true;

                case TOS_KEY:
                    mListener.tos();
                    return true;

                case PRIVACY_KEY:
                    mListener.privacy();
                    return true;

                case SOURCES_KEY:
                    mListener.sources();
                    return true;
            }
        }
        return false;
    }


    /**
     * Listener interface to handle preference click events int the host activity.
     */
    public interface OnSettingsClickListener{
        /**
         * Called when the notifications preference is clicked.
         */
        void notifications();

        /**
         * Called when the log out preference is clicked.
         */
        void logOut();

        /**
         * Called when the ToS preference is tapped.
         */
        void tos();

        /**
         * Called when the Privacy Policy preference is tapped.
         */
        void privacy();

        /**
         * Called when the sources preference is clicked.
         */
        void sources();
    }
}
