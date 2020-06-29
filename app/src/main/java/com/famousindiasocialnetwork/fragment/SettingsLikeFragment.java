package com.famousindiasocialnetwork.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.util.Helper;

/**
 * Displays various options to like the app like on Facebook, Twitter and Google Plus along with option to rate on Google Play Store
 */
public class SettingsLikeFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_settings_like, container, false);
        setHasOptionsMenu(true);
        int[] viewIds = {R.id.frag_settings_like_fb, R.id.frag_settings_like_gplus, R.id.frag_settings_like_twitter, R.id.frag_settings_like_rate_us};
        for (int vId : viewIds) {
            view.findViewById(vId).setOnClickListener(this);
        }
        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
//        ((SettingsActivity) getActivity()).setActionBarSubtitle("Like");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.frag_settings_like_fb:
                startActivity(Helper.newFacebookIntent(getActivity().getPackageManager(), "https://www.facebook.com/"));
                // startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/verbosetech/?fref=ts")));
                break;
            case R.id.frag_settings_like_gplus:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/u/0/+google")));
                break;
            case R.id.frag_settings_like_twitter:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/")));
                break;
            case R.id.frag_settings_like_rate_us:
                Helper.openPlayStoreIntent(getContext());
                break;

        }
    }
}
