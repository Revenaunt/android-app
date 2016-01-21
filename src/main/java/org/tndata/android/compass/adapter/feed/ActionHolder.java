package org.tndata.android.compass.adapter.feed;

import android.view.View;
import android.widget.TextView;

import org.tndata.android.compass.R;
import org.tndata.android.compass.model.UserAction;


/**
 * View holder for an action card.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
class ActionHolder extends MainFeedViewHolder implements View.OnClickListener{
    private TextView mAction;
    private TextView mGoal;
    private TextView mTime;


    /**
     * Constructor.
     *
     * @param adapter a reference to the adapter that will handle the holder.
     * @param rootView the root view held by the holder.
     */
    ActionHolder(MainFeedAdapter adapter, View rootView){
        super(adapter, rootView);

        mAction = (TextView)rootView.findViewById(R.id.action_title);
        mGoal = (TextView)rootView.findViewById(R.id.action_goal);
        mTime = (TextView)rootView.findViewById(R.id.action_time);

        rootView.setOnClickListener(this);
        rootView.findViewById(R.id.action_overflow_box).setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        mAdapter.setSelectedItem(getAdapterPosition());
        int index = getAdapterPosition()-(CardTypes.getUpcomingHeaderPosition()+1);
        UserAction action = mAdapter.getDataHandler().getUpcoming().get(index);
        switch (view.getId()){
            case R.id.action_overflow_box:
                mAdapter.showActionPopup(view, getAdapterPosition());
                break;

            default:
                mAdapter.mListener.onActionSelected(action);
        }
    }

    void bind(UserAction action){
        mAction.setText(action.getTitle());
        //TODO this shouldn't be happening
        if (action.getPrimaryGoal() != null){
            String goalTitle = action.getPrimaryGoal().getTitle().substring(0, 1).toLowerCase();
            goalTitle += action.getPrimaryGoal().getTitle().substring(1);
            mGoal.setText(mAdapter.mContext.getString(R.string.card_upcoming_goal_title, goalTitle));
        }
        else{
            mGoal.setText("");
        }
        mTime.setText(action.getNextReminderDate());
    }
}
