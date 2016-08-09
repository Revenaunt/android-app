package org.tndata.android.compass.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.adapter.DrawerAdapter;
import org.tndata.android.compass.adapter.feed.MainFeedAdapter;
import org.tndata.android.compass.databinding.ActivityFeedBinding;
import org.tndata.android.compass.model.Action;
import org.tndata.android.compass.model.CustomGoal;
import org.tndata.android.compass.model.FeedData;
import org.tndata.android.compass.model.Goal;
import org.tndata.android.compass.model.TDCGoal;
import org.tndata.android.compass.model.UpcomingAction;
import org.tndata.android.compass.model.UserGoal;
import org.tndata.android.compass.util.API;
import org.tndata.android.compass.util.CompassUtil;
import org.tndata.android.compass.util.DataSynchronizer;
import org.tndata.android.compass.util.FeedDataLoader;
import org.tndata.android.compass.util.GcmRegistration;
import org.tndata.android.compass.util.ParallaxEffect;

import java.util.List;

import es.sandwatch.httprequests.HttpRequest;


/**
 * The application's main activity. Contains a feed, a drawer, and a floating
 * action button. In the future it will also include a search functionality.
 *
 * The feed displays up next cards, reward cards, and goal cards.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class FeedActivity
        extends AppCompatActivity
        implements
                SwipeRefreshLayout.OnRefreshListener,
                DrawerAdapter.OnItemClickListener,
                MainFeedAdapter.Listener,
                View.OnClickListener,
                FeedDataLoader.Callback{

    //Activity request codes
    private static final int ACTION_RC = 4582;
    private static final int GOAL_RC = 3486;
    private static final int GOAL_SUGGESTION_RC = 8962;
    private static final int SETTINGS_RC = 6542;


    //A reference to the application class and the binding object
    private CompassApplication mApplication;
    private ActivityFeedBinding mBinding;

    //Drawer and feed components
    private ActionBarDrawerToggle mDrawerToggle;
    private MainFeedAdapter mAdapter;

    //Flags
    private boolean mSuggestionDismissed;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_feed);

        mApplication = (CompassApplication)getApplication();

        //Synchronize cached data with the back-end. For now, this is a one-way street.
        DataSynchronizer.sync(getApplicationContext());

        //Update the timezone and register with GCM
        HttpRequest.put(null, API.URL.putUserProfile(mApplication.getUser()),
                API.BODY.putUserProfile(mApplication.getUser()));
        new GcmRegistration(this);

        //Set up the toolbar
        mBinding.feedToolbar.setTitle("");
        setSupportActionBar(mBinding.feedToolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Set up the drawer
        mDrawerToggle = new ActionBarDrawerToggle(this, mBinding.feedDrawerLayout,
                R.string.nav_drawer_action, R.string.nav_drawer_action){
            @Override
            public void onDrawerClosed(View view){
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView){
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        mBinding.feedDrawerLayout.addDrawerListener(mDrawerToggle);

        mBinding.feedDrawer.setLayoutManager(new LinearLayoutManager(this));
        mBinding.feedDrawer.setAdapter(new DrawerAdapter(this, this));
        mBinding.feedDrawer.addItemDecoration(DrawerAdapter.getItemPadding(this));

        //Refresh functionality
        mBinding.feedRefresh.setColorSchemeColors(0xFFFF0000, 0xFFFFE900, 0xFF572364);
        mBinding.feedRefresh.setOnRefreshListener(this);

        //Create the adapter and set the feed
        mAdapter = new MainFeedAdapter(this, this, false);

        RecyclerView feed = mBinding.feedList;
        feed.setAdapter(mAdapter);
        feed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        feed.addOnScrollListener(new ParallaxEffect(mBinding.feedIllustration, 0.5f));

        ParallaxEffect toolbarEffect = new ParallaxEffect(mBinding.feedToolbar, 1);
        toolbarEffect.setParallaxCondition(new ParallaxEffect.ParallaxCondition(){
            @Override
            protected boolean doParallax(){
                int height = (int)((CompassUtil.getScreenWidth(FeedActivity.this) * 2 / 3) * 0.55);
                return -getRecyclerViewOffset() > height;
            }

            @Override
            protected int getFixedState(){
                return CompassUtil.getPixels(FeedActivity.this, 10);
            }

            @Override
            protected int getParallaxViewOffset(){
                int height = (int)((CompassUtil.getScreenWidth(FeedActivity.this) * 2 / 3) * 0.55);
                return height + getFixedState() + getRecyclerViewOffset();
            }
        });
        feed.addOnScrollListener(toolbarEffect);

        feed.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                if (dy > 0){
                    mBinding.feedMenu.hideMenuButton(true);
                }
                else if (dy < 0){
                    mBinding.feedMenu.showMenuButton(true);
                }
                if (recyclerView.canScrollVertically(-1)){
                    mBinding.feedRefresh.setEnabled(false);
                }
                else{
                    mBinding.feedRefresh.setEnabled(true);
                }
            }
        });

        //FAB menu related init
        Animation hideAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        hideAnimation.setDuration(200);
        Animation showAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        showAnimation.setDuration(200);

        mBinding.feedMenu.setMenuButtonHideAnimation(hideAnimation);
        mBinding.feedMenu.setMenuButtonShowAnimation(showAnimation);
        mBinding.feedMenu.setClosedOnTouchOutside(true);
        mBinding.feedMenu.setOnMenuButtonClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (!mBinding.feedMenu.isOpened()){
                    animateBackground(true);
                }
                mBinding.feedMenu.toggle(true);
            }
        });
        mBinding.feedMenu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener(){
            @Override
            public void onMenuToggle(boolean opened){
                if (!opened){
                    animateBackground(false);
                }
            }
        });

        //Set up the FAB menu items
        populateMenu();

        mSuggestionDismissed = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        if (mBinding.feedDrawerLayout.isDrawerOpen(GravityCompat.START)){
            searchItem.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public final boolean onOptionsItemSelected(MenuItem item){
        if (mDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        switch (item.getItemId()){
            case R.id.search:
                startActivity(new Intent(this, SearchActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Creates the FAB menu.
     */
    private void populateMenu(){
        for (int i = 0; i < 2; i++){
            ContextThemeWrapper ctx = new ContextThemeWrapper(this, R.style.MenuButtonStyle);
            FloatingActionButton fab = new FloatingActionButton(ctx);
            fab.setColorNormalResId(R.color.accent);
            fab.setColorPressedResId(R.color.accent);
            fab.setColorRippleResId(R.color.accent);
            fab.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (i == 0){
                fab.setId(R.id.fab_search_goals);
                fab.setLabelText(getString(R.string.fab_search_goals));
                fab.setImageResource(R.drawable.ic_search);
            }
            else if (i == 1){
                fab.setId(R.id.fab_browse_goals);
                fab.setLabelText(getString(R.string.fab_browse_goals));
                fab.setImageResource(R.drawable.ic_list_white_24dp);
            }
            fab.setOnClickListener(this);
            mBinding.feedMenu.addMenuButton(fab);
        }
    }

    @Override
    public void onClick(View v){
        mBinding.feedMenu.toggle(true);
        switch (v.getId()){
            case R.id.fab_search_goals:
                search();
                break;

            case R.id.fab_browse_goals:
                browseGoals();
                break;
        }
    }

    /**
     * Called when the search goals FAB is clicked.
     */
    private void search(){
        startActivityForResult(new Intent(this, SearchActivity.class), GOAL_RC);
    }

    /**
     * Called when the browse Goals FAB is clicked.
     */
    private void browseGoals(){
        startActivityForResult(new Intent(this, ChooseCategoryActivity.class), GOAL_RC);
    }

    /**
     * Creates the fade in/out effect over the FAB menu background.
     *
     * @param opening true if the menu is opening, false otherwise.
     */
    private void animateBackground(final boolean opening){
        AlphaAnimation animation;
        if (opening){
            animation = new AlphaAnimation(0, 1);
        }
        else{
            animation = new AlphaAnimation(1, 0);
        }
        //Start the animation
        animation.setDuration(300);
        animation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation){
                mBinding.feedStopper.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation){
                if (!opening){
                    mBinding.feedStopper.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation){
                //Unused
            }
        });
        mBinding.feedStopper.startAnimation(animation);
    }

    @Override
    public void onBackPressed(){
        //Order: drawer, FAB menu, application
        if (mBinding.feedDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mBinding.feedDrawerLayout.closeDrawers();
        }
        else if (mBinding.feedMenu.isOpened()){
            mBinding.feedMenu.toggle(true);
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onItemClick(int position){
        switch (position){
            case DrawerAdapter.MY_ACTIVITIES:
                startActivity(new Intent(this, MyActivitiesActivity.class));
                break;

            case DrawerAdapter.MYSELF:
                startActivity(new Intent(this, UserProfileActivity.class));
                break;

            case DrawerAdapter.AWARDS:
                startActivity(new Intent(this, AwardsActivity.class));
                break;

            case DrawerAdapter.PLACES:
                startActivity(new Intent(this, PlacesActivity.class));
                break;

            case DrawerAdapter.SETTINGS:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_RC);
                break;

            case DrawerAdapter.TOUR:
                startActivity(new Intent(this, TourActivity.class));
                break;

            case DrawerAdapter.SUPPORT:
                //Ask the user to open their default email client
                Intent emailIntent = new Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{"feedback@tndata.org"})
                        .putExtra(Intent.EXTRA_SUBJECT, getText(R.string.action_support_subject))
                        .setType("text/plain");
                try{
                    startActivity(Intent.createChooser(emailIntent, getText(R.string.action_support_share_title)));
                }
                catch (ActivityNotFoundException anfx){
                    Toast.makeText(FeedActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
                }
                break;

            case DrawerAdapter.DRAWER_COUNT:
                //Debug button
                startActivity(new Intent(this, PlaygroundActivity.class));
                break;
        }
        mBinding.feedDrawerLayout.closeDrawers();
    }

    @Override
    public void onNullData(){
        startActivity(new Intent(this, LauncherActivity.class));
        finish();
    }

    @Override
    public void onInstructionsSelected(){
        ((LinearLayoutManager)mBinding.feedList.getLayoutManager())
                .scrollToPositionWithOffset(mAdapter.getGoalsPosition(), 10);
    }

    @Override
    public void onSuggestionDismissed(){
        mSuggestionDismissed = true;
    }

    @Override
    public void onSuggestionSelected(TDCGoal goal){

    }

    @Override
    public void onGoalSelected(Goal goal){
        if (goal instanceof UserGoal){
            if (mApplication.getAvailableCategories().get(((UserGoal)goal).getPrimaryCategoryId()) != null){
                Intent reviewActionsIntent = new Intent(this, ReviewActionsActivity.class)
                        .putExtra(ReviewActionsActivity.USER_GOAL_KEY, goal);
                startActivityForResult(reviewActionsIntent, GOAL_RC);
            }
            else{
                Toast.makeText(this, "The content is currently unavailable", Toast.LENGTH_SHORT).show();
            }
        }
        else if (goal instanceof CustomGoal){
            Intent editGoal = new Intent(this, CustomContentActivity.class)
                    .putExtra(CustomContentActivity.CUSTOM_GOAL_KEY, goal);
            startActivityForResult(editGoal, GOAL_RC);
        }
    }

    @Override
    public void onStreaksSelected(List<FeedData.Streak> streaks){
        if(streaks != null && streaks.size() > 0){
            Toast.makeText(this, R.string.toast_streaks, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActionSelected(UpcomingAction action){
        Intent actionIntent = new Intent(this, ActionActivity.class)
                .putExtra(ActionActivity.UPCOMING_ACTION_KEY, action);
        startActivityForResult(actionIntent, ACTION_RC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //TODO transition from RESULT_OK to custom result codes
        if (requestCode == GOAL_RC){
            if (resultCode == CustomContentActivity.GOAL_REMOVED_RC){
                CustomGoal customGoal = data.getParcelableExtra(CustomContentActivity.REMOVED_GOAL_KEY);
                mAdapter.notifyGoalRemoved(mApplication.removeGoal(customGoal));
                Toast.makeText(this, R.string.goal_removed_toast, Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == ReviewActionsActivity.GOAL_REMOVED_RC){
                UserGoal userGoal = data.getParcelableExtra(ReviewActionsActivity.REMOVED_GOAL_KEY);
                mAdapter.notifyGoalRemoved(mApplication.removeGoal(userGoal));
                Toast.makeText(this, R.string.goal_removed_toast, Toast.LENGTH_SHORT).show();
            }
            else{
                mAdapter.updateDataSet();
            }
        }
        if (resultCode == RESULT_OK){
            if (requestCode == GOAL_SUGGESTION_RC){
                mAdapter.dismissSuggestion();
            }
            else if (requestCode == ACTION_RC){
                if (data.getBooleanExtra(ActionActivity.DID_IT_KEY, false)){
                    Action action = data.getParcelableExtra(ActionActivity.ACTION_KEY);
                    mAdapter.didIt(mApplication.getFeedData().getAction(action));
                    mAdapter.updateUpcoming();
                }
                else{
                    mApplication.updateAction((Action)data.getParcelableExtra(ActionActivity.ACTION_KEY));
                    mAdapter.updateUpcoming();
                }
            }
        }
        else if (resultCode == SettingsActivity.LOGGED_OUT_RESULT_CODE){
            startActivity(new Intent(this, LauncherActivity.class));
            finish();
        }
    }

    @Override
    public void onRefresh(){
        FeedDataLoader.load(this);
    }

    @Override
    public void onFeedDataLoaded(@Nullable FeedData feedData){
        if (feedData != null){
            mApplication.setFeedData(feedData);

            mAdapter = new MainFeedAdapter(this, this, !mSuggestionDismissed);
            mBinding.feedList.setAdapter(mAdapter);

            mSuggestionDismissed = false;

            if (mBinding.feedRefresh.isRefreshing()){
                mBinding.feedRefresh.setRefreshing(false);
            }
        }
    }
}