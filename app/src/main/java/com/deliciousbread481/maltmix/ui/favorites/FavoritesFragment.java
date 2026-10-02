package com.deliciousbread481.maltmix.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.deliciousbread481.maltmix.R;
import com.deliciousbread481.maltmix.api.BiliApi;
import com.deliciousbread481.maltmix.api.BiliAuthManager;
import com.deliciousbread481.maltmix.model.FavFolder;
import com.deliciousbread481.maltmix.util.LocalFavStore;
import com.deliciousbread481.maltmix.util.LogDialog;

import java.util.List;

public class FavoritesFragment extends Fragment {

    private LocalFavStore localStore;
    private BiliAuthManager biliAuth;

    private LinearLayout localContainer;
    private LinearLayout biliContainer;
    private TextView biliStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localStore = new LocalFavStore(requireContext());
        biliAuth = new BiliAuthManager(requireContext());

        localContainer = view.findViewById(R.id.localFoldersContainer);
        biliContainer = view.findViewById(R.id.biliFoldersContainer);
        biliStatus = view.findViewById(R.id.biliStatus);

        Button btnCreate = view.findViewById(R.id.btnCreateFolder);
        btnCreate.setOnClickListener(v -> showCreateDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLocalFolders();
        refreshBiliFolders();
    }

    // ---------- 本地收藏夹 ----------

    private void refreshLocalFolders() {
        localContainer.removeAllViews();
        List<String> names = localStore.getFolderNames();
        if (names.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("暂无本地收藏夹，点击右上角“新建”创建");
            tv.setTextColor(0xFF888888);
            tv.setTextSize(14);
            tv.setPadding(0, 8, 0, 8);
            localContainer.addView(tv);
            return;
        }
        for (String name : names) {
            View item = createFolderItem(name, 0, true, "local", 0);
            localContainer.addView(item);
        }
    }

    private void showCreateDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("例如：最好的歌单");

        new AlertDialog.Builder(requireContext())
                .setTitle("新建收藏夹")
                .setView(input)
                .setPositiveButton("创建", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        LogDialog.warn(requireContext(), "名称不能为空");
                        return;
                    }
                    boolean ok = localStore.addFolder(name);
                    if (ok) {
                        refreshLocalFolders();
                    } else {
                        LogDialog.warn(requireContext(), "已存在同名收藏夹");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ---------- B站收藏夹 ----------

    private void refreshBiliFolders() {
        biliContainer.removeAllViews();

        if (!biliAuth.isLoggedIn()) {
            biliStatus.setText("未登录，请先在“用户”页绑定 B站账号");
            return;
        }

        biliStatus.setText("正在加载收藏夹...");
        String sessdata = biliAuth.getSessdata();

        BiliApi.fetchUserInfo(sessdata, new BiliApi.UserInfoCallback() {
            @Override
            public void onSuccess(long mid, String uname, String face,
                                  String imgKey, String subKey) {
                BiliApi.fetchFavFolders(mid, sessdata, new BiliApi.FavFolderCallback() {
                    @Override
                    public void onSuccess(List<FavFolder> folders) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            biliStatus.setText("共 " + folders.size() + " 个收藏夹");
                            biliContainer.removeAllViews();
                            for (FavFolder f : folders) {
                                View item = createFolderItem(
                                        f.getTitle(), f.getMediaCount(), false,
                                        "bilibili", f.getId());
                                biliContainer.addView(item);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            biliStatus.setText("加载失败");
                            LogDialog.error(requireContext(), error);
                        });
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    biliStatus.setText("登录信息失效，请重新绑定");
                    LogDialog.error(requireContext(), error);
                });
            }
        });
    }

    // ---------- 列表项生成 ----------

    private View createFolderItem(String name, int count, boolean deletable,
                                  String source, long mediaId) {
        View item = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_folder, null);

        TextView nameView = item.findViewById(R.id.textFolderName);
        TextView countView = item.findViewById(R.id.textFolderCount);
        ImageButton deleteBtn = item.findViewById(R.id.btnFolderDelete);

        nameView.setText(name);
        countView.setText(count > 0 ? count + " 首" : "");

        if (deletable) {
            deleteBtn.setVisibility(View.VISIBLE);
            deleteBtn.setOnClickListener(v ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle("删除收藏夹")
                            .setMessage("确定删除“" + name + "”吗？")
                            .setPositiveButton("删除", (d, w) -> {
                                localStore.removeFolder(name);
                                refreshLocalFolders();
                            })
                            .setNegativeButton("取消", null)
                            .show());
        } else {
            deleteBtn.setVisibility(View.GONE);
        }

        item.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FavDetailActivity.class);
            intent.putExtra(FavDetailActivity.EXTRA_SOURCE, source);
            intent.putExtra(FavDetailActivity.EXTRA_FOLDER_TITLE, name);
            if ("local".equals(source)) {
                intent.putExtra(FavDetailActivity.EXTRA_FOLDER_NAME, name);
            } else {
                intent.putExtra(FavDetailActivity.EXTRA_MEDIA_ID, mediaId);
            }
            startActivity(intent);
        });

        return item;
    }
}