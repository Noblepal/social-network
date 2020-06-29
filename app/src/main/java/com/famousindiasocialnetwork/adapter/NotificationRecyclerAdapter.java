package com.famousindiasocialnetwork.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.famousindiasocialnetwork.activity.DetailHomeItemActivity;
import com.famousindiasocialnetwork.model.Activity;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.model.Post;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.EasyRecyclerViewAdapter;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by mayank on 9/7/16.
 */
public class NotificationRecyclerAdapter extends EasyRecyclerViewAdapter<Activity> {
    private Context context;
    private DrService foxyService;
    private SharedPreferenceUtil sharedPreferenceUtil;

    public NotificationRecyclerAdapter(Context context) {
        this.context = context;
        this.foxyService = ApiUtils.getClient().create(DrService.class);
        this.sharedPreferenceUtil = new SharedPreferenceUtil(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemView(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_notification, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindItemView(RecyclerView.ViewHolder commonHolder, Activity currActivity, int position) {
        ViewHolder holder = (ViewHolder) commonHolder;
        String action = "";
        switch (currActivity.getType()) {
            case "like":
                action = " liked your post";
                break;
            case "comment":
                action = " commented on your post";
                break;
        }

        String statusText = currActivity.getUser_profile_id().getName() + action;

        holder.status.setText(statusText);
        holder.time.setText(Helper.timeDiff(String.valueOf(currActivity.getCreatedAt())));
        holder.progressBar.setVisibility(currActivity.isInProgress() ? View.VISIBLE : View.GONE);
        RequestOptions options = new RequestOptions();
        options.centerCrop().transform(new RoundedCorners(Helper.dp2px(context, 5)));
        Glide.with(context).load(currActivity.getUser_profile_id().getImage())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(context, 5))).override(Helper.dp2px(context, 35),
                        Helper.dp2px(context, 35)).placeholder(R.drawable.ic_person))
                .into(holder.descImg);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView descImg;
        TextView status;
        TextView time;
        ProgressBar progressBar;
        RelativeLayout list_item_notification_container;

        public ViewHolder(View itemView) {
            super(itemView);
            descImg = itemView.findViewById(R.id.list_item_notification_desc_img);
            status = itemView.findViewById(R.id.list_item_notification_status);
            time = itemView.findViewById(R.id.list_item_notification_time);
            progressBar = itemView.findViewById(R.id.progress);
            list_item_notification_container = itemView.findViewById(R.id.list_item_notification_container);

            list_item_notification_container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openDetailHomeItemFragment();
                }
            });
        }

        /**
         * Opens the {@link DetailHomeItemActivity} for showing the post in full screen with comments
         */
        void openDetailHomeItemFragment() {
            final int pos = getAdapterPosition();
            final Activity activity = getItem(pos);
            if (!activity.isInProgress()) {
                activity.setInProgress(true);
                notifyItemChanged(pos);
                foxyService.getPostById(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), activity.getPost_id()).enqueue(new Callback<Post>() {
                    @Override
                    public void onResponse(Call<Post> call, Response<Post> response) {
                        activity.setInProgress(false);
                        notifyItemChanged(pos);
                        if (response.isSuccessful()) {
                            context.startActivity(DetailHomeItemActivity.newIntent(context, response.body()));
                        }
                    }

                    @Override
                    public void onFailure(Call<Post> call, Throwable t) {
                        activity.setInProgress(false);
                        notifyItemChanged(pos);
                    }
                });
            }
        }

    }
}
