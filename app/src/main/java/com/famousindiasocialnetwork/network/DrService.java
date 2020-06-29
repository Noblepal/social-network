package com.famousindiasocialnetwork.network;

import com.famousindiasocialnetwork.model.Activity;
import com.famousindiasocialnetwork.model.Comment;
import com.famousindiasocialnetwork.model.Post;
import com.famousindiasocialnetwork.network.request.CreateCommentRequest;
import com.famousindiasocialnetwork.network.request.CreatePostRequest;
import com.famousindiasocialnetwork.network.request.PaymentRequest;
import com.famousindiasocialnetwork.network.request.UserUpdateRequest;
import com.famousindiasocialnetwork.network.response.BaseListModel;
import com.famousindiasocialnetwork.network.response.CreatePostResponse;
import com.famousindiasocialnetwork.network.response.LikeDislikeResponse;
import com.famousindiasocialnetwork.network.response.ProfileResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.network.response.PaymentResponse;
import com.famousindiasocialnetwork.network.response.ProfileFollowResponse;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by a_man on 05-12-2017.
 */

public interface DrService {
    @Headers("Accept: application/json")
    @POST("api/profile")
    Call<UserResponse> createUpdateUser(@Header("Authorization") String token, @Body UserUpdateRequest userRegisterResponse, @Query("update") int update);

    @Headers("Accept: application/json")
    @GET("api/profile/following/{id}")
    Call<BaseListModel<UserResponse>> getFollowings(@Header("Authorization") String token, @Path("id") int profileId, @Query("page") int page);

    @Headers("Accept: application/json")
    @GET("api/profile/followers/{id}")
    Call<BaseListModel<UserResponse>> getFollowers(@Header("Authorization") String token, @Path("id") int profileId, @Query("page") int page);

    @Headers("Accept: application/json")
    @GET("api/stories/users")
    Call<ArrayList<UserResponse>> getStoryUsers(@Header("Authorization") String token);

    @Headers("Accept: application/json")
    @GET("api/stories/users/{id}")
    Call<ArrayList<Post>> getStory(@Header("Authorization") String token, @Path("id") int profileId);

    @Headers("Accept: application/json")
    @GET("api/profile/{id}")
    Call<ProfileResponse> getProfile(@Header("Authorization") String token, @Path("id") int profileId);

    @Headers("Accept: application/json")
    @GET("api/activities")
    Call<BaseListModel<Activity>> getActivities(@Header("Authorization") String token, @Query("page") int page);

    @Headers("Accept: application/json")
    @POST("api/posts")
    Call<CreatePostResponse> createPost(@Header("Authorization") String token, @Body CreatePostRequest createPostRequest);

    @Headers("Accept: application/json")
    @GET("api/posts")
    Call<BaseListModel<Post>> getPosts(@Header("Authorization") String token, @Query("treding") int type, @Query("page") int page);

    @Headers("Accept: application/json")
    @GET("api/posts")
    Call<BaseListModel<Post>> getPostsByUserId(@Header("Authorization") String token, @Query("user_profile_id") int userProfileId, @Query("treding") int type, @Query("page") int page);

    @Headers("Accept: application/json")
    @GET("api/posts/me")
    Call<BaseListModel<Post>> getPostsMy(@Header("Authorization") String token, @Query("page") int page);

    @Headers("Accept: application/json")
    @GET("api/posts/{id}/show")
    Call<Post> getPostById(@Header("Authorization") String token, @Path("id") String postId);

    @Headers("Accept: application/json")
    @DELETE("api/posts/{id}/delete")
    Call<JsonObject> deletePost(@Header("Authorization") String token, @Path("id") String postId);

    @Headers("Accept: application/json")
    @POST("api/posts/{id}/share")
    Call<JsonObject> updateSharePost(@Header("Authorization") String token, @Path("id") String postId);

    @Headers("Accept: application/json")
    @POST("api/posts/{id}/like")
    Call<LikeDislikeResponse> updatePostLike(@Header("Authorization") String token, @Path("id") String postId);

    @Headers("Accept: application/json")
    @POST("api/posts/{id}/dislike")
    Call<LikeDislikeResponse> updatePostDislike(@Header("Authorization") String token, @Path("id") String postId);

    @Headers("Accept: application/json")
    @GET("api/posts/{id}/comments")
    Call<BaseListModel<Comment>> getComments(@Header("Authorization") String token, @Path("id") String postId, @Query("page") int page);

    @Headers("Accept: application/json")
    @POST("api/posts/{id}/comments")
    Call<Comment> createComment(@Header("Authorization") String token, @Path("id") String postId, @Body CreateCommentRequest comment);

    @Headers("Accept: application/json")
    @POST("api/comments/{id}/like")
    Call<LikeDislikeResponse> updateCommentLike(@Header("Authorization") String token, @Path("id") String commentId);

    @Headers("Accept: application/json")
    @POST("api/comments/{id}/dislike")
    Call<LikeDislikeResponse> updateCommentDislike(@Header("Authorization") String token, @Path("id") String commentId);

    @Headers("Accept: application/json")
    @POST("api/profile/search")
    Call<BaseListModel<UserResponse>> profileSearch(@Header("Authorization") String token, @Body HashMap<String, String> request, @Query("page") int page);

    @Headers("Accept: application/json")
    @POST("api/profile/follow/{id}")
    Call<ProfileFollowResponse> profileFollowAction(@Header("Authorization") String token, @Path("id") int profileId);

    @Headers("Accept: application/json")
    @POST("api/profile/payment")
    Call<PaymentResponse> payment(@Header("Authorization") String token, @Body PaymentRequest paymentRequest);
}
