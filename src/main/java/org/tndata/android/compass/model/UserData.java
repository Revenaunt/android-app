package org.tndata.android.compass.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * This class encapsulates all of the user-selected content from the online REST api:
 *
 * - Categories
 * - Goals
 * - Behaviors
 * - Actions
 * - Places
 *
 * It includes methods to set & get those values, and keeps a consistently updated
 * hierarchy (i.e. Behaviors know about their parent goals & child actions, etc).
 *
 * @author Brad Montgomery
 * @author Ismael Alonso
 */
public class UserData{
    private static final String TAG = "UserData";


    //User selected content. There are the master lists. References to all the objects in
    //  the data structure are placed here. All other lists to be used within this class
    //  should contain references to these same exact objects, not references to other
    //  equivalent objects. This makes easier the access to and modification of the data
    //  structure.
    private List<Category> mCategories = new ArrayList<>();
    private List<Goal> mGoals = new ArrayList<>();
    private List<Behavior> mBehaviors = new ArrayList<>();
    private List<Action> mActions = new ArrayList<>();

    //User places
    private List<Place> mPlaces = new ArrayList<>();

    //Data for the main feed
    private FeedData mFeedData;


    /*--------------------------*
     * CATEGORY RELATED METHODS *
     *--------------------------*/

    /**
     * Sets the list of user selected categories and synchronizes them with the rest of the content.
     *
     * @param categories the list of categories selected by the user.
     */
    public void setCategories(List<Category> categories){
        setCategories(categories, true);
    }

    /**
     * Sets the list of user selected categories.
     *
     * @param categories the list of categories selected by the user.
     * @param sync true if the categories should be synchronized, false otherwise.
     */
    public void setCategories(List<Category> categories, boolean sync){
        if(categories != null && !categories.isEmpty()){
            mCategories = categories;
        }
        if(sync){
            assignGoalsToCategories();
        }
    }

    /**
     * Returns the list of user-selected Categories.
     *
     * @return an ArrayList of Category objects
     */
    public List<Category> getCategories(){
        return mCategories;
    }

    /**
     * Returns the original copy of the provided category.
     *
     * @param category the category whose original copy needs to be fetched.
     * @return the original copy of such category.
     */
    public Category getCategory(Category category){
        int index = mCategories.indexOf(category);
        if (index != -1){
            return mCategories.get(index);
        }
        return null;
    }

    /**
     * Returns all of the goals for a given Category. This is a wrapper for category.getGoals().
     *
     * @return a List of Goal objects.
     */
    public List<Goal> getCategoryGoals(Category category){
        return getCategory(category).getGoals();
    }

    /**
     * Adds a Category to the list of user-selected categories (if it's not
     * already included).
     *
     * Adding a Category also assigns any user-selected goals to the new Category.
     *
     * @param category the Category object to add.
     */
    public void addCategory(Category category){
        if(!mCategories.contains(category)){
            mCategories.add(category);
            assignGoalsToCategories();
        }
    }

    /**
     * Remove a single Category from the user's collection. This also removes the
     * reference to that category from all Goals.
     *
     * @param category the category to remove
     */
    public void removeCategory(Category category){
        mCategories.remove(category);

        List<Goal> toRemove = new ArrayList<>();
        //Remove this category from any child goals
        for (Goal goal:mGoals){
            goal.removeCategory(category);
            //Record all the Goals w/o parent Categories
            if (goal.getCategories().isEmpty()){
                toRemove.add(goal);
            }
        }
        //Remove all the Goals w/o parent Categories
        for (Goal goal:toRemove){
            removeGoal(goal);
        }
    }


    /*----------------------*
     * GOAL RELATED METHODS *
     *----------------------*/

    /**
     * Sets the list of user selected goals and synchronizes them with the rest of the content.
     *
     * @param goals the list of goals selected by the user.
     */
    public void setGoals(List<Goal> goals){
        setGoals(goals, true);
    }

    /**
     * Sets the list of user selected goals.
     *
     * @param goals the list of goals selected by the user.
     * @param sync true if the goals should be synchronized, false otherwise.
     */
    public void setGoals(List<Goal> goals, boolean sync){
        mGoals = goals;
        if (sync){
            assignGoalsToCategories();
        }
    }

    /**
     * Returns the list of user-selected Goals.
     *
     * @return a List of Goal objects.
     */
    public List<Goal> getGoals(){
        return mGoals;
    }

    /**
     * Returns the original copy of the provided goal.
     *
     * @param goal the goal whose original copy needs to be fetched.
     * @return the original copy of such goal.
     */
    public Goal getGoal(Goal goal){
        int index = mGoals.indexOf(goal);
        if (index != -1){
            return mGoals.get(index);
        }
        return null;
    }

    /**
     * Add a goal to the list of user selected Goals.
     *
     * Adding a Goal also assigns it to any existing user-selected categories,
     * as well as assigning any existing Behaviors to the Goal.
     *
     * @param goal the goal to be added to the user list.
     */
    public void addGoal(Goal goal){
        if(!mGoals.contains(goal)){
            mGoals.add(goal);
            assignGoalsToCategories();
            assignBehaviorsToGoals();
        }
    }

    /**
     * Removes a goal from the user's collection.
     *
     * Removing a goal also removes its reference from the parent Categories
     * as well as the child Behaviors.
     *
     * @param goal the goal to be removed from the user list.
     */
    public void removeGoal(Goal goal){
        mGoals.remove(goal);

        // Remove the goal from its parent categories
        for(Category category:mCategories){
            category.removeGoal(goal);
        }

        List<Behavior> toRemove = new ArrayList<>();
        //Remove the goal from its child Behaviors
        for(Behavior behavior:mBehaviors){
            behavior.removeGoal(goal);
            //Record all the Behaviors w/o parent Goals
            if (behavior.getGoals().isEmpty()){
                toRemove.add(behavior);
            }
        }
        //Remove Behaviors w/o parent Goals
        for (Behavior behavior:toRemove){
            removeBehavior(behavior);
        }
    }


    /*--------------------------*
     * BEHAVIOR RELATED METHODS *
     *--------------------------*/

    /**
     * Sets the list of user selected behaviors and synchronizes them with the rest of the content.
     *
     * @param behaviors the list of behaviors selected by the user.
     */
    public void setBehaviors(List<Behavior> behaviors){
        setBehaviors(behaviors, true);
    }

    /**
     * Sets the list of user selected behaviors.
     *
     * @param behaviors the list of behaviors selected by the user.
     * @param sync true if the behaviors should be synchronized, false otherwise.
     */
    public void setBehaviors(List<Behavior> behaviors, boolean sync){
        mBehaviors = behaviors;
        if (sync){
            assignBehaviorsToGoals();
        }
    }

    /**
     * Return the user-selected Behaviors.
     *
     * @return an ArrayList of Behavior objects.
     */
    public List<Behavior> getBehaviors(){
        return mBehaviors;
    }

    /**
     * Returns the original copy of the provided behavior.
     *
     * @param behavior the behavior whose original copy needs to be fetched.
     * @return the original copy of such behavior.
     */
    public Behavior getBehavior(Behavior behavior){
        int index = mBehaviors.indexOf(behavior);
        if (index != -1){
            return mBehaviors.get(index);
        }
        return null;
    }

    /**
     * Add a single Behavior to the user's collection.
     *
     * Adding this object will also trigger an update to behavior's parent goals.
     *
     * @param behavior the behavior to be added to the user's list.
     */
    public void addBehavior(Behavior behavior){
        if(!mBehaviors.contains(behavior)){
            Log.d("AddBehavior", "Added: " + behavior.getId());
            mBehaviors.add(behavior);
            assignBehaviorsToGoals();
        }
        else{
            Log.d("AddBehavior", "Not added: " + behavior.getId());
        }
    }

    /**
     * Remove a single Behavior from a user's collection. Doing this will also remove any
     * references to the Behavior from the parent Goal, and it will also remove the
     * child Actions.
     *
     * @param behavior the Behavior instance to remove.
     */
    public void removeBehavior(Behavior behavior){
        mBehaviors.remove(behavior);

        // Remove the behavior from any parent Goals
        for(Goal goal:getGoals()){
            goal.removeBehavior(behavior);
        }

        // Remove the child Actions
        List<Action> toRemove = new ArrayList<>();
        for (Action action:getActions()){
            if (action.getBehavior_id() == behavior.getId()){
                toRemove.add(action);
            }
        }
        mActions.removeAll(toRemove);
    }


    /*------------------------*
     * ACTION RELATED METHODS *
     *------------------------*/

    /**
     * Sets the list of user selected actions and synchronizes them with the rest of the content.
     *
     * @param actions the list of user selected actions.
     */
    public void setActions(List<Action> actions){
        setActions(actions, true);
    }

    /**
     * Sets the list of user selected actions.
     *
     * @param actions the list of actions selected by the user.
     * @param sync true if the actions should be synchronized, false otherwise.
     */
    public void setActions(List<Action> actions, boolean sync){
        mActions = actions;
        if (sync){
            assignActionsToBehaviors();
        }
    }

    /**
     * Retrieve a list of all user-selected Actions.
     *
     * @return an ArrayList of Action objects.
     */
    public List<Action> getActions(){
        return mActions;
    }

    /**
     * Returns the original copy of the provided action.
     *
     * @param action the action whose original copy needs to be fetched.
     * @return the original copy of such action.
     */
    public Action getAction(Action action){
        int index = mActions.indexOf(action);
        if (index != -1){
            return mActions.get(index);
        }
        return null;
    }

    /**
     *  Add an individual action the user's collection. Adding an action will also
     *  update the user's selected Behaviors, including a reference to the new
     *  action within any relevant parent Behavior.
     *
     * @param action the action to be added to the list.
     */
    public void addAction(Action action){
        if(!mActions.contains(action)){
            mActions.add(action);
            assignActionsToBehaviors();
        }
    }

    /**
     * Remove a single Action from a user's collection. Doing this will also remove
     * any reference to that Action from it's parent Behaviors.
     *
     * @param action the Action object to remove.
     */
    public void removeAction(Action action){
        mActions.remove(action);

        // Remove the action from any related behaviors
        for (Behavior behavior:mBehaviors){
            behavior.removeAction(action);
        }
    }


    /*-------------------------*
     * SYNCHRONIZATION METHODS *
     *-------------------------*/

    /**
     * Sync up all parent/child objects.
     */
    public void sync(){
        assignGoalsToCategories();
        assignBehaviorsToGoals();
        assignActionsToBehaviors();
        setActionParents();
    }

    /**
     * Goals are associated with one or more categories. This method will include
     * the user's selected goals within their selected Categories.
     */
    public void assignGoalsToCategories(){
        // add each goal to the correct category
        for (Category category:getCategories()){
            ArrayList<Goal> categoryGoals = new ArrayList<>();
            for (Goal goal:getGoals()){
                for (Category goalCategory:goal.getCategories()){
                    if (goalCategory.getId() == category.getId()){
                        categoryGoals.add(goal);
                        break;
                    }
                }
            }
            category.setGoals(categoryGoals);
        }
    }

    /**
     * Behaviors are contained within a Goal. This method will take the list of
     * selected behaviors, and associate them with the user's selected goals.
     */
    public void assignBehaviorsToGoals(){
        //Look at all the selected goals
        for (Goal goal:getGoals()){
            ArrayList<Behavior> goalBehaviors = new ArrayList<>();
            //Look at all the selected behaviors
            for (Behavior behavior:getBehaviors()){
                //The Behavior's parent goals
                for (Goal behaviorGoal:behavior.getGoals()){
                    if (behaviorGoal.getId() == goal.getId()){
                        goalBehaviors.add(behavior);
                        break;
                    }
                }
            }
            goal.setBehaviors(goalBehaviors);
        }
    }

    /**
     * Update a single Action in a user's collection; When some Action has been changed
     * (e.g. it's custom-trigger updated), this method will replace stale instances of
     * the Action with the new version.
     *
     * Updating an Action will also trigger an update to the parent behaviors.
     *
     * @param action the Action object to update.
     */
    public void updateAction(Action action){
        //Given a single action, find it in the list of Actions and keep the input version
        int i = 0;
        boolean found = false;
        for (Action a:mActions){
            if (a.getId() == action.getId()){
                found = true;
                break;
            }
            i++;
        }
        if(found){
            //Remove the old action
            removeAction(mActions.get(i));
        }
        mActions.add(action);
        assignActionsToBehaviors();
    }

    /**
     * Actions are contained within a single Behavior. This method will take the list
     * of selected actions, and associate them with the user-selected behaviors.
     *
     * This ensures that all behaviors have a valid reference to their child Actions.
     */
    public void assignActionsToBehaviors(){
        for (Behavior behavior:getBehaviors()){
            List<Action> behaviorActions = new ArrayList<>();
            for (Action action:getActions()){
                if (behavior.getId() == action.getBehavior_id()){
                    behaviorActions.add(action);
                }
            }
            behavior.setActions(behaviorActions);
        }

        // now, set each Action's parent Behavior.
        setActionParents();
    }

    /**
     * This method will set the Behavior attribute for all of the user's selected Actions, so the
     * Action will contain a valid reference to its parent.
     */
    public void setActionParents(){
        // set a reference to the parent for each action.
        for (Action action:getActions()){
            for (Behavior behavior:getBehaviors()){
                if (action.getBehavior_id() == behavior.getId()){
                    action.setBehavior(behavior);
                }
            }
        }
    }


    /*-----------------------*
     * PLACE RELATED METHODS *
     *-----------------------*/

    /**
     * Sets the list of user set places.
     *
     * @param places the list of places set by the user.
     */
    public void setPlaces(List<Place> places){
        mPlaces = places;
    }

    /**
     * Gets the list of user set places.
     *
     * @return the list of places set by te user.
     */
    public List<Place> getPlaces(){
        return mPlaces;
    }

    /**
     * Adds a place to the list of places set bu the user.
     *
     * @param place the place to be added.
     */
    public void addPlace(Place place){
        mPlaces.add(place);
    }


    /*---------------------------*
     * FEED DATA RELATED METHODS *
     *---------------------------*/

    /**
     * Sets the feed data retrieved from the API.
     *
     * @param feedData the object containing the information to be displayed in the feed.
     */
    public void setFeedData(FeedData feedData){
        mFeedData = feedData;
    }

    /**
     * Gets the feed data.
     *
     * @return the object containing the information to be displayed in the feed.
     */
    public FeedData getFeedData(){
        return mFeedData;
    }


    /*------------------------------------*
     * LOG RELATED METHODS. FOR DEBUGGING *
     *------------------------------------*/

    /**
     * Log the value of individual private data members.
     */
    public void logData() {
        Log.d(TAG, "Categories.");
        for (Category item:mCategories){
            Log.d(TAG, "- (" + item.getId() + ") " + item.getTitle());
            Log.d(TAG, "--> contains " + item.getGoals().size() + " goals");
        }
        Log.d(TAG, "Goals.");
        for (Goal item:mGoals){
            Log.d(TAG, "- (" + item.getId() + ") " + item.getTitle());
            Log.d(TAG, "--> contains " + item.getCategories().size() + " categories");
            Log.d(TAG, "--> contains " + item.getBehaviors().size() + " behaviors");
        }
        Log.d(TAG, "Behaviors.");
        for (Behavior item:mBehaviors){
            Log.d(TAG, "- (" + item.getId() + ") " + item.getTitle());
            Log.d(TAG, "--> contains " + item.getGoals().size() + " goals");
            Log.d(TAG, "--> contains " + item.getActions().size() + " actions");
        }
        Log.d(TAG, "Actions.");
        for (Action item:mActions){
            Log.d(TAG, "- (" + item.getId() + ") " + item.getTitle());
            Log.d(TAG, "--> contains, behavior_id = " + item.getBehavior_id());
        }
    }

    /**
     * Given a goal, Log information for it's parent Categories.
     *
     * @param goal the goal whose information needs to be logged.
     */
    public void logParentCategories(Goal goal){
        String output = "NONE";
        if (!goal.getCategories().isEmpty()){
            output = "";
            for (Category c:goal.getCategories()){
                output += "(" + c.getId() + ") " + c.getTitle() + ", ";
            }
        }
        Log.d(TAG, "- (parents) -> " + output);
    }

    /**
     * Given a behavior, log information for it's parent Goals.
     *
     * @param behavior the behavior whose information needs to be logged.
     */
    public void logParentGoals(Behavior behavior){
        String output = "NONE";
        if (!behavior.getGoals().isEmpty()){
            output = "";
            for (Goal g:behavior.getGoals()){
                output += "(" + g.getId() + ") " + g.getTitle() + ", ";
            }
        }
        Log.d(TAG, "-- (parents) ->" + output);
    }

    /**
     * Given an Action, log information for it's parent Behavior
     *
     * @param action the action whose information needs to be logged.
     */
    public void logParentBehavior(Action action){
        String output = "NONE";
        if (action.getBehavior() != null){
            output = "(" + action.getBehavior().getId() + ") " + action.getBehavior().getTitle();
        }
        Log.d(TAG, "--- (parent)-> " + output);
    }

    /**
     * Log the hierarchy of all User-selected data.
     */
    public void logSelectedData(){
        logSelectedData("User Data");
    }

    /**
     * Log the hierarchy of all User-selected data, with a custom title.
     *
     * @param title a custom title to display above the output.
     *
     */
    public void logSelectedData(String title){
        logSelectedData(title, true);
    }

    /**
     * Log the hierarchy of all User-selected data, with a custom title, and an option to
     * include each level's parent data.
     *
     * @param title a custom title to display above the output.
     * @param include_parents if true, will Log each item's parent objects.
     */
    public void logSelectedData(String title, boolean include_parents){
        // Log user-selected categories, goals, behaviors, actions
        Log.d(TAG, "------------- " + title + " --------------- ");
        for(Category c:getCategories()){
            Log.d(TAG, "CATEGORY: " + c.getTitle());
            for (Goal g:c.getGoals()){
                Log.d(TAG, "- GOAL: " + g.getTitle());
                if (include_parents){
                    logParentCategories(g);
                }
                for (Behavior b:g.getBehaviors()){
                    Log.d(TAG, "-- BEHAVIOR: " + b.getTitle());
                    if (include_parents){
                        logParentGoals(b);
                    }
                    for (Action a:b.getActions()){
                        Log.d(TAG, "--- ACTION: " + a.getTitle());
                        if (include_parents){
                            logParentBehavior(a);
                        }
                    }
                }
            }
        }
        Log.d(TAG, "------------------------------------------- ");
    }
}
