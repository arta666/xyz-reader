package com.example.xyzreader.ui.fragment;


import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.palette.graphics.Palette;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.databinding.FragmentArticleDetailBinding;
import com.example.xyzreader.ui.activity.ArticleDetailActivity;
import com.example.xyzreader.ui.activity.ArticleListActivity;
import com.example.xyzreader.util.DrawInsetsFrameLayout;
import com.example.xyzreader.util.ImageLoaderHelper;
import com.example.xyzreader.util.ObservableScrollView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Objects;

import static com.example.xyzreader.app.Constant.EXTRA_CURRENT_ARTICLE_POSITION;
import static com.example.xyzreader.app.Constant.EXTRA_STARTING_ARTICLE_POSITION;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final String TAG = ArticleDetailFragment.class.getSimpleName();
    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private int mMutedColor = 0xFF333333;
    private int mTopInset;
    private int mCurrentPosition;
    private int mStartingPosition;
    private boolean mIsTransitioning;
    private long mBackgroundImageFadeMillis;
    private boolean mIsCard = false;

    FragmentArticleDetailBinding binding;
    private String articleTitle;
    private String articleTime;
    private String articleBody;

    public ArticleDetailFragment() {
    }


    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(EXTRA_CURRENT_ARTICLE_POSITION, position);
        arguments.putInt(EXTRA_STARTING_ARTICLE_POSITION, startingPosition);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        mStartingPosition = getArguments().getInt(EXTRA_STARTING_ARTICLE_POSITION);
        mCurrentPosition = getArguments().getInt(EXTRA_CURRENT_ARTICLE_POSITION);
        mIsTransitioning = savedInstanceState == null && mStartingPosition == mCurrentPosition;
        mBackgroundImageFadeMillis = getResources().getInteger(
                R.integer.fragment_details_background_image_fade_millis);

        if (Build.VERSION.SDK_INT >= 21) {
            Objects.requireNonNull(getActivity()).getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LoaderManager.getInstance(this).initLoader(0, null, this);
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentArticleDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        setUpActionBar();

        bindViews();

    }

    private void setUpActionBar() {
        ((AppCompatActivity) Objects.requireNonNull(getActivity()))
                .setSupportActionBar(binding.detailToolbar);
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            Objects.requireNonNull(((AppCompatActivity) getActivity())
                    .getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            Objects.requireNonNull(((AppCompatActivity) getActivity())
                    .getSupportActionBar()).setDisplayShowTitleEnabled(false);

        }
    }

    private void bindViews() {

        binding.articleByline.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {

            String photo = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            articleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            articleTime = Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)).toString();
            articleBody = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)).toString();
            binding.articleTitle.setText(articleTitle);
            binding.articleByline.setText(articleTime);
            binding.thumbnail.setContentDescription(articleTitle + articleTime);
            binding.articleBody.setText(articleBody);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.thumbnail.setTransitionName(getString(R.string.transition_photo) + mCurrentPosition);
            }
            binding.shareFab.setOnClickListener(this);


            // Load the image with Glide to prevent OOM error when the image drawables are very large.
            Glide.with(this)
                    .asBitmap()
                    .load(photo)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                startEnterTransition();
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap bitmap, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                startEnterTransition();
                            }
                            if (bitmap != null) {
                                Palette p = Palette.from(bitmap).generate();
                                mMutedColor = p.getDarkMutedColor(0xFF424242);
//                                binding.thumbnail.setImageBitmap(bitmap);
                                binding.metaBar.setBackgroundColor(mMutedColor);
                            }
                            return false;
                        }
                    })
                    .into(binding.thumbnail);


        }
    }


    private void startEnterTransition(){
        if (mCurrentPosition == mStartingPosition) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.thumbnail.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        binding.thumbnail.getViewTreeObserver().removeOnPreDrawListener(this);
                        Objects.requireNonNull(getActivity()).supportStartPostponedEnterTransition();
                        return true;
                    }
                });
            }
        }
    }

    @NotNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NotNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NotNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        binding = null;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: in fragment");
        if (item.getItemId() == android.R.id.home) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Objects.requireNonNull(getActivity()).supportFinishAfterTransition();
                return true;
            }else {
                Objects.requireNonNull(getActivity()).finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    public ImageView getAlbumImage() {
        if (isViewInBounds(Objects.requireNonNull(getActivity()).getWindow().getDecorView(), binding.thumbnail)) {
            return binding.thumbnail;
        }
        return null;
    }

    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.share_fab) {
            handleFabClick();
        }
    }

    private void handleFabClick() {
        if (TextUtils.isEmpty(articleTitle) || TextUtils.isEmpty(articleBody)) {
            return;
        }
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(Objects.requireNonNull(getActivity()))
                .setType("text/plain")
                .setText(articleTitle + "/n" + articleTime + "/n" + articleBody)
                .getIntent(), getString(R.string.action_share)));
    }
}