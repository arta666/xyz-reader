package com.example.xyzreader.ui.activity;


import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.adapter.ArticleListAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.databinding.ActivityArticleListBinding;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import static com.example.xyzreader.app.Constant.EXTRA_CURRENT_ARTICLE_POSITION;
import static com.example.xyzreader.app.Constant.EXTRA_STARTING_ARTICLE_POSITION;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener, ArticleListAdapter.ItemListener {

    private static final String TAG = ArticleListActivity.class.getSimpleName();

    ActivityArticleListBinding binding;

    private Bundle mTmpReenterState;
    public boolean mIsRefreshing;
    public static boolean mIsDetailsActivityStarted;

    @SuppressWarnings("NewApi")
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null) {
                int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
                int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
                if (startingPosition != currentPosition) {
                    // If startingPosition != currentPosition the user must have swiped to a
                    // different page in the DetailsActivity. We must update the shared element
                    // so that the correct one falls into place.
                    String newTransitionName = getString(R.string.transition_photo) + currentPosition;
                    View newSharedElement = binding.recyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }

                mTmpReenterState = null;
            } else {
                // If mTmpReenterState is null, then the activity is exiting.
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };
    private ArticleListAdapter mAdapter;
    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArticleListBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        setUpActionBar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(mCallback);
        }

        binding.swipeRefreshLayout.setOnRefreshListener(this);

        LoaderManager.getInstance(this).initLoader(0, null, this);

        if (savedInstanceState == null) {
            startService(new Intent(this, UpdaterService.class));
        }

        IntentFilter filter = new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);

        filter.addAction(UpdaterService.BROADCAST_ACTION_NO_CONNECTIVITY);

        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshingReceiver,
                filter);

    }

    private void setUpActionBar() {
        setSupportActionBar(binding.toolbar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                binding.swipeRefreshLayout.setRefreshing(mIsRefreshing);
            } else if (UpdaterService.BROADCAST_ACTION_NO_CONNECTIVITY.equals(intent.getAction())) {
                binding.swipeRefreshLayout.setRefreshing(false);
                showSnackBar(getString(R.string.empty_recycler_view_no_network));
            }
        }
    };


    private void showSnackBar(String message){
        mSnackbar = Snackbar.make(binding.getRoot(),message,Snackbar.LENGTH_LONG);
        mSnackbar.setActionTextColor(ActivityCompat.getColor(this,R.color.color_accent));
        mSnackbar.setAction("Ok", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSnackbar.dismiss();
            }
        });
        if (mSnackbar.isShown()){
            mSnackbar.dismiss();
        }
        mSnackbar.show();

    }

    @NotNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NotNull Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter = new ArticleListAdapter(this);
        mAdapter.setCursor(cursor);
        mAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(mAdapter);

        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(layoutManager);

        mAdapter.setItemListener(this);
    }

    @Override
    public void onLoaderReset(@NotNull Loader<Cursor> loader) {
        binding.recyclerView.setAdapter(null);
    }

    @Override
    public void onRefresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }


    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
        int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
        if (startingPosition != currentPosition) {
            binding.recyclerView.scrollToPosition(currentPosition);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportPostponeEnterTransition();
            binding.recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    binding.recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    binding.recyclerView.requestLayout();
                    supportStartPostponedEnterTransition();
                    return true;
                }
            });
        }
    }


    @Override
    public void onClickListener(View view, int position) {

        Bundle bundle = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            bundle = ActivityOptions.makeSceneTransitionAnimation(
                    this,(ImageView)view,((ImageView)view).getTransitionName()
            ).toBundle();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW,ItemsContract.Items.buildItemUri(mAdapter.getItemId(position)));
        intent.putExtra(EXTRA_STARTING_ARTICLE_POSITION, position);
        if (bundle != null) {
            startActivity(intent, bundle);
        }else {
            startActivity(intent);
        }
    }
}
