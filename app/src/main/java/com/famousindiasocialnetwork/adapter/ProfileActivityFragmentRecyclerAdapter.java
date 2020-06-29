package com.famousindiasocialnetwork.adapter;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.famousindiasocialnetwork.activity.DetailHomeItemActivity;
import com.famousindiasocialnetwork.model.Post;
import com.famousindiasocialnetwork.model.Activity;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.EasyRecyclerViewAdapter;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.google.gson.Gson;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by mayank on 15/7/16.
 */
public class ProfileActivityFragmentRecyclerAdapter extends EasyRecyclerViewAdapter<Activity> {
    private Context context;
    private DrService foxyService;
    private SharedPreferenceUtil sharedPreferenceUtil;

    public ProfileActivityFragmentRecyclerAdapter(Context context, ArrayList<Activity> activityArrayList) {
        this.context = context;
        this.foxyService = ApiUtils.getClient().create(DrService.class);
        this.sharedPreferenceUtil = new SharedPreferenceUtil(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemView(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_frag_profile_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindItemView(RecyclerView.ViewHolder commonHolder, Activity currActivity, int position) {

        ViewHolder holder = (ViewHolder) commonHolder;

        String statusText = "";

        switch (currActivity.getType()) {
            case "comment":
                statusText = "commented on your status";
                break;
            case "like":
                statusText = "liked your status";
                break;
            case "dislike":
                statusText = "disliked your status";
                break;
            case "comment_like":
                statusText = "liked your comment";
                break;
            case "comment_dislike":
                statusText = "disliked your comment";
                break;
        }


        String someone = "someone " + statusText;
        SpannableString spannableSomeone = new SpannableString(someone);

        if (currActivity.getUser_profile_id().getGender().equals("m")) {
            spannableSomeone.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary)),
                    0,
                    "someone".length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannableSomeone.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)),
                    0,
                    "someone".length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.status.setText(spannableSomeone);
        holder.time.setText(Helper.timeDiff(String.valueOf(currActivity.getCreatedAt())));
        holder.progressBar.setVisibility(currActivity.isInProgress() ? View.VISIBLE : View.GONE);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView status;
        TextView time;
        ProgressBar progressBar;

        public ViewHolder(View itemView) {
            super(itemView);
            status=itemView.findViewById(R.id.list_item_frag_profile_activity_status);
            time=itemView.findViewById(R.id.list_item_frag_profile_activity_time);
            progressBar=itemView.findViewById(R.id.progress);
            itemView.findViewById(R.id.list_item_frag_profile_activity_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openDetailHomeItemFragment();
                }
            });
        }


        /**
         * Opens the {@link DetailHomeItemActivity} for showing the post in full screen with comments
         */
        public void openDetailHomeItemFragment() {
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
                            Intent intent = new Intent(context, DetailHomeItemActivity.class);
                            String postJsonString = new Gson().toJson(response.body());
                            intent.putExtra("post", postJsonString);
                            context.startActivity(intent);
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
