package com.example.xyzreader.ui;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.animation.ExpandingCardViewAnimation;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.views.NewspaperRolledView;

import org.w3c.dom.Text;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private int mFadeSleepTime = 16000;
    private int mWordBuffer = 200;//number of words we will display at a time
    private int mMaxChars = 0;
    private int mTotalPages = 0;
    private int mCurrentPage = 0;
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private int mTextPostion = 0;
    Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
    Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
    Animation mNewspaperClipSelectedAnimation;
    Animation mNewspaperClipSlideDownAnimation;
    Adapter mAdapter;
    String mbodyText;
    boolean mEndsInSpace = true;
    CoordinatorLayout mMainContainer;
    private Thread mTextSwitcherThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        final View toolbarContainerView = findViewById(R.id.toolbar_container);
        mMainContainer = (CoordinatorLayout) findViewById(R.id.main_coordinator_layout);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        fadeIn.setDuration(2000);
        fadeOut.setDuration(1000);

        mRecyclerView.setItemAnimator(null);
        mNewspaperClipSelectedAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.newspaper_selected_anim);
        mNewspaperClipSlideDownAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.newspaper_unselected_stories_slide_out);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                    final int index = i;
                    View v = mRecyclerView.getChildAt(i);
                    v.findViewById(R.id.article_body).startAnimation(fadeIn);
                    mRecyclerView.invalidate();
                    ArticleListActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            mAdapter.notifyItemChanged(index);
                        }
                    });
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });


        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        setTextFadeThread();
    }

    private void setTextFadeThread(){
        mTextSwitcherThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(mFadeSleepTime);
                        mTextPostion += mWordBuffer;

                        while (mEndsInSpace) {
                            if (mbodyText.charAt(mTextPostion) != ' ') {
                                mTextPostion += 1;
                            } else {
                                mEndsInSpace = false;
                            }
                        }

                        if (mTextPostion > mMaxChars - mWordBuffer) {
                            mTextPostion = 0;
                            mCurrentPage = -1;
                        }

                        ArticleListActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                                    View v = mRecyclerView.getChildAt(i);
                                    v.findViewById(R.id.article_body).startAnimation(fadeOut);
                                }
                                mCurrentPage = mCurrentPage + 1;
                            }
                        });

                        mEndsInSpace = true;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        mTextSwitcherThread.start();
    }

    private void addStoryToLibrary(){

    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver, new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter = new Adapter(this,cursor);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;
        private Context mContext;

        public Adapter(Context context,Cursor cursor) {
            mContext = context;
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    mTextSwitcherThread.interrupt(); // interrupt text cycle thread as we go to detail views
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View v = mRecyclerView.getChildAt(i);
                        v.findViewById(R.id.article_body).startAnimation(fadeOut);
                        v.setAnimation(mNewspaperClipSlideDownAnimation);
                        v.startAnimation(mNewspaperClipSlideDownAnimation);
                    }
                    ExpandingCardViewAnimation animation = new ExpandingCardViewAnimation(getApplicationContext(),parent,view);
                    animation.configureAnimation();
                    view.clearAnimation();
                    view.setAnimation(mNewspaperClipSelectedAnimation);
                    view.startAnimation(mNewspaperClipSelectedAnimation);

                    mNewspaperClipSlideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            String transitionName = getString(R.string.transition_list_item);
                            Activity activity = (Activity) mContext;
                            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, mRecyclerView,transitionName);
                            ActivityCompat.startActivity(activity, new Intent(Intent.ACTION_VIEW, ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))), options.toBundle());
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            mbodyText = mCursor.getString(ArticleLoader.Query.BODY);
            mMaxChars = mbodyText.length();
            mTotalPages = mMaxChars/mWordBuffer;
            holder.bodyView.setText(mbodyText.substring(mTextPostion, mTextPostion + mWordBuffer));

            holder.pagesview.removeAllViews();
            for(int i = 0; i < mTotalPages; i++){
                TextView mPageText = new TextView(getApplicationContext());
                mPageText.setLayoutParams(new RelativeLayout.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                mPageText.setText(Integer.toString(i + 1));
                mPageText.setPadding(10,10,10,0);
                mPageText.setTextSize(19);

                if(mCurrentPage == i)
                    mPageText.setTextColor(getResources().getColor(R.color.highlited_page_color));
                else
                    mPageText.setTextColor(getResources().getColor(R.color.blue_page_number_color));

                holder.pagesview.addView(mPageText);
            }

            holder.libraryfabbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addStoryToLibrary();
                    Snackbar addToLibrarySnackbar = Snackbar.make(mMainContainer,getString(R.string.story_added_to_library),Snackbar.LENGTH_LONG);
                    addToLibrarySnackbar.show();
                }
            });

            holder.facebooklikebutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO This button should share this newspaper
                }
            });
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public TextView bodyView;
        public LinearLayout pagesview;
        public ImageButton libraryfabbutton;
        public ImageButton facebooklikebutton;

        public ViewHolder(final View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            bodyView = (TextView) view.findViewById(R.id.article_body);
            pagesview = (LinearLayout) view.findViewById(R.id.newspaper_clip_pages);
            libraryfabbutton = (ImageButton) view.findViewById(R.id.add_to_lib_fab);
            facebooklikebutton = (ImageButton) view.findViewById(R.id.face_book_like_button);
        }
    }
}
