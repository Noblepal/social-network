package com.famousindiasocialnetwork.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.model.Post;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;

import jp.shts.android.storiesprogressview.StoriesProgressView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class StatusActivity extends AppCompatActivity implements StoriesProgressView.StoriesListener {
    private static final String DATA_STORY_USERS = "StoryUsers";

    private StoriesProgressView storiesProgressView;
    private ImageView storyImage, storyUserImage;
    private TextView storyUserName;
    private ProgressBar storyProgress;
    private ArrayList<UserResponse> storyUsers;
    private ArrayList<Post> stories;

    private SharedPreferenceUtil sharedPreferenceUtil;
    private DrService weService;

    private int counterImage = 0, counterStory = 0;

    long pressTime = 0L;
    long limit = 500L;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressTime = System.currentTimeMillis();
                    storiesProgressView.pause();
                    return false;
                case MotionEvent.ACTION_UP:
                    long now = System.currentTimeMillis();
                    storiesProgressView.resume();
                    return limit < now - pressTime;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_status);
        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        weService = ApiUtils.getClient().create(DrService.class);
        storyUsers = getIntent().getParcelableArrayListExtra(DATA_STORY_USERS);
        initUi();
        startStory(counterStory);
    }

    private void startStory(int pos) {
        resetComplete();
        Glide.with(this)
                .load(storyUsers.get(pos).getImage())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(Helper.dp2px(this, 8))).override(Helper.dp2px(this, 38), Helper.dp2px(this, 38)).placeholder(R.drawable.ic_person_gray_24dp))
                .into(storyUserImage);
        storyUserName.setText(storyUsers.get(pos).getName());
        storyProgress.setVisibility(View.VISIBLE);

        weService.getStory(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null), storyUsers.get(pos).getId()).enqueue(new Callback<ArrayList<Post>>() {
            @Override
            public void onResponse(Call<ArrayList<Post>> call, Response<ArrayList<Post>> response) {
                if (response.isSuccessful()) {
                    setupStories(response.body());
                }
            }

            @Override
            public void onFailure(Call<ArrayList<Post>> call, Throwable t) {
                storyProgress.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void resetComplete() {
        Field field = null;
        try {
            field = storiesProgressView.getClass().getDeclaredField("isComplete");
            // Allow modification on the field
            field.setAccessible(true);
            // Sets the field to the new value for this instance
            field.set(storiesProgressView, false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupStories(ArrayList<Post> posts) {
        counterImage = 0;
        this.stories = posts;

        storiesProgressView.setStoriesCount(stories.size());
        storiesProgressView.setStoryDuration(3000L);
        storiesProgressView.setStoriesListener(this);
        Glide.with(this).load(stories.get(counterImage).getMedia_url()).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).placeholder(R.drawable.placeholder)).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                storyProgress.setVisibility(View.INVISIBLE);
                storiesProgressView.startStories();
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                storyProgress.setVisibility(View.INVISIBLE);
                storiesProgressView.startStories();
                return false;
            }
        }).into(storyImage);
    }

    private void initUi() {
        storyImage = findViewById(R.id.image);
        storyUserImage = findViewById(R.id.storyUserImage);
        storyUserName = findViewById(R.id.storyUserName);
        storiesProgressView = findViewById(R.id.stories);
        storyProgress = findViewById(R.id.storyProgress);
        // bind reverse view
        View reverse = findViewById(R.id.reverse);
        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storiesProgressView.reverse();
            }
        });
        reverse.setOnTouchListener(onTouchListener);

        // bind skip view
        View skip = findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storiesProgressView.skip();
            }
        });
        skip.setOnTouchListener(onTouchListener);
    }

    @Override
    public void onNext() {
        counterImage++;
        if (counterImage < stories.size()) {
            //storiesProgressView.pause();
            Glide.with(this).load(stories.get(counterImage).getMedia_url()).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).placeholder(R.drawable.placeholder)).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    //storiesProgressView.resume();
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    //storiesProgressView.resume();
                    return false;
                }
            }).into(storyImage);
        }
    }

    @Override
    public void onPrev() {
        if (counterImage > 0) {
            counterImage--;
            //storiesProgressView.pause();
            Glide.with(this).load(stories.get(counterImage).getMedia_url()).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).placeholder(R.drawable.placeholder)).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    //storiesProgressView.resume();
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    //storiesProgressView.resume();
                    return false;
                }
            }).into(storyImage);
        } else if (counterStory != 0) {
            counterStory--;
            storiesProgressView.destroy();
            startStory(counterStory);
        }
    }

    @Override
    public void onComplete() {
        //finish();
        counterStory++;
        if (counterStory < storyUsers.size()) {
            startStory(counterStory);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        // Very important !
        storiesProgressView.destroy();
        super.onDestroy();
    }

    public static Intent newIntent(Context context, ArrayList<UserResponse> storyUsers, int pos) {
        Intent intent = new Intent(context, StatusActivity.class);
        ArrayList<UserResponse> toPass = new ArrayList<>();
        for (int i = pos; i < storyUsers.size(); i++) {
            toPass.add(storyUsers.get(i));
        }
        intent.putParcelableArrayListExtra(DATA_STORY_USERS, toPass);
        return intent;
    }
}