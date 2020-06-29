package com.famousindiasocialnetwork.fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.famousindiasocialnetwork.adapter.NotificationRecyclerAdapter;
import com.famousindiasocialnetwork.model.Activity;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.BaseListModel;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Displays the notifications mimicking a popup
 */
public class NotificationFragment extends Fragment {
    RecyclerView recyclerView;
    LinearLayout emptyViewContainer;
    TextView empty_view_text;
    SwipeRefreshLayout swipeRefreshLayout;
    private NotificationRecyclerAdapter notificationAdapter;

    private DrService foxyService;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private boolean allDone, isLoading;
    private int pageNumber = 1;
    private Call<BaseListModel<Activity>> getActivities;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferenceUtil = new SharedPreferenceUtil(getContext());
        foxyService = ApiUtils.getClient().create(DrService.class);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        recyclerView = view.findViewById(R.id.fragment_notification_recycler_view);
        emptyViewContainer = view.findViewById(R.id.empty_view_container);
        empty_view_text = view.findViewById(R.id.empty_view_text);
        swipeRefreshLayout = view.findViewById(R.id.frag_home_feeds_swipe_refresh_layout);

        ViewCompat.setElevation(view, Helper.dpToPx(getContext(), 8));
        view.bringToFront();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pageNumber = 1;
                notificationAdapter.clear();
                allDone = false;
                loadPosts();
                recyclerView.setVisibility(View.VISIBLE);
                emptyViewContainer.setVisibility(View.GONE);
                //addInitialPosts(true);
            }
        });

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addOnScrollListener(recyclerViewOnScrollListener);
        notificationAdapter = new NotificationRecyclerAdapter(getContext());
        recyclerView.setAdapter(notificationAdapter);

        initialize();

        return view;
    }

    private void initialize() {
        loadPosts();
        recyclerView.setVisibility(View.VISIBLE);
        emptyViewContainer.setVisibility(View.GONE);
    }

    private void loadPosts() {
        isLoading = true;
        getActivities = foxyService.getActivities(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), pageNumber);
        getActivities.enqueue(callBack);
    }

    private Callback<BaseListModel<Activity>> callBack = new Callback<BaseListModel<Activity>>() {
        @Override
        public void onResponse(Call<BaseListModel<Activity>> call, Response<BaseListModel<Activity>> response) {
            isLoading = false;
            if (notificationAdapter.isLoaderShowing())
                notificationAdapter.hideLoading();
            if (swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(false);
            if (response.isSuccessful()) {
                BaseListModel<Activity> postResponse = response.body();
                if (postResponse.getData() == null || postResponse.getData().isEmpty()) {
                    allDone = true;
                    if (notificationAdapter.itemsList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyViewContainer.setVisibility(View.VISIBLE);
                        empty_view_text.setText("Nothing found..");
                    }
                } else {
                    notificationAdapter.addItemsAtBottom(postResponse.getData());
                }
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyViewContainer.setVisibility(View.VISIBLE);
                empty_view_text.setText("Nothing found..");
            }
        }

        @Override
        public void onFailure(Call<BaseListModel<Activity>> call, Throwable t) {
            isLoading = false;
            if (notificationAdapter.isLoaderShowing())
                notificationAdapter.hideLoading();
            if (swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(false);
            recyclerView.setVisibility(View.GONE);
            emptyViewContainer.setVisibility(View.VISIBLE);
            empty_view_text.setText("Something went wrong");
        }
    };

    private RecyclerView.OnScrollListener recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            // init
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            RecyclerView.Adapter adapter = recyclerView.getAdapter();

            if (layoutManager.getChildCount() > 0) {
                // Calculations..
                int indexOfLastItemViewVisible = layoutManager.getChildCount() - 1;
                View lastItemViewVisible = layoutManager.getChildAt(indexOfLastItemViewVisible);
                int adapterPosition = layoutManager.getPosition(lastItemViewVisible);
                boolean isLastItemVisible = (adapterPosition == adapter.getItemCount() - 1);
                // check
                if (isLastItemVisible && !isLoading && !allDone) {
                    pageNumber++;
                    notificationAdapter.showLoading();
                    loadPosts();
                }
            }
        }
    };
}
