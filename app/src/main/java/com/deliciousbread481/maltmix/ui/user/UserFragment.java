package com.deliciousbread481.maltmix.ui.user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.deliciousbread481.maltmix.api.BiliApi;
import com.deliciousbread481.maltmix.api.BiliAuthManager;
import com.deliciousbread481.maltmix.util.LogDialog;

public class UserFragment extends Fragment {

    private BiliAuthManager authManager;

    private TextView statusText;
    private TextView unameText;
    private Button biliButton;

    private final ActivityResultLauncher<Intent> biliLoginLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            refreshUi();
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        authManager = new BiliAuthManager(requireContext());

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(requireContext());
        title.setText("账号绑定");
        title.setTextSize(24);
        layout.addView(title);

        statusText = new TextView(requireContext());
        statusText.setTextSize(16);
        statusText.setPadding(0, 24, 0, 8);
        layout.addView(statusText);

        unameText = new TextView(requireContext());
        unameText.setTextSize(14);
        unameText.setPadding(0, 0, 0, 24);
        layout.addView(unameText);

        biliButton = new Button(requireContext());
        biliButton.setOnClickListener(v -> {
            if (authManager.isLoggedIn()) {
                authManager.logout();
                refreshUi();
            } else {
                biliLoginLauncher.launch(
                        new Intent(requireContext(), BiliLoginActivity.class));
            }
        });
        layout.addView(biliButton);

        Button btnNetease = new Button(requireContext());
        btnNetease.setText("绑定网易云音乐账号");
        layout.addView(btnNetease);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        if (authManager.isLoggedIn()) {
            statusText.setText("B站：已登录");
            biliButton.setText("退出B站登录");

            String sessdata = authManager.getSessdata();
            BiliApi.fetchUserInfo(sessdata, new BiliApi.UserInfoCallback() {
                @Override
                public void onSuccess(long mid, String uname, String face) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            unameText.setText("用户名：" + uname + "  UID：" + mid);
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            unameText.setText("获取用户信息失败");
                            LogDialog.error(requireContext(), error);
                        });
                    }
                }
            });
        } else {
            statusText.setText("B站：未登录");
            unameText.setText("");
            biliButton.setText("绑定哔哩哔哩账号");
        }
    }
}