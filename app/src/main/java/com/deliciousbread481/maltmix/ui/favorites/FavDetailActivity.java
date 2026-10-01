package com.deliciousbread481.maltmix.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.deliciousbread481.maltmix.R;
import com.deliciousbread481.maltmix.api.BiliApi;
import com.deliciousbread481.maltmix.api.BiliAuthManager;
import com.deliciousbread481.maltmix.model.FavVideo;
import com.deliciousbread481.maltmix.model.Song;
import com.deliciousbread481.maltmix.ui.player.PlayerViewModel;
import com.deliciousbread481.maltmix.util.LocalFavStore;
import com.deliciousbread481.maltmix.util.LogDialog;

import java.util.ArrayList;
import java.util.List;

public class FavDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_FOLDER_NAME = "folder_name";
    public static final String EXTRA_FOLDER_TITLE = "folder_title";
    public static final String EXTRA_MEDIA_ID = "media_id";

    private FavDetailAdapter adapter;
    private LocalFavStore localStore;
    private BiliAuthManager biliAuth;
    private PlayerViewModel playerViewModel;

    private String source;
    private String folderName;
    private String folderTitle;
    private long mediaId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_detail);

        Intent intent = getIntent();
        source = intent.getStringExtra(EXTRA_SOURCE);
        folderName = intent.getStringExtra(EXTRA_FOLDER_NAME);
        folderTitle = intent.getStringExtra(EXTRA_FOLDER_TITLE);
        mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, 0);

        localStore = new LocalFavStore(this);
        biliAuth = new BiliAuthManager(this);

        Toolbar toolbar = findViewById(R.id.toolbarFavDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(folderTitle);
        }

        RecyclerView recycler = findViewById(R.id.recyclerFavDetail);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavDetailAdapter(this);
        recycler.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                adapter.moveItem(
                        vh.getBindingAdapterPosition(),
                        target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(recycler);
        adapter.attachTouchHelper(touchHelper);

        Button btnSetPlaylist = findViewById(R.id.btnSetPlaylist);
        Button btnReorder = findViewById(R.id.btnReorder);

        playerViewModel = PlayerViewModel.getInstance(getApplication());

        btnSetPlaylist.setOnClickListener(v -> {
            List<Song> songs = adapter.getSongs();
            if (songs.isEmpty()) {
                Toast.makeText(this, "列表为空", Toast.LENGTH_SHORT).show();
                return;
            }
            playerViewModel.setPlaylist(songs);
            Toast.makeText(this, "已设为收听列表", Toast.LENGTH_SHORT).show();
        });

        if ("local".equals(source)) {
            btnReorder.setVisibility(Button.VISIBLE);
            btnReorder.setOnClickListener(v -> {
                boolean isReorder = adapter.toggleReorderMode();
                btnReorder.setText(isReorder ? "完成" : "调整顺序");
            });
        } else {
            btnReorder.setVisibility(Button.GONE);
        }

        if ("local".equals(source)) {
            adapter.setSongs(localStore.getSongs(folderName));
        } else if ("bilibili".equals(source)) {
            loadBiliVideos();
        }
    }

    private void loadBiliVideos() {
        String sessdata = biliAuth.getSessdata();
        if (sessdata == null) {
            LogDialog.error(this, "B站未登录");
            return;
        }
        BiliApi.fetchFavVideos(mediaId, sessdata, new BiliApi.FavVideoCallback() {
            @Override
            public void onSuccess(List<FavVideo> videos) {
                List<Song> songs = new ArrayList<>();
                for (FavVideo v : videos) {
                    songs.add(new Song(
                            v.getBvid() + ":" + v.getCid(),
                            v.getTitle(),
                            v.getUpperName(),
                            v.getCover(),
                            null,
                            "bilibili"
                    ));
                }
                adapter.setSongs(songs);
            }

            @Override
            public void onFailure(String error) {
                LogDialog.error(FavDetailActivity.this, error);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 本地收藏夹退出时保存当前顺序
        if ("local".equals(source) && adapter != null) {
            localStore.saveSongs(folderName, adapter.getSongs());
        }
    }
}