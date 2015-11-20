package org.tndata.android.compass.adapter.feed;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import org.tndata.android.compass.R;
import org.tndata.android.compass.model.Action;
import org.tndata.android.compass.util.CompassUtil;

import java.util.Calendar;

import at.grabner.circleprogress.CircleProgressView;
import at.grabner.circleprogress.TextMode;


/**
 * View holder for the up next card.
 *
 * @author Ismael Alonso
 * @version 1.1.0
 */
final class UpNextHolder extends MainFeedViewHolder implements View.OnClickListener{
    //Header
    private TextView mHeader;
    private View mOverflow;

    //Indicator
    private CircleProgressView mIndicator;

    //No actions content
    private View mNoActionsContainer;
    private TextView mNoActionsTitle;
    private TextView mNoActionsSubtitle;

    //Action content
    private View mContentContainer;
    private TextView mAction;
    private TextView mGoal;

    //Footer
    private TextView mIndicatorCaption;
    private TextView mTime;


    /**
     * Constructor.
     *
     * @param adapter a reference to the adapter that will handle the holder.
     * @param rootView the root view held by the holder.
     */
    UpNextHolder(MainFeedAdapter adapter, View rootView){
        super(adapter, rootView);

        mHeader = (TextView)rootView.findViewById(R.id.up_next_header);
        mOverflow = rootView.findViewById(R.id.up_next_overflow_box);

        mIndicator = (CircleProgressView)rootView.findViewById(R.id.up_next_indicator);

        mNoActionsContainer = rootView.findViewById(R.id.up_next_no_actions);
        mNoActionsTitle = (TextView)rootView.findViewById(R.id.up_next_no_actions_title);
        mNoActionsSubtitle = (TextView)rootView.findViewById(R.id.up_next_no_actions_subtitle);

        mContentContainer = rootView.findViewById(R.id.up_next_content);
        mAction = (TextView)rootView.findViewById(R.id.up_next_action);
        mGoal = (TextView)rootView.findViewById(R.id.up_next_goal);

        mIndicatorCaption = (TextView)rootView.findViewById(R.id.up_next_indicator_caption);
        mTime = (TextView)rootView.findViewById(R.id.up_next_time);

        rootView.setOnClickListener(this);
        mOverflow.setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        mAdapter.setSelectedItem(getAdapterPosition());
        switch (view.getId()){
            case R.id.up_next_overflow_box:
                mAdapter.showActionPopup(view, getAdapterPosition());
                break;

            default:
                if (CardTypes.hasUpNextAction()){
                    mAdapter.mListener.onActionSelected(mAdapter.getDataHandler().getUpNext());
                }
        }
    }

    /**
     * Binds an action to the holder.
     *
     * @param action the action to be bound to the holder.
     */
    void bind(@Nullable Action action){
        if (action == null){
            mOverflow.setVisibility(View.GONE);
            mNoActionsContainer.setVisibility(View.VISIBLE);
            mContentContainer.setVisibility(View.GONE);

            if (mAdapter.getDataHandler().getTotalActions() != 0){
                mHeader.setText(R.string.card_up_next_header_completed);
                mNoActionsTitle.setText(R.string.card_up_next_title_completed);
                mNoActionsSubtitle.setText(R.string.card_up_next_subtitle_completed);
            }
            else{
                mHeader.setText(R.string.card_up_next_header);
                mNoActionsTitle.setText(R.string.card_up_next_title_empty);
                mNoActionsSubtitle.setText(R.string.card_up_next_subtitle_empty);
            }
            mTime.setText("");
        }
        else{
            mOverflow.setVisibility(View.VISIBLE);
            mNoActionsContainer.setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);

            mHeader.setText(R.string.card_up_next_header);
            mAction.setText(action.getTitle());
            //TODO this is a workaround
            if (action.getPrimaryGoal() != null){
                String goalTitle = action.getPrimaryGoal().getTitle().substring(0, 1).toLowerCase();
                goalTitle += action.getPrimaryGoal().getTitle().substring(1);
                mGoal.setText(mAdapter.mContext.getString(R.string.card_up_next_goal_title, goalTitle));
            }
            else{
                mGoal.setText("");
            }
            mTime.setText(action.getNextReminderDate());
        }

        mIndicator.setAutoTextSize(true);
        mIndicator.setValue(mAdapter.getDataHandler().getProgress());
        mIndicator.setTextMode(TextMode.TEXT);
        mIndicator.setValueAnimated(0, mAdapter.getDataHandler().getProgress(), 1500);
        mIndicatorCaption.setText(mAdapter.mContext.getString(R.string.card_up_next_indicator_caption,
                mAdapter.getDataHandler().getProgressFraction()));

        Calendar calendar = Calendar.getInstance();
        String month = CompassUtil.getMonthString(calendar.get(Calendar.MONTH) + 1);
        mIndicator.setText(month + " " + calendar.get(Calendar.DAY_OF_MONTH));
    }
}