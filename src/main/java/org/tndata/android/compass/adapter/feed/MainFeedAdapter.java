package org.tndata.android.compass.adapter.feed;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.model.FeedData;
import org.tndata.android.compass.model.Goal;
import org.tndata.android.compass.model.GoalContent;
import org.tndata.android.compass.model.UpcomingAction;
import org.tndata.android.compass.parser.Parser;
import org.tndata.android.compass.parser.ParserModels;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.CompassUtil;

import java.util.List;

import es.sandwatch.httprequests.HttpRequest;
import es.sandwatch.httprequests.HttpRequestError;


/**
 * Adapter for the main feed.
 *
 * @author Ismael Alonso
 * @version 2.0.0
 */
public class MainFeedAdapter
        extends RecyclerView.Adapter
        implements
                HttpRequest.RequestCallback,
                Parser.ParserCallback{

    //Item view types
    private static final int TYPE_BLANK = 0;
    private static final int TYPE_WELCOME = TYPE_BLANK+1;
    private static final int TYPE_UP_NEXT = TYPE_WELCOME+1;
    private static final int TYPE_FEEDBACK = TYPE_UP_NEXT+1;
    private static final int TYPE_SUGGESTION = TYPE_FEEDBACK+1;
    private static final int TYPE_UPCOMING = TYPE_SUGGESTION+1;
    private static final int TYPE_MY_GOALS = TYPE_UPCOMING +1;
    private static final int TYPE_GOAL_SUGGESTIONS = TYPE_MY_GOALS+1;
    private static final int TYPE_OTHER = TYPE_GOAL_SUGGESTIONS+1;


    final Context mContext;
    final Listener mListener;

    private FeedData mFeedData;
    private FeedUtil mFeedUtil;
    private GoalContent mSuggestion;

    private UpcomingHolder mUpcomingHolder;
    private GoalsHolder<Goal> mMyGoalsHolder;
    private GoalsHolder<GoalContent> mSuggestionsHolder;

    private int mGetMoreGoalsRC;


    /**
     * Constructor.
     *
     * @param context the context,
     * @param listener the listener.
     * @param initialSuggestion true the feed should display an initial suggestion.
     */
    public MainFeedAdapter(@NonNull Context context, @NonNull Listener listener,
                           boolean initialSuggestion){
        mContext = context;
        mListener = listener;
        mFeedData = ((CompassApplication)mContext.getApplicationContext()).getFeedData();

        if (mFeedData == null){
            mListener.onNullData();
        }
        else{
            CardTypes.setDataSource(mFeedData);
            List<GoalContent> suggestions = mFeedData.getSuggestions();
            if (suggestions.isEmpty()){
                mSuggestion = null;
            }
            else{
                mSuggestion = suggestions.get((int)(Math.random()*suggestions.size()));
            }
            mFeedUtil = new FeedUtil(this);
        }

        if (mSuggestion != null){
            if (initialSuggestion){
                CardTypes.displaySuggestion(true);
            }
            else{
                new Handler().postDelayed(new Runnable(){
                    @Override
                    public void run(){
                        CardTypes.displaySuggestion(true);
                        notifyItemInserted(CardTypes.getSuggestionPosition());
                        notifyItemRangeChanged(CardTypes.getSuggestionPosition() + 1, getItemCount() - 1);
                    }
                }, 2000);
            }
        }
    }


    /*------------------------------------*
     * OVERRIDDEN ADAPTER RELATED METHODS *
     *------------------------------------*/

    @Override
    public int getItemViewType(int position){
        //The first card is always a blank card
        if (position == 0){
            return TYPE_BLANK;
        }
        //The second card may be a welcome card, but only if the user has no goals selected
        if (CardTypes.hasWelcomeCard() && position == 1){
            return TYPE_WELCOME;
        }
        //The rest of them have checker methods
        if (CardTypes.isUpNext(position)){
            return TYPE_UP_NEXT;
        }
        if (CardTypes.isFeedback(position)){
            return TYPE_FEEDBACK;
        }
        if (CardTypes.isSuggestion(position)){
            return TYPE_SUGGESTION;
        }
        if (CardTypes.isUpcoming(position)){
            return TYPE_UPCOMING;
        }
        if (CardTypes.isMyGoals(position)){
            return TYPE_MY_GOALS;
        }
        if (CardTypes.isGoalSuggestions(position)){
            return TYPE_GOAL_SUGGESTIONS;
        }
        return TYPE_OTHER;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        if (viewType == TYPE_BLANK){
            return new RecyclerView.ViewHolder(new CardView(mContext)){};
        }
        else if (viewType == TYPE_WELCOME){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.card_welcome, parent, false);
            view.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    mListener.onInstructionsSelected();
                }
            });
            return new RecyclerView.ViewHolder(view){};
        }
        else if (viewType == TYPE_UP_NEXT){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            return new UpNextHolder(this, inflater.inflate(R.layout.card_up_next, parent, false));
        }
        else if (viewType == TYPE_FEEDBACK){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            return new FeedbackHolder(this, inflater.inflate(R.layout.card_feedback, parent, false));
        }
        else if (viewType == TYPE_SUGGESTION){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            return new GoalSuggestionHolder(this, inflater.inflate(R.layout.card_goal_suggestion, parent, false));
        }
        else if (viewType == TYPE_UPCOMING){
            if (mUpcomingHolder == null){
                LayoutInflater inflater = LayoutInflater.from(mContext);
                View rootView = inflater.inflate(R.layout.card_upcoming, parent, false);
                mUpcomingHolder = new UpcomingHolder(this, rootView);
            }
            return mUpcomingHolder;
        }
        else if (viewType == TYPE_MY_GOALS){
            if (mMyGoalsHolder == null){
                LayoutInflater inflater = LayoutInflater.from(mContext);
                View rootView = inflater.inflate(R.layout.card_goals, parent, false);
                mMyGoalsHolder = new GoalsHolder<>(this, rootView);
            }
            return mMyGoalsHolder;
        }
        else if (viewType == TYPE_GOAL_SUGGESTIONS){
            if (mSuggestionsHolder == null){
                LayoutInflater inflater = LayoutInflater.from(mContext);
                View rootView = inflater.inflate(R.layout.card_goals, parent, false);
                mSuggestionsHolder = new GoalsHolder<>(this, rootView);
            }
            return mSuggestionsHolder;
        }
        else if (viewType == TYPE_OTHER){
            return new RecyclerView.ViewHolder(new CardView(mContext)){};
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder rawHolder, int position){
        //This is a possible fix to a crash where the application gets destroyed and the
        //  user data gets invalidated. In such a case, the app should restart and fetch
        //  the user data again. Bottomline, do not keep going
        if (mFeedData == null){
            return;
        }

        //Blank space
        if (position == 0){
            int width = CompassUtil.getScreenWidth(mContext);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int)((width*2/3)*0.8));
            rawHolder.itemView.setLayoutParams(params);
            rawHolder.itemView.setVisibility(View.INVISIBLE);
        }
        //Up next
        else if (CardTypes.isUpNext(position)){
            ((UpNextHolder)rawHolder).bind(mFeedData.getUpNextAction(), mFeedData.getProgress());
        }
        //Feedback
        else if (CardTypes.isFeedback(position)){
            ((FeedbackHolder)rawHolder).bind(mFeedData.getFeedback());
        }
        //Goal suggestion card
        else if (CardTypes.isSuggestion(position)){
            GoalSuggestionHolder holder = (GoalSuggestionHolder)rawHolder;
            holder.mTitle.setText(mSuggestion.getTitle());
        }
        //Today's activities / upcoming
        else if (CardTypes.isUpcoming(position)){
            if (mUpcomingHolder.getItemCount() == 0){
                moreActions();
            }
        }
        //My goals
        else if (CardTypes.isMyGoals(position)){
            if (mMyGoalsHolder.getItemCount() == 0){
                mMyGoalsHolder.bind(mContext.getString(R.string.card_my_goals_header));
                mMyGoalsHolder.setGoals(mFeedData.getGoals());
                if (mFeedData.getNextGoalBatchUrl() == null){
                    mMyGoalsHolder.hideFooter();
                }
            }
        }
        else if (CardTypes.isGoalSuggestions(position)){
            if (mSuggestionsHolder.getItemCount() == 0){
                mSuggestionsHolder.bind(mContext.getString(R.string.card_suggestions_header));
                mSuggestionsHolder.setGoals(mFeedData.getSuggestions());
                mSuggestionsHolder.hideFooter();
            }
        }
    }

    @Override
    public int getItemCount(){
        return CardTypes.getItemCount();
    }


    /*----------------------*
     * FEED ADAPTER METHODS *
     *----------------------*/

    public int getGoalsPosition(){
        return CardTypes.getGoalsPosition();
    }

    public void updateDataSet(){
        updateUpcoming();
        updateMyGoals();
    }

    public void updateUpcoming(){
        notifyItemChanged(CardTypes.getUpNextPosition());
        if (mUpcomingHolder != null){
            mUpcomingHolder.updateActions(mFeedData);
        }
    }

    public void updateMyGoals(){
        if (mMyGoalsHolder != null){
            mMyGoalsHolder.updateGoals();
            notifyItemChanged(CardTypes.getGoalsPosition());
        }
    }


    /*------------------------*
     * ACTION RELATED METHODS *
     *------------------------*/

    /**
     * Marks an action as done in every possible way. This is done in three steps:
     * (1) mark the action as complete within the model and the webapp through an
     * api request, (2) update the data set, and (3) update the adapter to reflect
     * the changes in the data set.
     *
     * @param action the action to be marked as done.
     */
    public void didIt(UpcomingAction action){
        mFeedData.removeUpcomingActionX(action, true);
    }


    /*----------------------*
     * GOAL RELATED METHODS *
     *----------------------*/

    void showSuggestionPopup(View anchor){
        mFeedUtil.showSuggestionPopup(anchor);
    }

    void refreshSuggestion(){
        List<GoalContent> suggestions = mFeedData.getSuggestions();
        mSuggestion = suggestions.get((int)(Math.random()*suggestions.size()));
        notifyItemChanged(CardTypes.getSuggestionPosition());
    }

    public void dismissSuggestion(){
        CardTypes.displaySuggestion(false);
        notifyItemRemoved(CardTypes.getSuggestionPosition());
        notifyItemRangeChanged(CardTypes.getSuggestionPosition() + 1, getItemCount() - 1);
        mListener.onSuggestionDismissed();
    }

    void viewSuggestion(){
        mListener.onSuggestionSelected(mSuggestion);
    }


    /*------------------------*
     * FOOTER RELATED METHODS *
     *------------------------*/

    /**
     * Loads the next batch of actions into the feed.
     */
    void moreActions(){
        mUpcomingHolder.addActions(mFeedData.loadMoreUpcoming(mUpcomingHolder.getItemCount()));
        if (!mFeedData.canLoadMoreActions(mUpcomingHolder.getItemCount())){
            mUpcomingHolder.hideFooter();
        }
    }

    /**
     * Loads the next batch of goals into the feed.
     */
    void moreGoals(){
        mGetMoreGoalsRC = HttpRequest.get(this, mFeedData.getNextGoalBatchUrl());
    }


    /*-------------------------*
     * REQUEST RELATED METHODS *
     *-------------------------*/

    @Override
    public void onRequestComplete(int requestCode, String result){
        if (requestCode == mGetMoreGoalsRC){
            if (mFeedData.getNextGoalBatchUrl().contains("customgoals")){
                Parser.parse(result, ParserModels.CustomGoalsResultSet.class, this);
            }
            else{
                Parser.parse(result, ParserModels.UserGoalsResultSet.class, this);
            }
        }
    }

    @Override
    public void onRequestFailed(int requestCode, HttpRequestError error){

    }

    @Override
    public void onProcessResult(int requestCode, ParserModels.ResultSet result){

    }

    @Override
    public void onParseSuccess(int requestCode, ParserModels.ResultSet result){
        if (result instanceof ParserModels.CustomGoalsResultSet){
            ParserModels.CustomGoalsResultSet set = (ParserModels.CustomGoalsResultSet)result;
            String url = set.next;
            if (url == null){
                url = API.getUserGoalsUrl();
            }
            if (API.STAGING && url.startsWith("https")){
                url = url.replaceFirst("s", "");
            }
            mMyGoalsHolder.prepareGoalAddition();
            mFeedData.addGoals(set.results, url);
            mMyGoalsHolder.onGoalsAdded();
        }
        else if (result instanceof ParserModels.UserGoalsResultSet){
            ParserModels.UserGoalsResultSet set = (ParserModels.UserGoalsResultSet)result;
            mMyGoalsHolder.prepareGoalAddition();
            String url = set.next;
            if (url == null){
                mMyGoalsHolder.hideFooter();
            }
            else{
                if (API.STAGING && url.startsWith("https")){
                    url = url.replaceFirst("s", "");
                }
            }
            mFeedData.addGoals(set.results, url);
            mMyGoalsHolder.onGoalsAdded();
        }
    }


    /**
     * Parent class of all the view holders in for the main feed adapter. Provides a reference
     * to the adapter that needs to be passed through the constructor.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    abstract static class ViewHolder extends RecyclerView.ViewHolder{
        protected MainFeedAdapter mAdapter;

        /**
         * Constructor,
         *
         * @param adapter a reference to the adapter that will handle the holder.
         * @param rootView the root view of this adapter.
         */
        protected ViewHolder(MainFeedAdapter adapter, View rootView){
            super(rootView);
            mAdapter = adapter;
        }
    }


    /**
     * Listener interface for the main feed adapter.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    public interface Listener{
        /**
         * Called when the user data is null.
         */
        void onNullData();

        /**
         * Called when the welcome card is tapped.
         */
        void onInstructionsSelected();

        /**
         * Called when a suggestion is dismissed.
         */
        void onSuggestionDismissed();

        /**
         * Called when a goal from a goal list is selected.
         *
         * @param suggestion the selected goal suggestion.
         */
        void onSuggestionSelected(GoalContent suggestion);

        /**
         * Called when a goal is selected.
         *
         * @param goal the selected goal.
         */
        void onGoalSelected(Goal goal);

        /**
         * Called when the feedback card is tapped.
         *
         * @param feedback bundle containing information about the feedback goal.
         */
        void onFeedbackSelected(FeedData.ActionFeedback feedback);

        /**
         * Called when an action card is tapped.
         *
         * @param action the action being displayed at the card.
         */
        void onActionSelected(UpcomingAction action);
    }
}
