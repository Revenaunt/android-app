package org.tndata.android.compass.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;

import org.tndata.android.compass.R;
import org.tndata.compass.model.ResultSet;
import org.tndata.compass.model.Reward;
import org.tndata.compass.model.TDCCategory;
import org.tndata.compass.model.TDCGoal;
import org.tndata.android.compass.parser.Parser;
import org.tndata.android.compass.parser.ParserModels;
import org.tndata.android.compass.util.API;

import es.sandwatch.httprequests.HttpRequest;
import es.sandwatch.httprequests.HttpRequestError;


/**
 * Adapter for GoalActivity.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class GoalAdapter
        extends MaterialAdapter
        implements
                View.OnClickListener,
                HttpRequest.RequestCallback,
                Parser.ParserCallback{

    private Context mContext;
    private GoalListener mListener;
    private TDCCategory mCategory;
    private TDCGoal mGoal;

    private int mGetRewardRC;
    private Reward mReward;

    private Button mSignMeUp;


    /**
     * Constructor.
     *
     * @param context a reference to the context.
     * @param listener the listener.
     * @param category the parent category of the goal to be displayed.
     * @param goal the goal to be displayed.
     */
    public GoalAdapter(@NonNull Context context, @NonNull GoalListener listener,
                       @NonNull TDCCategory category, @NonNull TDCGoal goal){

        super(context, ContentType.DETAIL, true);

        mContext = context;
        mListener = listener;
        mCategory = category;
        mGoal = goal;
    }

    @Override
    protected boolean hasDetails(){
        return mReward != null;
    }

    @Override
    protected void bindHeaderHolder(RecyclerView.ViewHolder rawHolder){
        final HeaderViewHolder holder = (HeaderViewHolder)rawHolder;
        holder.setTitle(mContext.getString(R.string.library_goal_title, mGoal.getTitle()));
        holder.setTitleBold();
        holder.setContent(mGoal.getDescription());
        holder.addButton(R.id.goal_no, R.string.library_goal_no, this);
        mSignMeUp = holder.addButton(R.id.goal_yes, R.string.library_goal_yes, this);

        ViewTreeObserver vto = holder.itemView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout(){
                if (Build.VERSION.SDK_INT < 16){
                    holder.itemView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                else{
                    holder.itemView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                mListener.onGoalCardLoaded();
            }
        });

        fetchReward();
    }

    @Override
    protected void bindDetailHolder(DetailViewHolder holder){
        holder.setHeaderColor(Color.parseColor(mCategory.getColor()));
        setReward(holder);
    }

    private void setReward(DetailViewHolder holder){
        if (mReward.isFortune()){
            holder.setTitle(R.string.reward_fortune_header);
        }
        else if (mReward.isFunFact()){
            holder.setTitle(R.string.reward_fact_header);
        }
        else if (mReward.isJoke()){
            holder.setTitle(R.string.reward_joke_header);
        }
        else if (mReward.isQuote()){
            holder.setTitle(R.string.reward_quote_header);
        }
        holder.setDescription(mReward.format());
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.goal_yes:
                mListener.acceptGoal();
                break;

            case R.id.goal_no:
                mListener.declineGoal();
                break;
        }
    }

    private void fetchReward(){
        mGetRewardRC = HttpRequest.get(this, API.URL.getRandomReward());
    }

    @Override
    public void onRequestComplete(int requestCode, String result){
        if (requestCode == mGetRewardRC){
            Parser.parse(result, ParserModels.RewardResultSet.class, this);
        }
    }

    @Override
    public void onRequestFailed(int requestCode, HttpRequestError error){

    }

    @Override
    public void onProcessResult(int requestCode, ResultSet result){

    }

    @Override
    public void onParseSuccess(int requestCode, ResultSet result){
        if (result instanceof ParserModels.RewardResultSet){
            mReward = ((ParserModels.RewardResultSet)result).results.get(0);
            notifyDetailsInserted();
            updateLoading(false);
        }
    }

    @Override
    public void onParseFailed(int requestCode){

    }

    public Button getSignMeUpButton(){
        return mSignMeUp;
    }


    /**
     * Listener interface for the adapter.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    public interface GoalListener{
        /**
         * Called when the goal card has loaded.
         */
        void onGoalCardLoaded();

        /**
         * Called when the user taps the 'yes, I'm in' button.
         */
        void acceptGoal();

        /**
         * Called when the user taps the 'not now' button.
         */
        void declineGoal();
    }
}
