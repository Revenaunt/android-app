package org.tndata.android.compass.model;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import org.tndata.android.compass.util.FeedDataLoader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Data holder for the data to be displayed in the feed.
 *
 * @author Ismael Alonso
 * @version 2.0.0
 */
public class FeedData{
    public static final String TAG = "FeedData";
    public static final String API_TYPE = "feed";


    //API delivered fields
    @SerializedName("progress")
    private Progress mProgress;
    @SerializedName("upcoming")
    private List<UpcomingAction> mUpcomingActions;
    @SerializedName("suggestions")
    private List<TDCGoal> mSuggestions;
    @SerializedName("streaks")
    private List<Streak> mStreaks;
    @SerializedName("funcontent")
    private Reward mReward;

    //Fields set during post-processing or after data retrieval
    private UpcomingAction mUpNextAction;
    private List<Goal> mDisplayedGoals;


    private Action mUpNext;
    private UserAction mNextUserAction;
    private CustomAction mNextCustomAction;


    /**
     * Initializes the feed data bundle.
     */
    public void init(){
        mDisplayedGoals = new ArrayList<>();
        if (!mUpcomingActions.isEmpty()){
            mUpNextAction = mUpcomingActions.remove(0);
        }
    }


    /*------------------------------*
     * REGULAR GETTERS AND CHECKERS *
     *------------------------------*/

    /**
     * Progress getter.
     *
     * @return the action daily progress.
     */
    public Progress getProgress(){
        return mProgress;
    }

    /**
     * Tells whether there are any streaks.
     *
     * @return true if there are streaks, false otherwise.
     */
    public boolean hasStreaks(){
        return mStreaks != null && mStreaks.size() > 0;
    }

    /**
     * Streaks getter.
     *
     * @return the streaks data.
     */
    public List<Streak> getStreaks(){
        return mStreaks;
    }

    /**
     * Goal getter.
     *
     * @return the list of loaded goals.
     */
    public List<Goal> getGoals(){
        return mDisplayedGoals;
    }

    /**
     * Gets the list of suggestions.
     *
     * @return the list of suggestions.
     */
    public List<TDCGoal> getSuggestions(){
        return mSuggestions;
    }

    public Reward getReward(){
        return mReward;
    }


    /*--------------------------------------*
     * LOAD MORE / SEE MORE RELATED METHODS *
     *--------------------------------------*/

    /**
     * Adds a batch of goals loaded from the API.
     *
     * @param goals a list containing the loaded goals.
     */
    public void addGoals(@NonNull List<Goal> goals){
        for (Goal goal:goals){
            //Call addGoal() to ensure the goal wasn't already added to the bundle.
            addGoal(goal);
        }
    }


    /*------------------------*
     * NEW STUFF, MOVE AROUND *
     *------------------------*/

    public void setNextUserAction(UserAction userAction){
        mNextUserAction = userAction;
    }

    public void setNextCustomAction(CustomAction customAction){
        mNextCustomAction = customAction;
    }

    private Action getNextAction(){
        if (mNextUserAction != null && mNextCustomAction == null){
            return mNextUserAction;
        }
        else if (mNextUserAction == null && mNextCustomAction != null){
            return mNextCustomAction;
        }
        else if (mNextUserAction != null){ //&& mNextCustomAction != null){ //Implicit
            //Comparing the actions compares their triggers first
            if (mNextUserAction.compareTo(mNextCustomAction) < 0){
                return mNextUserAction;
            }
            else{
                return mNextCustomAction;
            }
        }
        else{
            return null;
        }
    }

    public void replaceUpNext(){
        Action next = getNextAction();
        if (next instanceof UserAction){
            bumpNextUserAction();
        }
        else if (next instanceof CustomAction){
            bumpNextCustomAction();
        }
        else{
            mUpNext = null;
        }
    }

    private void bumpNextUserAction(){
        mUpNext = mNextUserAction;
        mNextUserAction = null;
        FeedDataLoader.getInstance().loadNextUserAction();
    }

    private void bumpNextCustomAction(){
        mUpNext = mNextCustomAction;
        mNextCustomAction = null;
        FeedDataLoader.getInstance().loadNextCustomAction();
    }

    public Action getUpNext(){
        return mUpNext;
    }





    /**
     * Adds a goal to the data set.
     *
     * @param goal the goal to be added.
     */
    public void addGoal(Goal goal){
        if (!mDisplayedGoals.contains(goal)){
            Log.d(TAG, "Adding goal: " + goal);
            mDisplayedGoals.add(goal);
        }
    }

    /**
     * Updates a goal in the data set.
     *
     * @param goal the goal to be updated.
     */
    public void updateGoal(Goal goal){
        Log.d(TAG, "Updating goal: " + goal);
        //This operation can only happen for custom goals
        if (goal instanceof CustomGoal){
            CustomGoal customGoal = (CustomGoal)goal;
            //Find the goal and update it
            for (Goal listedGoal:mDisplayedGoals){
                if (listedGoal.equals(goal)){
                    CustomGoal existingGoal = (CustomGoal)listedGoal;
                    existingGoal.setTitle(customGoal.getTitle());
                    break;
                }
            }
            //Try to update all the actions, the update() method checks for equality
            for (UpcomingAction upcomingAction:mUpcomingActions){
                upcomingAction.update(customGoal);
            }
        }
    }

    /**
     * Removes a goal from the data set.
     *
     * @param goal the goal to be removed.
     * @return the index of the goal in the backing list prior to removal, -1 if not found.
     */
    public int removeGoal(Goal goal){
        for (int i = 0; i < mDisplayedGoals.size(); i++){
            if (mDisplayedGoals.get(i).equals(goal)){
                Log.d(TAG, "Removing goal: " + goal);
                mDisplayedGoals.remove(i);
                return i;
            }
        }
        return -1;
    }

    /**
     * Tells whether a particular action is scheduled to happen between now and the end of the day.
     *
     * @param action the action to be checked.
     * @return true if the action is happening today, false otherwise.
     */
    private boolean happensToday(Action action){
        if (action.getNextReminder().equals("")){
            Log.d(TAG, "happensToday(): next reminder is not set");
            return false;
        }

        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.set(Calendar.DAY_OF_MONTH, tomorrowCalendar.get(Calendar.DAY_OF_MONTH)+1);
        tomorrowCalendar.set(Calendar.HOUR_OF_DAY, 0);
        tomorrowCalendar.set(Calendar.MINUTE, 0);
        tomorrowCalendar.set(Calendar.SECOND, 0);
        tomorrowCalendar.set(Calendar.MILLISECOND, 0);

        Date now = Calendar.getInstance().getTime();
        Date tomorrow = tomorrowCalendar.getTime();
        Date actionDate = action.getNextReminderDate();

        Log.d(TAG, "Action date: " + actionDate);

        return actionDate.after(now) && actionDate.before(tomorrow);
    }

    /**
     * Adds an action to the upcoming list if the action is due today.
     *
     * @param action the action to be added.
     */
    public void addAction(Action action){
        if (happensToday(action)){
            Log.i(TAG, "Adding " + action + ".");
            if (mUpNext == null){
                Log.i(TAG, "Action is up next.");
                mUpNext = action;
            }
            else if (action.happensBefore(mUpNext)){
                Log.i(TAG, "Action is up next.");
                if (mUpNext instanceof UserAction){
                    mNextUserAction = (UserAction)mUpNext;
                }
                else if (mUpNext instanceof CustomAction){
                    mNextCustomAction = (CustomAction)mUpNext;
                }
            }
            else{
                if (action instanceof UserAction && action.happensBefore(mNextUserAction)){
                    Log.i(TAG, "Action is next user action.");
                    mNextUserAction = (UserAction)action;
                }
                else if (action instanceof CustomAction && action.happensBefore(mNextCustomAction)){
                    Log.i(TAG, "Action is next custom action.");
                    mNextCustomAction = (CustomAction)action;
                }
            }
        }
    }

    /**
     * Updates an action in the data set. Use when no goal can be provided.
     *
     * @param action the action to be updated.
     */
    public void updateAction(Action action){
        Log.d(TAG, "Updating action: " + action);

        if (!happensToday(action)){
            replaceUpNext();
        }
        else{
            //TODO title might've change
            Action next = getNextAction();
            if (next != null && next.compareTo(action) < 0){
                if (next instanceof UserAction){
                    bumpNextUserAction();
                }
                else if (next instanceof CustomAction){
                    bumpNextCustomAction();
                }
            }
        }
    }

    /**
     * Removes an action from the data set.
     *
     * @param action the action to be removed.
     */
    public void removeAction(Action action){
        if (mUpNext != null && mUpNext.equals(action)){
            replaceUpNext();
        }
        else if (mNextUserAction != null && mNextUserAction.equals(action)){
            FeedDataLoader.getInstance().loadNextUserAction();
        }
        else if (mNextCustomAction != null && mNextCustomAction.equals(action)){
            FeedDataLoader.getInstance().loadNextCustomAction();
        }
    }


    /**
     * Model for the user's daily progress towards actions.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    public class Progress{
        @SerializedName("total")
        private int mTotalActions;
        @SerializedName("completed")
        private int mCompletedActions;
        @SerializedName("progress")
        private int mProgressPercentage;


        /**
         * Completes one action's stats in the data set.
         */
        private void complete(){
            mCompletedActions++;
            mProgressPercentage = mCompletedActions * 100 / mTotalActions;
        }

        /**
         * Removes one action's stats from the data set.
         */
        private void remove(){
            mTotalActions--;
            mProgressPercentage = mCompletedActions * 100 / mTotalActions;
        }

        /**
         * Gets the total actions.
         *
         * @return the total actions.
         */
        public int getTotalActions(){
            return mTotalActions;
        }

        /**
         * Gets the completed actions,
         *
         * @return the completed actions.
         */
        public int getCompletedActions(){
            return mCompletedActions;
        }

        /**
         * Gets the progress percentage of completed actions.
         *
         * @return the progress percentage of completed actions.
         */
        public int getProgressPercentage(){
            return mProgressPercentage;
        }

        /**
         * Gets the progress percentage as a fraction.
         *
         * @return the progress percentage as a fraction.
         */
        public String getProgressFraction(){
            return mCompletedActions + "/" + mTotalActions;
        }
    }

    /**
     * Model for the user's daily progress streaks.
     *
     * @author Brad Montgomery
     * @version 1.0.0
     */
    public class Streak {
        @SerializedName("day")
        private String mDay;
        @SerializedName("date")
        private String mDate;
        @SerializedName("count")
        private int mCount = 0;

        public boolean completed() {
            return mCount > 0;
        }
        public String getDay() {
            return mDay;
        }

        public String getDayAbbrev() {
            return mDay.substring(0, 1);
        }

        public String getDate() {
            return mDate;
        }

        public int getCount() {
            return mCount;
        }
    }
}
