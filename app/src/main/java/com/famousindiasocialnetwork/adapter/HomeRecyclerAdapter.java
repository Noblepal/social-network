package com.famousindiasocialnetwork.adapter;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.activity.DetailHomeItemActivity;
import com.famousindiasocialnetwork.activity.UserProfileDetailActivity;
import com.famousindiasocialnetwork.fragment.CommentsFragment;
import com.famousindiasocialnetwork.listener.OnCommentAddListener;
import com.famousindiasocialnetwork.listener.OnPopupMenuItemClickListener;
import com.famousindiasocialnetwork.model.LikeDislikeScoreUpdate;
import com.famousindiasocialnetwork.model.Post;
import com.famousindiasocialnetwork.model.UserMeta;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.LikeDislikeResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.EasyRecyclerViewAdapter;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.LinkTransformationMethod;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.famousindiasocialnetwork.util.SpringAnimationHelper;
import com.famousindiasocialnetwork.view.SquareVideoView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.JsonObject;

import java.util.HashMap;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by mayank on 9/7/16.
 */
public class HomeRecyclerAdapter extends EasyRecyclerViewAdapter<Post> {
    private Fragment fragment;
    private Context context;
    private HashMap<String, LikeDislikeScoreUpdate> likeDislikeUpdateMap;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private DrService foxyService;
    private String title;

    public HomeRecyclerAdapter(Fragment fragment) {
        this.context = fragment.getContext();
        this.fragment = fragment;
        likeDislikeUpdateMap = new HashMap<>();
        sharedPreferenceUtil = new SharedPreferenceUtil(fragment.getContext());
        foxyService = ApiUtils.getClient().create(DrService.class);
    }

    @Override
    public int getItemViewType(int position) {
        if (getItemsListSize() > position)
            return getItem(position).getId().equalsIgnoreCase("add") ? 2 : super.getItemViewType(position);
        else
            return super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemView(ViewGroup parent, int viewType) {
        if (viewType == 2) {
            return new AddMobViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_add, parent, false));
        } else {
            return new HomeItemViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_home, parent, false));
        }
    }

    @Override
    public void onBindItemView(RecyclerView.ViewHolder commonHolder, Post currPost, int position) {
        if (commonHolder instanceof HomeItemViewHolder) {
            final HomeItemViewHolder holder = (HomeItemViewHolder) commonHolder;
            setPostData(holder, currPost);

            if (TextUtils.isEmpty(currPost.getTitle())) {
                holder.postTitle.setVisibility(View.GONE);
            } else {
                holder.postTitle.setVisibility(View.VISIBLE);
                holder.postTitle.setText(currPost.getTitle());
            }

            if (currPost.getUserMetaData() != null) {
                Glide.with(context).load(currPost.getUserMetaData().getImage())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(context, 8)))
                                .override(Helper.dp2px(context, 38), Helper.dp2px(context, 38)).placeholder(R.drawable.ic_person_gray_24dp)).into(holder.foxyImage);

            }

            switch (currPost.getType()) {
                case "text":
                    holder.videoActionContainer.setVisibility(View.GONE);
                    holder.postText.setVisibility(View.VISIBLE);
                    holder.imageView.setVisibility(View.GONE);
                    holder.videoView.setVisibility(View.GONE);
                    holder.postText.setText(currPost.getText());
                    if (!TextUtils.isEmpty(currPost.getMedia_url())) {
                        holder.imageView.setVisibility(View.VISIBLE);
                        Glide.with(context).load(currPost.getUserMetaData().getImage())
                                .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(context, 5))).override(Helper.dp2px(context, 38), Helper.dp2px(context, 38)).placeholder(R.drawable.placeholder))
                                .into(holder.foxyImage);

                        Glide.with(context)
                                .load(currPost.getMedia_url())
                                .apply(new RequestOptions().placeholder(R.drawable.placeholder).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate())
                                .into(holder.imageView);
                    }
                    break;
                case "image":
                    holder.videoActionContainer.setVisibility(View.GONE);
                    holder.postText.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.VISIBLE);
                    holder.videoView.setVisibility(View.GONE);
                    Glide.with(context)
                            .load(currPost.getMedia_url())
                            .apply(new RequestOptions().placeholder(R.drawable.placeholder).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate())
                            .into(holder.imageView);
                    break;
                case "video":
                    holder.videoActionContainer.setVisibility(View.VISIBLE);
                    holder.postText.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.GONE);
                    holder.videoView.setVisibility(View.VISIBLE);

                    holder.videoProgress.setVisibility(View.VISIBLE);
                    holder.videoAction.setVisibility(View.GONE);

                    String videoUrl = currPost.getMedia_url();
                    holder.videoView.setVideoURI(Uri.parse(videoUrl));
                    holder.videoView.setVideoPath(videoUrl);
                    holder.videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            holder.videoProgress.setVisibility(View.GONE);
                            return true;
                        }
                    });
                    holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            holder.videoProgress.setVisibility(View.GONE);
                            holder.videoAction.setVisibility(View.VISIBLE);
                            holder.videoView.seekTo(100);
                        }
                    });
                    holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            holder.videoAction.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play_circle_outline_36dp));
                        }
                    });
                    holder.videoAction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (holder.videoView.isPlaying()) {
                                holder.mediaStopPosition = holder.videoView.getCurrentPosition();
                                holder.videoView.pause();
                            } else {
                                holder.videoView.seekTo(holder.mediaStopPosition);
                                holder.videoView.start();
                            }
                            holder.videoAction.setImageDrawable(ContextCompat.getDrawable(context, holder.videoView.isPlaying() ? R.drawable.ic_pause_circle_outline_36dp : R.drawable.ic_play_circle_outline_36dp));
                        }
                    });

//                    String videoThumbUrl = currPost.getVideoThumbnailUrl();
//
//                    Glide.with(context)
//                            .load(videoThumbUrl)
//                            .apply(new RequestOptions().placeholder(R.drawable.placeholder).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate())
//                            .into(holder.imageView);
                    break;
            }
        } else if (commonHolder instanceof AddMobViewHolder) {
            ((AddMobViewHolder) commonHolder).setData();
        }
    }


    /**
     * Binds the post data to the views in proper format.
     *
     * @param holder   {@link HomeItemViewHolder}
     * @param currPost the {@link Post} object
     */
    private void setPostData(HomeItemViewHolder holder, Post currPost) {
        holder.postText.setTransformationMethod(new LinkTransformationMethod());
        holder.postText.setMovementMethod(LinkMovementMethod.getInstance());
        holder.postTitle.setTransformationMethod(new LinkTransformationMethod());
        holder.postTitle.setMovementMethod(LinkMovementMethod.getInstance());

        String dateOfPost = currPost.getCreatedAt();

        String commentString = "(" + currPost.getCommentCount() + " )";
        String dislikeString = "(" + currPost.getDislikeCount() + " )";
        String likeString = "(" + currPost.getLikeCount() + " )";

        holder.commentCount.setText(commentString);
        //holder.dislikeCount.setText(dislikeString);
        holder.likeCount.setText(likeString);
        holder.postedTime.setText("Posted " + Helper.timeDiff(dateOfPost));
        holder.userName.setText(currPost.getUserMetaData().getName());

        holder.setLikedView(currPost.getLiked() == 1);
        holder.setDislikedView(currPost.getDisliked() == 1);
    }

    class AddMobViewHolder extends RecyclerView.ViewHolder {
        private AdView mAdView;

        public AddMobViewHolder(View itemView) {
            super(itemView);
            mAdView = itemView.findViewById(R.id.adView);
        }


        public void setData() {
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("33BE2250B43518CCDA7DE426D04EE232").build();
            mAdView.loadAd(adRequest);
        }
    }

    class HomeItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView foxyImage;
        CardView cardView;
        TextView postedTime;
        TextView postText;
        TextView postTitle;
        ImageView imageView;
        View videoActionContainer;
        SquareVideoView videoView;
        LinearLayout commentNow;
        TextView userName, commentCount, likeCount;
        ImageView dislike;
        LinearLayout like;
        ImageView likeIcon;
        ImageView videoAction;
        ProgressBar videoProgress;

        int mediaStopPosition = 0;

//        @BindView(R.id.list_item_home_video_player_progress_bar)
//        ProgressBar progressBar;

        HomeItemViewHolder(View itemView) {
            super(itemView);
            foxyImage = itemView.findViewById(R.id.list_item_home_foxy_img);
            cardView = itemView.findViewById(R.id.cardView);
            postedTime = itemView.findViewById(R.id.list_item_home_posted_txt);
            postText = itemView.findViewById(R.id.list_item_home_text);
            postTitle = itemView.findViewById(R.id.list_item_home_title);
            imageView = itemView.findViewById(R.id.list_item_home_image);
            videoActionContainer = itemView.findViewById(R.id.videoActionContainer);
            videoView = itemView.findViewById(R.id.list_item_home_video);
            commentNow = itemView.findViewById(R.id.list_item_home_comment_now);
            userName = itemView.findViewById(R.id.list_item_home_posted_name);
            commentCount = itemView.findViewById(R.id.tvCommentCount);
            likeCount= itemView.findViewById(R.id.tvLikeCount);
            dislike = itemView.findViewById(R.id.list_item_home_dislike);
            like = itemView.findViewById(R.id.list_item_home_like);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            videoAction = itemView.findViewById(R.id.videoAction);
            videoProgress = itemView.findViewById(R.id.videoProgress);

            itemView.findViewById(R.id.list_item_home_menu).setOnClickListener(this);
            itemView.findViewById(R.id.userDetailContainer).setOnClickListener(this);
            itemView.findViewById(R.id.list_item_home_share).setOnClickListener(this);
            itemView.findViewById(R.id.list_item_home_txt_pic_vid_holder).setOnClickListener(this);
            commentNow.setOnClickListener(this);
            dislike.setOnClickListener(this);
            like.setOnClickListener(this);
        }

        /**
         * A function used to share the post on clicking the share button
         */
        void sharePost() {
            int pos = getLayoutPosition();
            if (pos != -1) {
                final Post post = getItem(getLayoutPosition());
                Branch.BranchLinkCreateListener branchLinkCreateListener = new Branch.BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        if (error == null) {
                            Log.i("BRANCH SDK", "got my Branch link to share: " + url);
                            Helper.openShareIntent(context, itemView, url);
                            foxyService.updateSharePost(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), post.getId()).enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                                }

                                @Override
                                public void onFailure(Call<JsonObject> call, Throwable t) {

                                }
                            });
                        }
                    }
                };
                Helper.sharePost(context, post, branchLinkCreateListener);
            }
        }

        /**
         * Opens the {@link CommentsFragment} for particular post on click of comment button
         */
        void commentPopUp() {
            int pos = getLayoutPosition();
            if (pos != -1) {
                final Post currPost = getItem(pos);
                String postId = currPost.getId();

                OnPopupMenuItemClickListener onPopupMenuItemClickListener = new OnPopupMenuItemClickListener() {
                    @Override
                    public void onReportNowClick() {

                    }

                    @Override
                    public void onDeleteClick() {
                        currPost.setCommentCount(currPost.getCommentCount() - 1);
                        String commentString = String.valueOf(currPost.getCommentCount()) + " " + context.getString(R.string.commented);
                        //commentCount.setText(commentString);
                    }
                };

                OnCommentAddListener onCommentAddListener = new OnCommentAddListener() {
                    @Override
                    public void onCommentAdded() {
                        currPost.setCommentCount(currPost.getCommentCount() + 1);
                        String commentString = String.valueOf(currPost.getCommentCount()) + " " + context.getString(R.string.commented);
                        //commentCount.setText(commentString);
                    }
                };

                CommentsFragment commentsFragment = CommentsFragment.newInstance(postId, onPopupMenuItemClickListener, onCommentAddListener);

                ((AppCompatActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down, R.anim.bottom_up, R.anim.bottom_down)
                        .add(R.id.activity_main_container, commentsFragment, CommentsFragment.class.getName())
                        .addToBackStack(null)
                        .commit();
            }
        }

        void onDislikeClick() {
            final int position = getLayoutPosition();
            if (position != -1) {
                final Post currPost = getItem(position);

                boolean alreadyDisliked = currPost.getDisliked() == 1;
                currPost.setDisliked(alreadyDisliked ? 0 : 1);

                Intent bookmarkEventIntent = new Intent(Constants.BOOKMARK_EVENT);
                bookmarkEventIntent.putExtra("post", currPost);
                LocalBroadcastManager.getInstance(context).sendBroadcast(bookmarkEventIntent);

                Intent postChangeEventIntent = new Intent(Constants.POST_CHANGE_EVENT);
                postChangeEventIntent.putExtra("post", currPost);
                LocalBroadcastManager.getInstance(context).sendBroadcast(postChangeEventIntent);
            }
        }

        void onLikeClick() {
            final int position = getLayoutPosition();
            if (position != -1) {
                final Post currPost = getItem(position);

                boolean alreadyLiked = currPost.getLiked() == 1;
                currPost.setLiked(alreadyLiked ? 0 : 1);
                currPost.setLikeCount(alreadyLiked ? (currPost.getLikeCount() - 1) : (currPost.getLikeCount() + 1));
                Intent postChangeEventIntent = new Intent(Constants.POST_CHANGE_EVENT);
                postChangeEventIntent.putExtra("post", currPost);
                LocalBroadcastManager.getInstance(context).sendBroadcast(postChangeEventIntent);
                if (!likeDislikeUpdateMap.containsKey(currPost.getId())) {
                    likeDislikeUpdateMap.put(currPost.getId(), new LikeDislikeScoreUpdate());
                }
                likeDislikeUpdateMap.get(currPost.getId()).setLike(alreadyLiked ? -1 : 1);
                executeLike(currPost.getId());
            }
        }

        void setDislikedView(boolean disliked) {
            //dislike.setTypeface(null, disliked ? Typeface.BOLD : Typeface.NORMAL);
            //dislike.setTextColor(ContextCompat.getColor(context, disliked ? R.color.colorAccent : R.color.colorText));
            //dislike.setCompoundDrawablesWithIntrinsicBounds(disliked ? R.drawable.ic_bookmark_blue_18dp : R.drawable.ic_bookmark_gray_18dp, 0, 0, 0);
            dislike.setImageDrawable(ContextCompat.getDrawable(context, disliked ? R.drawable.ic_bookmark_blue_18dp : R.drawable.ic_bookmark_gray_18dp));
        }

        void setLikedView(boolean liked) {
//            like.setTypeface(null, liked ? Typeface.BOLD : Typeface.NORMAL);
//            like.setTextColor(ContextCompat.getColor(context, liked ? R.color.colorPrimary : R.color.colorText));
            likeIcon.setImageResource(liked ? R.drawable.ic_m_like_active : R.drawable.ic_m_like);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.userDetailContainer:
                    UserMeta userMeta = getItem(getLayoutPosition()).getUserMetaData();
                    context.startActivity(UserProfileDetailActivity.newInstance(context, userMeta.getId(), userMeta.getName(), userMeta.getImage()));
                    break;
                case R.id.list_item_home_share:
                    SpringAnimationHelper.performAnimation(view);
                    sharePost();
                    break;
                case R.id.list_item_home_comment_now:
                    SpringAnimationHelper.performAnimation(view);
                    commentPopUp();
                    break;
                case R.id.list_item_home_dislike:
                    SpringAnimationHelper.performAnimation(view);
                    onDislikeClick();
                    break;
                case R.id.list_item_home_like:
                    SpringAnimationHelper.performAnimation(view);
                    onLikeClick();
                    break;
                case R.id.list_item_home_menu:
                    final int pos = getLayoutPosition();
                    if (pos != -1) {
                        final Post post = getItem(pos);
                        UserResponse userMe = Helper.getLoggedInUser(sharedPreferenceUtil);
                        SpringAnimationHelper.performAnimation(view);
                        PopupMenu popup = new PopupMenu(context, view);
                        popup.inflate(R.menu.menu_home_item);
                        popup.getMenu().getItem(0).setVisible(post.getUserMetaData().getId().equals(userMe.getId()));
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    /*case R.id.action_report:
                                        //Toast.makeText(context, "You reported this post", Toast.LENGTH_SHORT).show();
                                        break;*/
                                    case R.id.action_delete:
                                        deletePost(post.getId());
                                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                                        removeItemAt(pos);
                                        break;
                                }
                                return false;
                            }
                        });
                        //displaying the popup
                        popup.show();
                    }
                    break;
                case R.id.list_item_home_txt_pic_vid_holder:
                    int posi = getLayoutPosition();
                    if (posi != -1)
                        context.startActivity(DetailHomeItemActivity.newIntent(context, getItem(posi)));
                    break;
            }
        }
    }

    private void deletePost(String id) {
        foxyService.deletePost(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), id).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                response.isSuccessful();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                t.getMessage();
            }
        });
    }

    private void executeDislike(String id) {
        if (!likeDislikeUpdateMap.get(id).isInProgress()) {
            likeDislikeUpdateMap.get(id).setInProgress(true);
            foxyService.updatePostDislike(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), id).enqueue(new Callback<LikeDislikeResponse>() {
                @Override
                public void onResponse(Call<LikeDislikeResponse> call, Response<LikeDislikeResponse> response) {
                    if (response.isSuccessful()) {
                        likeDislikeUpdateMap.get(response.body().getId()).setInProgress(false);
                        if (likeDislikeUpdateMap.get(response.body().getId()).getDislike() != response.body().getStatus()) {
                            executeDislike(response.body().getId());
                        }
                    }
                }

                @Override
                public void onFailure(Call<LikeDislikeResponse> call, Throwable t) {

                }
            });
        }
    }

    private void executeLike(String id) {
        if (!likeDislikeUpdateMap.get(id).isInProgress()) {
            likeDislikeUpdateMap.get(id).setInProgress(true);
            foxyService.updatePostLike(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), id).enqueue(new Callback<LikeDislikeResponse>() {
                @Override
                public void onResponse(Call<LikeDislikeResponse> call, Response<LikeDislikeResponse> response) {
                    if (response.isSuccessful()) {
                        likeDislikeUpdateMap.get(response.body().getId()).setInProgress(false);
                        if (likeDislikeUpdateMap.get(response.body().getId()).getLike() != response.body().getStatus()) {
                            executeLike(response.body().getId());
                        }
                    }
                }

                @Override
                public void onFailure(Call<LikeDislikeResponse> call, Throwable t) {
                }
            });
        }
    }
}
