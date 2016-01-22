package org.tndata.android.compass.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import org.tndata.android.compass.CompassApplication;
import org.tndata.android.compass.R;
import org.tndata.android.compass.adapter.ChooseCategoryAdapter;
import org.tndata.android.compass.model.Category;


/**
 * Intermediate step to choose goals. Lets the user pick the category to choose goals from.
 *
 * @author Ismael Alonso
 * @version 1.0.0
 */
public class ChooseCategoryActivity extends AppCompatActivity implements ChooseCategoryAdapter.ChooseCategoryAdapterListener{
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_category);

        Toolbar toolbar = (Toolbar)findViewById(R.id.choose_category_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.choose_category_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new ChooseCategoryAdapter(this, this, ((CompassApplication)getApplication()).getPublicCategories()));
    }

    @Override
    public void onCategorySelected(Category category){
        startActivity(new Intent(this, ChooseGoalsActivity.class).putExtra(ChooseGoalsActivity.CATEGORY_KEY, category));
    }
}
