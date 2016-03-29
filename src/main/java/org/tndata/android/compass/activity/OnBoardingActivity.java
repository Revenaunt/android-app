package org.tndata.android.compass.activity;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.adapter.ChooseInterestsAdapter;
import org.tndata.android.compass.fragment.ChooseInterestsFragment;
import org.tndata.android.compass.fragment.InstrumentFragment;
import org.tndata.android.compass.model.CategoryContent;
import org.tndata.android.compass.model.FeedData;
import org.tndata.android.compass.model.User;
import org.tndata.android.compass.model.UserCategory;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.Constants;
import org.tndata.android.compass.util.FeedDataLoader;

import java.util.List;

import es.sandwatch.httprequests.HttpRequest;
import es.sandwatch.httprequests.HttpRequestError;


/**
 * Activity to present the user the onboarding process, which includes a short survey and
 * a category picker.
 *
 * @author Edited by Ismael Alonso
 * @version 1.1.0
 */
public class OnBoardingActivity
        extends AppCompatActivity
        implements
                InstrumentFragment.InstrumentFragmentCallback,
                ChooseInterestsAdapter.OnCategoriesSelectedListener,
                HttpRequest.RequestCallback,
                FeedDataLoader.Callback{

    private static final int STAGE_PROFILE = 0;
    private static final int STAGE_CHOOSE_CATEGORIES = 1;


    private CompassApplication mApplication;
    private Fragment mFragment = null;

    //Request codes
    private int mInitialPostCategoryRC;
    private int mLastPostCategoryRC;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_toolbar);

        mApplication = (CompassApplication)getApplication();

        Toolbar toolbar = (Toolbar)findViewById(R.id.tool_bar);
        toolbar.setNavigationIcon(R.drawable.ic_back_white_24dp);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().hide();
        }

        swapFragments(STAGE_PROFILE);
    }

    /**
     * Replaces the current fragment for a new one.
     *
     * @param index the fragment id.
     */
    private void swapFragments(int index){
        switch (index){
            case STAGE_PROFILE:
                mFragment = InstrumentFragment.newInstance(Constants.INITIAL_PROFILE_INSTRUMENT_ID, 3);
                break;

            case STAGE_CHOOSE_CATEGORIES:
                mFragment = ChooseInterestsFragment.newInstance(true);
                break;
        }

        if (mFragment != null){
            getSupportFragmentManager().beginTransaction().replace(R.id.base_content, mFragment).commit();
        }
    }

    @Override
    public void onInstrumentFinished(int instrumentId){
        //When the user is done with the survey, he is taken to the category picker
        if (instrumentId == Constants.INITIAL_PROFILE_INSTRUMENT_ID){
            swapFragments(STAGE_CHOOSE_CATEGORIES);
        }
    }

    @Override
    public void onCategoriesSelected(List<CategoryContent> selection, List<UserCategory> original){
        //Process and log the selection, and save it
        //int[] categoryIds = new int[selection.size()];
        for (int i = 0; i < selection.size(); i++){
            if (i == 0){
                mInitialPostCategoryRC = HttpRequest.post(this, API.getUserCategoriesUrl(),
                        API.getPostCategoryBody(selection.get(i)));
                mLastPostCategoryRC = mInitialPostCategoryRC +selection.size();
            }
            else{
                CategoryContent cat = selection.get(i);
                HttpRequest.post(this, API.getUserCategoriesUrl(), API.getPostCategoryBody(cat));
            }
        }

        User user = mApplication.getUser();
        user.setOnBoardingComplete();
        HttpRequest.put(null, API.getPutUserProfileUrl(user), API.getPutUserProfileBody(user));
    }

    @Override
    public void onRequestComplete(int requestCode, String result){
        if (requestCode < mLastPostCategoryRC){
            mInitialPostCategoryRC++;
            if (mInitialPostCategoryRC == mLastPostCategoryRC){
                FeedDataLoader.load(this);
            }
        }
    }

    @Override
    public void onRequestFailed(int requestCode, HttpRequestError error){
        if (requestCode < mLastPostCategoryRC){
            mInitialPostCategoryRC++;
            if (mInitialPostCategoryRC == mLastPostCategoryRC){
                FeedDataLoader.load(this);
            }
        }
    }

    @Override
    public void onFeedDataLoaded(@Nullable FeedData feedData){
        if (feedData != null){
            mApplication.setFeedData(feedData);
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }
}
