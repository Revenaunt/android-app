package org.tndata.android.compass.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;
import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.adapter.CustomContentManagerAdapter;
import org.tndata.android.compass.model.CustomAction;
import org.tndata.android.compass.model.CustomGoal;
import org.tndata.android.compass.parser.Parser;
import org.tndata.android.compass.parser.ParserModels;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.NetworkRequest;


/**
 * Activity used to create, edit, and delete custom goals and actions.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class CustomContentManagerActivity
        extends MaterialActivity
        implements
                View.OnClickListener,
                NetworkRequest.RequestCallback,
                Parser.ParserCallback,
                CustomContentManagerAdapter.CustomActionAdapterListener{

    private static final String TAG = "CustomContentManager";

    public static final String CUSTOM_GOAL_KEY = "org.tndata.compass.CreateGoal.Goal";
    public static final String CUSTOM_GOAL_TITLE_KEY = "org.tndata.compass.CreateGoal.GoalTitle";


    private CompassApplication mApplication;

    //Dataset and adapter
    private CustomGoal mCustomGoal;
    private CustomContentManagerAdapter mAdapter;

    //Request codes
    private int mAddGoalRequestCode;
    private int mAddActionRequestCode;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mApplication = (CompassApplication)getApplication();

        //Retrieve the data set and populate the UI accordingly
        mCustomGoal = (CustomGoal)getIntent().getSerializableExtra(CUSTOM_GOAL_KEY);
        if (mCustomGoal != null){
            Log.d(TAG, "Edit goal mode");

            mCustomGoal = (CustomGoal)mApplication.getUserData().getGoal(mCustomGoal);

            //Set the adapter
            mAdapter = new CustomContentManagerAdapter(this, mCustomGoal, this);
        }
        else{
            Log.d(TAG, "New goal mode");

            mAdapter = new CustomContentManagerAdapter(this, null, this);
        }
        setAdapter(mAdapter);
        setColor(getResources().getColor(R.color.grow_primary));
    }

    @Override
    public void onBackPressed(){
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v){
        /*switch (v.getId()){
            //When the user enters edition mode
            case R.id.create_goal_edit:
                //Make the goal title focusable and focus it
                mGoalTitle.setFocusable(true);
                mGoalTitle.setFocusableInTouchMode(true);
                mGoalTitle.requestFocus();
                //Put the cursor at the end and open the keyboard
                mGoalTitle.setSelection(mGoalTitle.getText().length());
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                        InputMethodManager.HIDE_IMPLICIT_ONLY);

                //Swap the edit button for the save button
                mEditGoal.setVisibility(View.GONE);
                mSaveGoal.setVisibility(View.VISIBLE);
                break;

            //When the user saves a currently existing goal (only from edition)
            case R.id.create_goal_save:
                //Grab the title and check it ain't empty
                String newTitle = mGoalTitle.getText().toString().trim();
                if (newTitle.length() > 0){
                    //If the title has changed, set it and send an update to the backend
                    if (!mCustomGoal.getTitle().equals(newTitle)){
                        mCustomGoal.setTitle(newTitle);
                        NetworkRequest.put(this, null, API.getPutCustomGoalUrl(mCustomGoal),
                                mApplication.getToken(), API.getPostPutCustomGoalBody(mCustomGoal));
                    }
                    //Hide the keyboard and make the title not focusable
                    InputMethodManager imm2 = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm2.hideSoftInputFromWindow(mGoalTitle.getWindowToken(), 0);
                    mGoalTitle.clearFocus();
                    mGoalTitle.setFocusable(false);

                    //Swap the save button for the edit button
                    mEditGoal.setVisibility(View.VISIBLE);
                    mSaveGoal.setVisibility(View.GONE);
                }
                break;

            //When the user adds a goal
            case R.id.create_goal_add:
                //Grab the title and check that it ain't empty
                String goalTitle = mGoalTitle.getText().toString().trim();
                if (goalTitle.length() > 0){
                    //Send a request to the backend and disable the add button and the title field
                    mAddGoalRequestCode = NetworkRequest.post(this, this, API.getPostCustomGoalUrl(),
                            mApplication.getToken(), API.getPostPutCustomGoalBody(new CustomGoal(goalTitle)));
                    mGoalTitle.setEnabled(false);
                    mAddGoal.setEnabled(false);
                }
                break;

            //When the user deletes a goal
            case R.id.create_goal_delete:
                //Remove it from the dataset, send a DELETE request, and exit the activity
                mApplication.removeGoal(mCustomGoal);
                NetworkRequest.delete(this, null, API.getDeleteGoalUrl(mCustomGoal),
                        mApplication.getToken(), new JSONObject());
                setResult(RESULT_OK);
                finish();
                break;
        }*/
    }

    @Override
    public void onRequestComplete(int requestCode, String result){
        if (requestCode == mAddGoalRequestCode){
            Parser.parse(result, CustomGoal.class, this);
        }
        else if (requestCode == mAddActionRequestCode){
            Parser.parse(result, CustomAction.class, this);
        }
    }

    @Override
    public void onRequestFailed(int requestCode, String message){

    }

    @Override
    public void onProcessResult(int requestCode, ParserModels.ResultSet result){
        if (result instanceof CustomGoal){
            ((CustomGoal)result).init();
            mApplication.getUserData().addGoal((CustomGoal)result);
        }
        else if (result instanceof CustomAction){
            ((CustomAction)result).init();
            mApplication.getUserData().addAction((CustomAction)result);
        }
    }

    @Override
    public void onParseSuccess(int requestCode, ParserModels.ResultSet result){
        if (result instanceof CustomGoal){
            /*mCustomGoal = (CustomGoal)result;
            mGoalTitle.setEnabled(true);
            mGoalTitle.clearFocus();
            mGoalTitle.setFocusable(false);

            mAddGoal.setVisibility(View.GONE);
            mEditGoal.setVisibility(View.VISIBLE);
            mDeleteGoal.setVisibility(View.VISIBLE);
            mActionContainer.setVisibility(View.VISIBLE);

            mAdapter = new CustomContentManagerAdapter(this, this, mCustomGoal.getActions());
            LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(llm);
            mRecyclerView.setAdapter(mAdapter);
        }
        else if (result instanceof CustomAction){
            mAdapter.customActionAdded();*/
        }
    }

    @Override
    public void onSaveAction(CustomAction customAction){
        NetworkRequest.put(this, null, API.getPutCustomActionUrl(customAction),
                mApplication.getToken(), API.getPostPutCustomActionBody(customAction, mCustomGoal));
    }

    @Override
    public void onAddClicked(CustomAction customAction){
        mAddActionRequestCode = NetworkRequest.post(this, this, API.getPostCustomActionUrl(),
                mApplication.getToken(), API.getPostPutCustomActionBody(customAction, mCustomGoal));
    }

    @Override
    public void onRemoveClicked(CustomAction customAction){
        NetworkRequest.delete(this, this, API.getDeleteActionUrl(customAction),
                mApplication.getToken(), new JSONObject());
        mApplication.getUserData().removeAction(customAction);
    }

    @Override
    public void onEditTrigger(CustomAction customAction){
        startActivity(new Intent(this, TriggerActivity.class)
                .putExtra(TriggerActivity.GOAL_KEY, mCustomGoal)
                .putExtra(TriggerActivity.ACTION_KEY, customAction));
    }
}
