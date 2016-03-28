package org.tndata.android.compass.adapter.feed;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.tndata.android.compass.R;
import org.tndata.android.compass.model.FeedData;


/**
 * View holder for the feedback card.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
class FeedbackHolder extends MainFeedAdapter.ViewHolder implements View.OnClickListener{
    private FeedData.ActionFeedback mFeedback;

    private ImageView mIcon;
    private TextView mTitle;
    private TextView mSubtitle;


    /**
     * Constructor.
     *
     * @param adapter a reference to the adapter that will handle the holder.
     * @param rootView the root view held by this holder.
     */
    FeedbackHolder(MainFeedAdapter adapter, View rootView){
        super(adapter, rootView);

        mIcon = (ImageView)rootView.findViewById(R.id.card_feedback_icon);
        mTitle = (TextView)rootView.findViewById(R.id.card_feedback_title);
        mSubtitle = (TextView)rootView.findViewById(R.id.card_feedback_subtitle);

        rootView.setOnClickListener(this);
    }

    /**
     * Retrieves the values of the feedback from the feed data bundle.
     *
     * @param feedback the feed data bundle.
     */
    void bind(FeedData.ActionFeedback feedback){
        mFeedback = feedback;

        mIcon.setImageResource(feedback.getFeedbackIcon());
        mTitle.setText(feedback.getFeedbackTitle());
        mSubtitle.setText(feedback.getFeedbackSubtitle());
    }

    @Override
    public void onClick(View v){
        mAdapter.mListener.onFeedbackSelected(mFeedback);
    }
}
