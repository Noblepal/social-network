package com.famousindiasocialnetwork.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.activity.SettingsActivity;
import com.famousindiasocialnetwork.network.ApiUtils;
import com.famousindiasocialnetwork.network.DrService;
import com.famousindiasocialnetwork.network.request.UserUpdateRequest;
import com.famousindiasocialnetwork.network.response.ProfileResponse;
import com.famousindiasocialnetwork.network.response.UserResponse;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;
import com.google.firebase.iid.FirebaseInstanceId;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Provides the option for customizing the Notification settings like sound and vibration
 */
public class SettingsPushNotification extends Fragment {
    SwitchCompat[] switches = new SwitchCompat[2];

    private SharedPreferenceUtil sharedPreferenceUtil;
    private ProfileResponse profileResponse;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_settings_push_notification, container, false);

        switches[0] = view.findViewById(R.id.frag_settings_push_noti_like_switch_view);
        switches[1] = view.findViewById(R.id.frag_settings_push_noti_comment_switch_view);
        setHasOptionsMenu(true);

        sharedPreferenceUtil = new SharedPreferenceUtil(getContext());
        profileResponse = Helper.getProfileMe(sharedPreferenceUtil);
        switches[0].setChecked(profileResponse != null && profileResponse.getNotification_on_like());
        switches[1].setChecked(profileResponse != null && profileResponse.getNotification_on_comment());





        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.activity_settings_menu_done).setVisible(true);
        ((SettingsActivity) getActivity()).setActionBarSubtitle("Push Notification");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.activity_settings_menu_done) {
            Helper.closeKeyboard(getActivity());
            updateNotificationSettingsCall();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Makes network call here if required and updates the settings in the {@link android.content.SharedPreferences}
     */
    private void updateNotificationSettingsCall() {
        DrService service = ApiUtils.getClient().create(DrService.class);
        service.createUpdateUser(sharedPreferenceUtil.getStringPreference(Constants.KEY_API_KEY, null),
                new UserUpdateRequest(profileResponse.getGender(),
                        FirebaseInstanceId.getInstance().getToken(),
                        switches[0].isChecked(), true, switches[1].isChecked()), 1).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Settings Updated", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                t.getMessage();
            }
        });
    }
}
