package com.famousindiasocialnetwork.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.adapter.SearchUserResultAdapter;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.BaseListModel;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.network.response.ProfileFollowResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by a_man on 09-02-2018.
 */

public class SearchUserFragment extends Fragment {
    private String query;
    private RecyclerView usersRecycler;
    private ArrayList<UserResponse> users = new ArrayList<>();
    private ProgressBar searchProgress;
    private DrService owhloService;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private boolean isLoading, allDone;
    private int pageNumber = 1, myId;
    private TextView noResults;

    private Callback<BaseListModel<UserResponse>> searchCallback = new Callback<BaseListModel<UserResponse>>() {
        @Override
        public void onResponse(Call<BaseListModel<UserResponse>> call, Response<BaseListModel<UserResponse>> response) {
            searchProgress.setVisibility(View.GONE);
            if (response.isSuccessful()) {
                allDone = response.body().getData().isEmpty();
                ArrayList<UserResponse> newUsers = response.body().getData();
                for (UserResponse user : newUsers) {
                    if (user.getId() != myId)
                        users.add(user);
                }
                usersRecycler.getAdapter().notifyDataSetChanged();
                if (users.isEmpty())
                    noResults.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onFailure(Call<BaseListModel<UserResponse>> call, Throwable t) {
            searchProgress.setVisibility(View.GONE);
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
                    //homeRecyclerAdapter.showLoading();

                    HashMap<String, String> request = new HashMap<>();
                    request.put("search", query);
                    loadResults(request);
                }
            }
        }
    };

    public SearchUserFragment() {
        // Required empty public constructor
    }

    public static SearchUserFragment newInstance(String query) {
        SearchUserFragment fragment = new SearchUserFragment();
        fragment.query = query;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        owhloService = ApiUtils.getClient().create(DrService.class);
        sharedPreferenceUtil = new SharedPreferenceUtil(getContext());
        UserResponse userResponse = Helper.getLoggedInUser(sharedPreferenceUtil);
        myId = userResponse != null ? userResponse.getId() : -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_user, container, false);
        usersRecycler = view.findViewById(R.id.usersRecycler);
        searchProgress = view.findViewById(R.id.searchProgress);
        noResults = view.findViewById(R.id.noResults);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        usersRecycler.addOnScrollListener(recyclerViewOnScrollListener);
        usersRecycler.setAdapter(new SearchUserResultAdapter(getContext(), users, new SearchUserResultAdapter.SearchUserActionClickListener() {
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

        HashMap<String, String> request = new HashMap<>();
        request.put("search", query);
        loadResults(request);
        searchProgress.setVisibility(View.VISIBLE);
    }

    public void newQuery(String query) {
        this.query = query;
        isLoading = true;
        pageNumber = 1;

        users.clear();
        usersRecycler.getAdapter().notifyDataSetChanged();

        HashMap<String, String> request = new HashMap<>();
        request.put("search", query);

        loadResults(request);
        searchProgress.setVisibility(View.VISIBLE);
    }

    private void loadResults(HashMap<String, String> request) {
        owhloService.profileSearch(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), request, pageNumber).enqueue(searchCallback);
        noResults.setVisibility(View.GONE);
    }

}
