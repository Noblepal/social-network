package com.famousindiasocialnetwork.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.adapter.SearchUserResultAdapter;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.BaseListModel;
import com.famousindiasocialnetwork.network.response.ProfileFollowResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowersFragment extends Fragment {

    private RecyclerView usersRecycler;
    private ArrayList<UserResponse> users = new ArrayList<>();
    private ProgressBar searchProgress;
    private DrService owhloService;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private boolean isLoading, allDone;
    private int pageNumber = 1;
    private TextView noResults;
    private SwipeRefreshLayout srlFollowers;

    public FollowersFragment() {
        // Required empty public constructor
    }


    public static FollowersFragment newInstance() {
        FollowersFragment fragment = new FollowersFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_followers, container, false);

        owhloService = ApiUtils.getClient().create(DrService.class);
        sharedPreferenceUtil = new SharedPreferenceUtil(getActivity());
        initUi(root);
        loadResults();
        searchProgress.setVisibility(View.VISIBLE);

        srlFollowers.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pageNumber = 1;
                users.clear();
                loadResults();
            }
        });

        return root;
    }

    private void initUi(View root) {
        //root.findViewById(R.id.ll_top).setVisibility(View.GONE);
        usersRecycler = root.findViewById(R.id.usersRecycler);
        srlFollowers = root.findViewById(R.id.srlFollowers);
        searchProgress = root.findViewById(R.id.searchProgress);
        noResults = root.findViewById(R.id.noResults);

        usersRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        usersRecycler.addOnScrollListener(recyclerViewOnScrollListener);
        usersRecycler.setAdapter(new SearchUserResultAdapter(getActivity(), users, new SearchUserResultAdapter.SearchUserActionClickListener() {
            @Override
            public void onActionClick(final UserResponse user) {
                owhloService.profileFollowAction(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), user.getId()).enqueue(new Callback<ProfileFollowResponse>() {
                    @Override
                    public void onResponse(Call<ProfileFollowResponse> call, Response<ProfileFollowResponse> response) {
                        if (response.isSuccessful() && response.body().getSuccess() != 0) {
                            user.setIs_following(response.body().isFollowed() ? 1 : 0);
                            int index = users.indexOf(user);
                            if (index == -1)
                                usersRecycler.getAdapter().notifyDataSetChanged();
                            else
                                usersRecycler.getAdapter().notifyItemChanged(index);
                        }
                    }

                    @Override
                    public void onFailure(Call<ProfileFollowResponse> call, Throwable t) {

                    }
                });
            }
        }));

    }

    private Callback<BaseListModel<UserResponse>> searchCallback = new Callback<BaseListModel<UserResponse>>() {
        @Override
        public void onResponse(Call<BaseListModel<UserResponse>> call, Response<BaseListModel<UserResponse>> response) {
            searchProgress.setVisibility(View.GONE);
            srlFollowers.setRefreshing(false);
            if (response.isSuccessful()) {
                allDone = response.body().getData().isEmpty();
                users.addAll(response.body().getData());
                usersRecycler.getAdapter().notifyDataSetChanged();
                if (users.isEmpty())
                    noResults.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onFailure(Call<BaseListModel<UserResponse>> call, Throwable t) {
            searchProgress.setVisibility(View.GONE);
            srlFollowers.setRefreshing(false);
            noResults.setVisibility(View.VISIBLE);
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
                    loadResults();
                }
            }
        }
    };

    private void loadResults() {
        srlFollowers.setRefreshing(true);
        String serializedUser = sharedPreferenceUtil.getStringPreference("user", "null");

        try {
            JSONObject userJSON = new JSONObject(serializedUser);
            int userID = userJSON.getInt("id");

            Call<BaseListModel<UserResponse>> call;
            call = owhloService.getFollowers(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), userID, pageNumber);
            call.enqueue(searchCallback);
            noResults.setVisibility(View.GONE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}