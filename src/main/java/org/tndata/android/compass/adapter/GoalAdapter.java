package org.tndata.android.compass.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tndata.android.compass.R;
import org.tndata.android.compass.model.Action;
import org.tndata.android.compass.model.Behavior;
import org.tndata.android.compass.model.Goal;
import org.tndata.android.compass.model.Trigger;

import java.util.List;
import java.util.Stack;


/**
 * Adapter for the goal activity.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class GoalAdapter extends RecyclerView.Adapter{
    private static final int TYPE_SPACER = 0;
    private static final int TYPE_BEHAVIOR = 1;


    private Context mContext;
    private Goal mGoal;
    private int mSpacingRowHeight;


    /**
     * Constructor.
     *
     * @param context the application context.
     * @param goal the goal whose content is to be displayed.
     * @param spacingRowHeight the height of the header.
     */
    public GoalAdapter(@NonNull Context context, @NonNull Goal goal, int spacingRowHeight){
        mContext = context;
        mGoal = goal;
        mSpacingRowHeight = spacingRowHeight;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        if (viewType == TYPE_SPACER){
            return new RecyclerView.ViewHolder(new View(mContext)){};
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        return new BehaviorHolder(inflater.inflate(R.layout.item_behavior, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder rawHolder, int position){
        if (getItemViewType(position) == TYPE_SPACER){
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mSpacingRowHeight);
            rawHolder.itemView.setLayoutParams(params);
            rawHolder.itemView.setVisibility(View.INVISIBLE);
        }
        else{
            Behavior behavior = mGoal.getBehaviors().get(position-1);
            BehaviorHolder holder = (BehaviorHolder)rawHolder;
            holder.mTitle.setText(behavior.getTitle());
            populate(holder, behavior);
        }
    }

    @Override
    public int getItemCount(){
        return mGoal.getBehaviors().size()+1;
    }

    @Override
    public int getItemViewType(int position){
        if (position == 0){
            return TYPE_SPACER;
        }
        return TYPE_BEHAVIOR;
    }

    /**
     * Recycles and populates a row holder.
     *
     * @param holder the victim.
     * @param behavior the new behavior.
     */
    private void populate(BehaviorHolder holder, Behavior behavior){
        List<Action> actions = behavior.getActions();

        //Unnecessary holders (if any) are recycled
        while (actions.size() < holder.mActionContainer.getChildCount()){
            int last = holder.mActionContainer.getChildCount()-1;
            BehaviorHolder.holderPool.push(getActionHolder(holder.mActionContainer, last));
            holder.mActionContainer.removeViewAt(last);
        }

        //For each action
        for (int i = 0; i < actions.size(); i++){
            ActionHolder actionHolder;
            //If a holder already exists, retrieve it
            if (i < holder.mActionContainer.getChildCount()){
                actionHolder = getActionHolder(holder.mActionContainer, i);
            }
            //Otherwise, get a new one
            else{
                actionHolder = getActionHolder(holder.mActionContainer);
            }
            actionHolder.mTitle.setText(actions.get(i).getTitle());

            Trigger trigger = actions.get(i).getTrigger();
            String triggerText = trigger.getRecurrencesDisplay();
            String date = trigger.getFormattedDate();
            if (!date.equals("")){
                triggerText += " " + date;
            }
            String triggerDate = trigger.getFormattedTime();
            if (!triggerDate.equals("")){
                triggerText += " " + triggerDate;
            }
            actionHolder.mTime.setText(triggerText);

            holder.mActionContainer.addView(actionHolder.mItemView);
        }
    }

    /**
     * Gets the action holder at a given position within the passed container.
     *
     * @param container the object containing the holders.
     * @param position the position of the holder.
     * @return the holder.
     */
    private ActionHolder getActionHolder(ViewGroup container, int position){
        return (ActionHolder)container.getChildAt(position).getTag();
    }

    /**
     * Gets a new action holder, either from the stack or a brand new one.
     *
     * @param parent the view where it will be placed in the future.
     * @return the holder.
     */
    private ActionHolder getActionHolder(ViewGroup parent){
        if (!BehaviorHolder.holderPool.isEmpty()){
            return BehaviorHolder.holderPool.pop();
        }
        LayoutInflater inflater = LayoutInflater.from(mContext);
        return new ActionHolder(inflater.inflate(R.layout.item_action, parent, false));
    }


    /**
     * View holder for a behavior item.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    private static class BehaviorHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        //This is a pool of ActionHolders to be reused. Any unused ActionHolders should be
        //  placed here. Before creating new ones, the list should be checked to see if
        //  there are any of them available.
        private static Stack<ActionHolder> holderPool = new Stack<>();


        private TextView mTitle;
        private LinearLayout mActionContainer;


        /**
         * Constructor.
         *
         * @param itemView the root view.
         */
        public BehaviorHolder(View itemView){
            super(itemView);

            itemView.findViewById(R.id.behavior_overflow).setOnClickListener(this);
            mTitle = (TextView)itemView.findViewById(R.id.behavior_title);
            mActionContainer = (LinearLayout)itemView.findViewById(R.id.behavior_actions);
        }

        @Override
        public void onClick(View v){

        }
    }


    /**
     * View holder for an action item.
     *
     * @author Ismael Alonso
     * @version 1.0.0
     */
    private static class ActionHolder{
        private View mItemView;
        private TextView mTitle;
        private TextView mTime;


        /**
         * Constructor.
         *
         * @param itemView the root view.
         */
        public ActionHolder(View itemView){
            mItemView = itemView;
            mItemView.setTag(this);
            mTitle = (TextView)itemView.findViewById(R.id.action_title);
            mTime = (TextView)itemView.findViewById(R.id.action_time);
        }
    }
}
