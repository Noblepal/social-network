package com.famousindiasocialnetwork.network.request;

/**
 * Created by a_man on 05-12-2017.
 */

public class CreatePostRequest {
    private String title, text, type, media_url, video_thumbnail_url, category_id;
    private boolean is_story;

    public CreatePostRequest(String title, String text, String type, String media_url, String category_id) {
        this.title = title;
        this.text = text;
        this.type = type;
        this.media_url = media_url;
        this.category_id = category_id;
    }

    public CreatePostRequest(String title, String text, String type, String category_id) {
        this.title = title;
        this.text = text;
        this.type = type;
        this.category_id = category_id;
    }

    public CreatePostRequest(String title, String text, String type, String media_url, String videoThumUrl, String category_id) {
        this.title = title;
        this.text = text;
        this.type = type;
        this.media_url = media_url;
        this.video_thumbnail_url = videoThumUrl;
        this.category_id = category_id;
    }

    public void setIs_story(boolean is_story) {
        this.is_story = is_story;
    }
}
