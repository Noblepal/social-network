package com.famousindiasocialnetwork.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.anupcowkur.reservoir.Reservoir;
//import com.anupcowkur.reservoir.ReservoirPutCallback;
import com.kbeanie.multipicker.api.CameraImagePicker;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.famousindiasocialnetwork.activity.StatusActivity;
import com.famousindiasocialnetwork.adapter.HomeRecyclerAdapter;
import com.famousindiasocialnetwork.adapter.StoriesAdapter;
import com.famousindiasocialnetwork.listener.ShowHideViewListener;
import com.famousindiasocialnetwork.model.Post;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.request.CreatePostRequest;
import com.famousindiasocialnetwork.network.response.CreatePostResponse;
import com.famousindiasocialnetwork.network.response.ProfileResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.FirebaseUploader;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.response.BaseListModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Displays the feed
 */
public class HomeFeedsFragment extends Fragment implements ImagePickerCallback {
    private RecyclerView recyclerView;
    private NestedScrollView nestedScrollView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView;
    private TextView empty_view_text;

    private LinearLayoutManager linearLayoutManager;
    private HomeRecyclerAdapter homeRecyclerAdapter;
    private String postType;
    private SharedPreferenceUtil sharedPreferenceUtil;
    private int pageNumber = 1, userId;

    private TextView playAll;
    private LinearLayout storyContainer;
    private RecyclerView recyclerStory;
    private ArrayList<UserResponse> storyUsers = new ArrayList<>();
    private StoriesAdapter storyAdapter;
    private ImagePicker imagePicker;
    private CameraImagePicker cameraPicker;
    private String pickerPath;
    private File mediaFile;
    private FirebaseUploader firebaseUploader;
    private final int REQUEST_CODE_PERMISSION = 55;

    private static float MAX_SWIPE_DISTANCE_FACTOR = 0.6f;
    private static int DEFAULT_REFRESH_TRIGGER_DISTANCE = 200;
    private int refreshTriggerDistance = DEFAULT_REFRESH_TRIGGER_DISTANCE;

    private DrService weService;
    private boolean allDone, isLoading, storyProgress, showStory;

    private ArrayList<Post> bookmarkedPosts;
    private Call<BaseListModel<Post>> getPosts;
    private String title;

    private static final int HIDE_THRESHOLD = 25;
    private int scrolledDistance = 0;
    private boolean controlsVisible = true, wait;
    private ShowHideViewListener showHideViewListener;

    private Callback<BaseListModel<Post>> callBack = new Callback<BaseListModel<Post>>() {
        @Override
        public void onResponse(Call<BaseListModel<Post>> call, Response<BaseListModel<Post>> response) {
            isLoading = false;
//            if (homeRecyclerAdapter != null && homeRecyclerAdapter.isLoaderShowing())
//                homeRecyclerAdapter.hideLoading();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(false);
            if (response.isSuccessful()) {
                BaseListModel<Post> postResponse = response.body();
                if (postResponse.getData() == null || postResponse.getData().isEmpty()) {
                    if (homeRecyclerAdapter != null && homeRecyclerAdapter.itemsList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                    allDone = true;
                } else {
                    ArrayList<Post> newList = postResponse.getData();
                    if (bookmarkedPosts != null) {
                        for (Post post : newList) {
                            post.setDisliked(bookmarkedPosts.contains(post) ? 1 : 0);
                        }
                    }
                    newList.add(new Post("add"));
//                    if (newList.size() >= 5) {
//                        newList.add(5, new Post("add"));
//                    }
//                    if (newList.size() >= 11) {
//                        newList.add(11, new Post("add"));
//                    } else if (newList.size() >= 16) {
//                        newList.add(16, new Post("add"));
//                    } else {
//                        newList.add(new Post("add"));
//                    }
                    if (homeRecyclerAdapter != null) {
                        homeRecyclerAdapter.addItemsAtBottom(newList);
                    }
                }
            } else {
                if (homeRecyclerAdapter != null && homeRecyclerAdapter.itemsList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onFailure(Call<BaseListModel<Post>> call, Throwable t) {
            isLoading = false;
//            if (homeRecyclerAdapter != null && homeRecyclerAdapter.isLoaderShowing())
//                homeRecyclerAdapter.hideLoading();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(false);
            if (homeRecyclerAdapter != null && homeRecyclerAdapter.itemsList.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
    };

//    private RecyclerView.OnScrollListener recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
//        @Override
//        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//            super.onScrolled(recyclerView, dx, dy);
//            if (showHideViewListener != null && !wait) {
//                if (scrolledDistance > HIDE_THRESHOLD && controlsVisible) {
//                    showHideViewListener.hideView();
//                    wait = true;
//                    controlsVisible = false;
//                    scrolledDistance = 0;
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            wait = false;
//                        }
//                    }, 600);
//                } else if (scrolledDistance < -HIDE_THRESHOLD && !controlsVisible) {
//                    showHideViewListener.showView();
//                    wait = true;
//                    controlsVisible = true;
//                    scrolledDistance = 0;
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            wait = false;
//                        }
//                    }, 600);
//                }
//                if ((controlsVisible && dy > 0) || (!controlsVisible && dy < 0)) {
//                    scrolledDistance += dy;
//                }
//            }
//
//            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
//            RecyclerView.Adapter adapter = recyclerView.getAdapter();
//            if (layoutManager.getChildCount() > 0) {
//                // Calculations..
//                int indexOfLastItemViewVisible = layoutManager.getChildCount() - 1;
//                View lastItemViewVisible = layoutManager.getChildAt(indexOfLastItemViewVisible);
//                int adapterPosition = layoutManager.getPosition(lastItemViewVisible);
//                boolean isLastItemVisible = (adapterPosition == adapter.getItemCount() - 1);
//                if (isLastItemVisible && !isLoading && !allDone) {
//                    Log.e("CHECK_LAST", "loading more");
//                    pageNumber++;
//                    homeRecyclerAdapter.showLoading();
//                    loadPosts();
//                }
//            }
//        }
//    };

    private NestedScrollView.OnScrollChangeListener nestedScrollViewChangeListener = new NestedScrollView.OnScrollChangeListener() {
        @Override
        public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            if (scrollY > oldScrollY) {
            }
            if (scrollY < oldScrollY) {
            }

            if (showHideViewListener != null && !wait) {
                if (scrolledDistance > HIDE_THRESHOLD && controlsVisible) {
                    showHideViewListener.hideView();
                    wait = true;
                    controlsVisible = false;
                    scrolledDistance = 0;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            wait = false;
                        }
                    }, 600);
                } else if (scrolledDistance < -HIDE_THRESHOLD && !controlsVisible) {
                    showHideViewListener.showView();
                    wait = true;
                    controlsVisible = true;
                    scrolledDistance = 0;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            wait = false;
                        }
                    }, 600);
                }
                int dy = scrollY - oldScrollY;
                if ((controlsVisible && dy > 0) || (!controlsVisible && dy < 0)) {
                    scrolledDistance += dy;
                }
            }

            if (v.getChildAt(v.getChildCount() - 1) != null) {
                if ((scrollY >= (v.getChildAt(v.getChildCount() - 1).getMeasuredHeight() - v.getMeasuredHeight())) && scrollY > oldScrollY) {
                    int visibleItemCount = linearLayoutManager.getChildCount();
                    int totalItemCount = linearLayoutManager.getItemCount();
                    int pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisiblesItems) >= totalItemCount && !isLoading && !allDone) {
                        Log.e("CHECK_LAST", "loading more");
                        pageNumber++;
                        //homeRecyclerAdapter.showLoading();
                        loadPosts();
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferenceUtil = new SharedPreferenceUtil(getActivity());
        weService = ApiUtils.getClient().create(DrService.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_view, container, false);
        playAll = view.findViewById(R.id.playAll);
        storyContainer = view.findViewById(R.id.storyContainer);
        recyclerStory = view.findViewById(R.id.recyclerStory);

        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.frag_home_feeds_swipe_refresh_layout);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        emptyView = view.findViewById(R.id.empty_view_container);
        empty_view_text = view.findViewById(R.id.empty_view_text);
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        //recyclerView.addOnScrollListener(recyclerViewOnScrollListener);
        nestedScrollView.setOnScrollChangeListener(nestedScrollViewChangeListener);
        homeRecyclerAdapter = new HomeRecyclerAdapter(this);
        recyclerView.setAdapter(homeRecyclerAdapter);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Float mDistanceToTriggerSync = Math.min(swipeRefreshLayout.getHeight() * MAX_SWIPE_DISTANCE_FACTOR, refreshTriggerDistance * metrics.density);
        swipeRefreshLayout.setDistanceToTriggerSync(mDistanceToTriggerSync.intValue());
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initialize();
        if (showStory) loadStoryUsers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION && checkStoragePermissions()) {
            pickMedia();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Picker.PICK_IMAGE_DEVICE:
                    if (imagePicker == null) {
                        imagePicker = new ImagePicker(this);
                    }
                    imagePicker.submit(data);
                    break;
                case Picker.PICK_IMAGE_CAMERA:
                    if (cameraPicker == null) {
                        cameraPicker = new CameraImagePicker(this);
                        cameraPicker.reinitialize(pickerPath);
                    }
                    cameraPicker.submit(data);
                    break;
            }
        }
    }

    public void refresh() {
        pageNumber = 1;
        homeRecyclerAdapter.clear();
        allDone = false;
        if (showStory) loadStoryUsers();
        loadPosts();
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    public void pickMedia() {
        if (checkStoragePermissions()) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setMessage("Get story from");
            alertDialog.setPositiveButton("Camera", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    cameraPicker = new CameraImagePicker(HomeFeedsFragment.this);
                    cameraPicker.shouldGenerateMetadata(true);
                    cameraPicker.shouldGenerateThumbnails(true);
                    cameraPicker.setImagePickerCallback(HomeFeedsFragment.this);
                    pickerPath = cameraPicker.pickImage();
                }
            });
            alertDialog.setNegativeButton("Gallery", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    imagePicker = new ImagePicker(HomeFeedsFragment.this);
                    imagePicker.shouldGenerateMetadata(true);
                    imagePicker.shouldGenerateThumbnails(true);
                    imagePicker.setImagePickerCallback(HomeFeedsFragment.this);
                    imagePicker.pickImage();
                }
            });
            alertDialog.create().show();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION);
        }
    }

    private boolean checkStoragePermissions() {
        return
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        &&
                        ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        &&
                        ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void setStoryProgress(boolean progress) {
        storyProgress = progress;
        storyUsers.get(0).setStoryUpdateProgress(storyProgress);
        storyAdapter.notifyItemChanged(0);
    }

    private void loadStoryUsers() {
        storyAdapter = new StoriesAdapter(storyUsers, getContext(), new StoriesAdapter.StoryClickListener() {
            @Override
            public void showStory(int pos) {
                startActivity(StatusActivity.newIntent(getContext(), storyUsers, pos));
            }

            @Override
            public void postStory() {
                if (!storyProgress)
                    pickMedia();
            }
        });
        recyclerStory.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        recyclerStory.setAdapter(storyAdapter);

        storyContainer.setVisibility(View.GONE);
        weService.getStoryUsers(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null)).enqueue(new Callback<ArrayList<UserResponse>>() {
            @Override
            public void onResponse(Call<ArrayList<UserResponse>> call, Response<ArrayList<UserResponse>> response) {
                if (response.isSuccessful()) {
                    storyUsers.clear();
                    storyUsers.add(new UserResponse(-1, "add", "add"));
                    storyUsers.addAll(response.body());
                    storyAdapter.notifyDataSetChanged();
                    Log.e("CHECK_STORY", String.valueOf(storyUsers.size()));
                    storyContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ArrayList<UserResponse>> call, Throwable t) {
                Log.e("CHECK_STORY", t.getMessage());
            }
        });

        playAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (storyUsers != null && storyUsers.size() > 1)
                    startActivity(StatusActivity.newIntent(getContext(), storyUsers, 1));
            }
        });
    }

    @Override
    public void onImagesChosen(List<ChosenImage> images) {
        mediaFile = new File(Uri.parse(images.get(0).getOriginalPath()).getPath());

        firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
            @Override
            public void onUploadFail(String message) {
                setStoryProgress(false);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUploadSuccess(String downloadUrl) {
                post(new CreatePostRequest("StoryTitle", "StoryText", "image", downloadUrl, "1"));
            }

            @Override
            public void onUploadProgress(int progress) {
            }

            @Override
            public void onUploadCancelled() {
                setStoryProgress(false);
            }
        });
        firebaseUploader.uploadImage(getContext(), mediaFile);
        setStoryProgress(true);
        Toast.makeText(getContext(), "Uploading story", Toast.LENGTH_SHORT).show();
    }

    private void post(CreatePostRequest createPostRequest) {
        createPostRequest.setIs_story(true);
        weService.createPost(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), createPostRequest).enqueue(new Callback<CreatePostResponse>() {
            @Override
            public void onResponse(Call<CreatePostResponse> call, Response<CreatePostResponse> response) {
                setStoryProgress(false);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Story added", Toast.LENGTH_SHORT).show();
                    CreatePostResponse postResponse = response.body();
                    if (!storyUsers.contains(new UserResponse(postResponse.getUser_profile_id().getId(), "", ""))) {
                        storyUsers.add(1, new UserResponse(postResponse.getUser_profile_id().getId(), postResponse.getUser_profile_id().getImage(), postResponse.getUser_profile_id().getName()));
                        storyAdapter.notifyItemInserted(1);
                    }
                }
            }

            @Override
            public void onFailure(Call<CreatePostResponse> call, Throwable t) {
                setStoryProgress(false);
            }
        });
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // You have to save path in case your activity is killed.
        // In such a scenario, you will need to re-initialize the CameraImagePicker
        outState.putString("picker_path", pickerPath);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("picker_path")) {
                pickerPath = savedInstanceState.getString("picker_path");
            }
        }
    }

    private void initialize() {
        loadPosts();
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void loadPosts() {
        if (postType != null && postType.equals("bookmark")) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
//                    if (homeRecyclerAdapter.isLoaderShowing())
//                        homeRecyclerAdapter.hideLoading();
                    if (swipeRefreshLayout.isRefreshing())
                        swipeRefreshLayout.setRefreshing(false);

                    homeRecyclerAdapter.addItemsAtBottom(bookmarkedPosts);
                    if (bookmarkedPosts.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                }
            }, 200);
        } else {
            isLoading = true;
            if (userId != -1)
                getPosts = weService.getPostsByUserId(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), userId, postType.equals("hot") ? 1 : 0, pageNumber);
            else
                getPosts = weService.getPosts(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), postType.equals("hot") ? 1 : 0, pageNumber);
            getPosts.enqueue(callBack);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //recyclerView.removeOnScrollListener(recyclerViewOnScrollListener);
        if (getPosts != null && !getPosts.isCanceled())
            getPosts.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.POST_CHANGE_EVENT);
        intentFilter.addAction(Constants.POST_NEW_EVENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(postEventReceiver, intentFilter);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(profileEventReceiver, new IntentFilter(Constants.PROFILE_CHANGE_EVENT));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(postEventReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(profileEventReceiver);
    }

    private BroadcastReceiver profileEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (recyclerView != null && homeRecyclerAdapter != null) {
                ProfileResponse profileResponse = Helper.getProfileMe(sharedPreferenceUtil);
                if (profileResponse != null) {
                    boolean changed = false;
                    for (Post post : homeRecyclerAdapter.itemsList) {
                        if (!post.getId().equalsIgnoreCase("add") && post.getUserMetaData() != null && post.getUserMetaData().getId() == userId) {
                            if (!TextUtils.isEmpty(profileResponse.getName()))
                                post.getUserMetaData().setName(profileResponse.getName());
                            if (!TextUtils.isEmpty(profileResponse.getImage()))
                                post.getUserMetaData().setImage(profileResponse.getImage());
                            changed = true;
                        }
                    }
                    if (changed) {
                        homeRecyclerAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    private BroadcastReceiver postEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.POST_CHANGE_EVENT)) {
                    Post post = intent.getParcelableExtra("post");
                    if (post != null) {
                        if (homeRecyclerAdapter != null) {
                            homeRecyclerAdapter.updateItem(post);
                        }
                    }
                } else if (intent.getAction().equals(Constants.POST_NEW_EVENT)) {
                    Post post = intent.getParcelableExtra("post");
                    if (post != null && !postType.equals("bookmark")) {
                        homeRecyclerAdapter.addItemOnTop(post);
                        recyclerView.scrollToPosition(0);
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    }
                }
            }
        }
    };

//    @OnClick(R.id.frag_home_feeds_refresh_indicator)
//    public void scrollToTop() {
//        if (recyclerView != null) {
//            recyclerView.scrollToPosition(0);
//        }
//        if (refreshIndicator != null) {
//            refreshIndicator.setVisibility(View.GONE);
//        }
//    }

    public static HomeFeedsFragment newInstance(String name, ArrayList<Post> bookmarkedPosts, int userId, boolean showStory, ShowHideViewListener showHideViewListener) {
        HomeFeedsFragment homeFeedsFragment = new HomeFeedsFragment();
        homeFeedsFragment.postType = name;
        homeFeedsFragment.bookmarkedPosts = bookmarkedPosts;
        homeFeedsFragment.userId = userId;
        homeFeedsFragment.showStory = showStory;
        homeFeedsFragment.showHideViewListener = showHideViewListener;
        return homeFeedsFragment;
    }
}