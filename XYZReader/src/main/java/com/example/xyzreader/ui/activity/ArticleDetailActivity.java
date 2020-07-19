package com.example.xyzreader.ui.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.databinding.ActivityArticleDetailBinding;
import com.example.xyzreader.ui.fragment.ArticleDetailFragment;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static com.example.xyzreader.app.Constant.EXTRA_CURRENT_ARTICLE_POSITION;
import static com.example.xyzreader.app.Constant.EXTRA_STARTING_ARTICLE_POSITION;

public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    public final static String ARTICLE_ID = "ARTICLE_ID";

    private Cursor mCursor;
    private long mStartId;

    private ArticleDetailFragment mCurrentDetailsFragment;
    private boolean mIsReturning;
    private int mCurrentPosition;
    private int mStartingPosition;

    ActivityArticleDetailBinding binding;

    private ViewPagerAdapter mPagerAdapter;

    @SuppressWarnings("NewApi")
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReturning) {

                ImageView sharedElement = mCurrentDetailsFragment.getAlbumImage();
                if (sharedElement == null) {
                    // If shared element is null, then it has been scrolled off screen and
                    // no longer visible. In this case we cancel the shared element transition by
                    // removing the shared element from the shared elements map.
                    names.clear();
                    sharedElements.clear();
                } else if (mStartingPosition != mCurrentPosition) {
                    // If the user has swiped to a different ViewPager page, then we need to
                    // remove the old shared element and replace it with the new shared element
                    // that should be transitioned instead.
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArticleDetailBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();

        hideStatusBar();

//        ActionBar actionBar = getActionBar();
//        if (actionBar != null) {
//            actionBar.hide();
//        }
        setContentView(view);
        postEnter();



        mStartingPosition = getIntent().getIntExtra(EXTRA_STARTING_ARTICLE_POSITION, 0);
        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            }
            mCurrentPosition = mStartingPosition;
        } else {
            mStartId = savedInstanceState.getLong(ARTICLE_ID);
            mCurrentPosition = savedInstanceState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
        }

        // Replaced with getLoadManagerF
        LoaderManager.getInstance(this).initLoader(0, null, this);

        setUpViewPager();


    }

    private void hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);

        }
    }

    private void postEnter() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportPostponeEnterTransition();
            setEnterSharedElementCallback(mCallback);
        }
    }


    private void setUpViewPager() {
        mPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), 0);

        binding.pager.setAdapter(mPagerAdapter);
        binding.pager.setCurrentItem(mCurrentPosition);
        binding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    mCurrentPosition = position;
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARTICLE_ID, mStartId);
        outState.putInt(EXTRA_CURRENT_ARTICLE_POSITION, mCurrentPosition);
    }

    @Override
    public void supportFinishAfterTransition() {
        super.supportFinishAfterTransition();
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_STARTING_ARTICLE_POSITION, mStartingPosition);
        data.putExtra(EXTRA_CURRENT_ARTICLE_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
    }

    @NotNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NotNull Loader<Cursor> cursorLoader, Cursor cursor) {

        // Select the start ID
        if (mStartId > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (cursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = cursor.getPosition();
                    mCursor = cursor;
                    mPagerAdapter.notifyDataSetChanged();
                    binding.pager.setCurrentItem(position, false);
                    break;
                }
                cursor.moveToNext();
            }
        }
    }

    @Override
    public void onLoaderReset(@NotNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: in Activity");
        return true;
    }

    private class ViewPagerAdapter extends FragmentStatePagerAdapter {
        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }


        @Override
        public void setPrimaryItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailsFragment = (ArticleDetailFragment) object;
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID), position, mStartingPosition);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }


}
