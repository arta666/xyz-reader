package com.example.xyzreader.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.databinding.ListItemArticleBinding;
import com.example.xyzreader.util.ImageLoaderHelper;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {

    private Cursor mCursor;

    private Context mContext;

    private ItemListener itemListener;


    public ArticleListAdapter(Context context) {
        mContext = context;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull final ViewGroup parent, int viewType) {


        ListItemArticleBinding itemBinding = ListItemArticleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(itemBinding);
    }


    @Override
    public void onBindViewHolder(@NotNull final ViewHolder holder, int position) {
        holder.binder(position);
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
        notifyDataSetChanged();
    }

    public interface ItemListener {
        void onClickListener(View view, int position);
    }

    public void setItemListener(ItemListener itemListener) {
        this.itemListener = itemListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ListItemArticleBinding binding;

        public ViewHolder(@NonNull ListItemArticleBinding itemBinding) {
            super(itemBinding.getRoot());
            binding = itemBinding;

            binding.getRoot().setOnClickListener(this);

        }

        public void binder(int position) {

            mCursor.moveToPosition(position);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.thumbnail.setTransitionName(mContext.getString(R.string.transition_photo) + position);
            }
            binding.thumbnail.setTag(mContext.getString(R.string.transition_photo) + position);

            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            String subtitle = DateUtils.getRelativeTimeSpanString(
                    mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString();
            String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
            String image = mCursor.getString(ArticleLoader.Query.THUMB_URL);
            binding.articleTitle.setText(title);
            binding.articleSubtitle.setText(subtitle);
            binding.articleAuthor.setText(author);
            binding.thumbnail.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            ImageLoader loader = ImageLoaderHelper.getInstance(mContext).getImageLoader();
            binding.thumbnail.setImageUrl(image, loader);
            loader.get(image, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                    Bitmap bitmap = imageContainer.getBitmap();
                    if (bitmap != null) {
                        Palette p = Palette.from(bitmap).generate();
                        int mMutedColor = p.getDarkMutedColor(0xFF424242);
                        binding.getRoot().setBackgroundColor(mMutedColor);
                    }
                }

                @Override
                public void onErrorResponse(VolleyError volleyError) {

                }
            });

        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == binding.getRoot().getId()) {
                if (itemListener != null) {
                    itemListener.onClickListener(binding.thumbnail, getAdapterPosition());
                }
            }
        }
    }


}