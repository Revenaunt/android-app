package org.tndata.android.compass.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.tndata.android.compass.fragment.CheckInReviewFragment;
import org.tndata.android.compass.model.Action;
import org.tndata.android.compass.model.Goal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Adapter used for CheckInActivity's ViewPager.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class CheckInPagerAdapter extends FragmentPagerAdapter{
    private List<Goal> mGoals;
    private List<List<Action>> mActionLists;

    private List<Fragment> mFragments;


    /**
     * Constructor.
     *
     * @param fm the fragment manager.
     * @param dataSet the data to be displayed by the adapter.
     */
    public CheckInPagerAdapter(FragmentManager fm, Map<Goal, List<Action>> dataSet){
        super(fm);

        //Populate the lists with the data in the set
        mGoals = new ArrayList<>();
        mActionLists = new ArrayList<>();
        for (Map.Entry<Goal, List<Action>> entry:dataSet.entrySet()){
            mGoals.add(entry.getKey());
            mActionLists.add(entry.getValue());
        }

        mFragments = new ArrayList<>();
    }

    @Override
    public Fragment getItem(int position){
        //If the fragment has not been created yet, create it
        if (position == mFragments.size()){
            mFragments.add(CheckInReviewFragment.newInstance(mGoals.get(position), mActionLists.get(position)));
        }
        return mFragments.get(position);
    }

    @Override
    public int getCount(){
        return mGoals.size();
    }
}
