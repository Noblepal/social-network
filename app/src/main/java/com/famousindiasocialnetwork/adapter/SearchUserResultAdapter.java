package com.famousindiasocialnetwork.adapter;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.activity.UserProfileDetailActivity;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Helper;

import java.util.ArrayList;

/**
 * Created by a_man on 09-02-2018.
 */

public class SearchUserResultAdapter extends RecyclerView.Adapter<SearchUserResultAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<UserResponse> dataList;
    private SearchUserActionClickListener listener;

    public SearchUserResultAdapter(Context context, ArrayList<UserResponse> users, SearchUserActionClickListener listener) {
        this.context = context;
        this.dataList = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_user_search_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setData(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView userName;
        private Button userAction;
        private ImageView userImage;

        MyViewHolder(View itemView) {
            super(itemView);
            userAction = itemView.findViewById(R.id.userAction);
            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.fullName);

            userAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onActionClick(dataList.get(getAdapterPosition()));
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    UserResponse user = dataList.get(getAdapterPosition());
                    context.startActivity(UserProfileDetailActivity.newInstance(context, user.getId(), user.getName(), user.getImage()));
                }
            });
        }

        public void setData(UserResponse user) {
            Glide.with(context).load(user.getImage())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(context, 8)))
                            .override(Helper.dp2px(context, 38), Helper.dp2px(context, 38))).into(userImage);
            userName.setText(user.getName());
            userName.setSelected(true);

            userAction.setTextColor(user.getIs_following() == 1 ? ContextCompat.getColor(context, android.R.color.black) : Color.WHITE);
            userAction.setBackground(ContextCompat.getDrawable(context, user.getIs_following() == 1 ? R.drawable.angular_grey_box : R.drawable.angular_pink_box));
            userAction.setText(user.getIs_following() == 1 ? "Following" : "Follow");
        }
    }

    public interface SearchUserActionClickListener {
        void onActionClick(UserResponse user);
    }
}
