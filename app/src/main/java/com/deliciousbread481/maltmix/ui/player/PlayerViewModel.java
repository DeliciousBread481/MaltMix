package com.deliciousbread481.maltmix.ui.player;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.Player;

import com.deliciousbread481.maltmix.api.BiliApi;
import com.deliciousbread481.maltmix.api.BiliAuthManager;
import com.deliciousbread481.maltmix.api.NeteaseApiClient;
import com.deliciousbread481.maltmix.api.NeteaseAuthManager;
import com.deliciousbread481.maltmix.model.Song;
import com.deliciousbread481.maltmix.player.PlayerManager;

import java.util.ArrayList;
import java.util.List;

public class PlayerViewModel extends AndroidViewModel {

    private static PlayerViewModel instance;

    public static synchronized PlayerViewModel getInstance(Application app) {
        if (instance == null) {
            instance = new PlayerViewModel(app);
        }
        return instance;
    }

    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> playlist = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final PlayerManager playerManager;
    private int currentIndex = -1;

    public PlayerViewModel(@NonNull Application application) {
        super(application);
        playerManager = PlayerManager.getInstance(application);

        // 冷启动恢复网易云 Cookie
        NeteaseAuthManager na = new NeteaseAuthManager(application);
        if (na.isLoggedIn()) {
            NeteaseApiClient.setCookie(na.getCookie());
        }

        playerManager.getPlayer().addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying.setValue(playing);
            }
        });

        playerManager.setErrorListener(message -> errorMessage.postValue(message));
    }

    public LiveData<Song> getCurrentSong() { return currentSong; }
    public LiveData<List<Song>> getPlaylist() { return playlist; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void togglePlayPause() {
        if (!playerManager.hasMedia()) {
            Song current = currentSong.getValue();
            if (current != null) {
                playSong(current);
            } else {
                errorMessage.setValue("播放列表为空，请先添加歌曲");
            }
            return;
        }
        playerManager.togglePlayPause();
    }

    public void playNext() {
        List<Song> list = playlist.getValue();
        if (list == null || list.isEmpty()) return;
        currentIndex = (currentIndex + 1) % list.size();
        playSong(list.get(currentIndex));
    }

    public void playPrevious() {
        List<Song> list = playlist.getValue();
        if (list == null || list.isEmpty()) return;
        currentIndex = (currentIndex - 1 + list.size()) % list.size();
        playSong(list.get(currentIndex));
    }

    public void playSong(Song song) {
        List<Song> list = playlist.getValue();
        if (list != null) currentIndex = list.indexOf(song);
        currentSong.setValue(song);

        if (song.getPlayUrl() != null && !song.getPlayUrl().isEmpty()) {
            playerManager.playUrl(song.getPlayUrl());
            return;
        }

        if ("bilibili".equals(song.getSource())) {
            fetchBiliAndPlay(song);
        } else if ("netease".equals(song.getSource())) {
            fetchNeteaseAndPlay(song);
        } else {
            errorMessage.setValue("暂不支持的音源：" + song.getSource());
        }
    }

    // ---------- 网易云 ----------

    private void fetchNeteaseAndPlay(Song song) {
        isLoading.setValue(true);
        NeteaseApiClient.getSongUrl(song.getId(), new NeteaseApiClient.SongUrlCallback() {
            @Override
            public void onSuccess(String url) {
                isLoading.setValue(false);
                song.setPlayUrl(url);
                playerManager.playUrl(url);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // ---------- B站 ----------

    private void fetchBiliAndPlay(Song song) {
        String id = song.getId();
        if (id == null || id.isEmpty()) {
            errorMessage.setValue("歌曲 ID 为空");
            return;
        }

        String bvid;
        long cid = 0;
        int colonIdx = id.indexOf(':');
        if (colonIdx >= 0) {
            bvid = id.substring(0, colonIdx);
            try {
                cid = Long.parseLong(id.substring(colonIdx + 1));
            } catch (NumberFormatException ignored) {
                cid = 0;
            }
        } else {
            bvid = id;
        }

        if (bvid.isEmpty()) {
            errorMessage.setValue("歌曲 ID 格式错误：" + id);
            return;
        }

        isLoading.setValue(true);
        BiliAuthManager auth = new BiliAuthManager(getApplication());

        if (cid > 0) {
            doFetchBiliAudio(song, bvid, cid, auth);
        } else {
            BiliApi.fetchVideoInfo(bvid,
                    auth.getSessdata(), auth.getImgKey(), auth.getSubKey(),
                    new BiliApi.VideoInfoCallback() {
                @Override
                public void onSuccess(String title, String cover, long fetchedCid) {
                    if (fetchedCid <= 0) {
                        isLoading.setValue(false);
                        errorMessage.setValue("无法获取视频 cid：" + bvid);
                        return;
                    }
                    doFetchBiliAudio(song, bvid, fetchedCid, auth);
                }

                @Override
                public void onFailure(String error) {
                    isLoading.setValue(false);
                    errorMessage.setValue(error);
                }
            });
        }
    }

    private void doFetchBiliAudio(Song song, String bvid, long cid,
                                  BiliAuthManager auth) {
        BiliApi.fetchAudioUrl(bvid, cid,
                auth.getSessdata(), auth.getImgKey(), auth.getSubKey(),
                auth.getBuvid3(),
                new BiliApi.AudioUrlCallback() {
            @Override
            public void onSuccess(String audioUrl) {
                isLoading.setValue(false);
                song.setPlayUrl(audioUrl);
                playerManager.playUrl(audioUrl);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void addBiliSong(String bvid) {
        isLoading.setValue(true);

        BiliAuthManager auth = new BiliAuthManager(getApplication());

        BiliApi.fetchVideoInfo(bvid,
                auth.getSessdata(), auth.getImgKey(), auth.getSubKey(),
                new BiliApi.VideoInfoCallback() {
            @Override
            public void onSuccess(String title, String cover, long cid) {
                isLoading.setValue(false);
                if (cid <= 0) {
                    errorMessage.setValue("无法获取 cid，video info 返回 cid=0"
                            + "\n\n标题：" + title
                            + "\n封面：" + cover);
                    return;
                }
                String id = bvid + ":" + cid;
                List<Song> list = playlist.getValue();
                if (list != null) {
                    for (Song s : list) {
                        if (s.getId().equals(id)) {
                            errorMessage.setValue("列表中已存在该歌曲");
                            return;
                        }
                    }
                }
                Song song = new Song(id, title, "B站UP主", cover, null, "bilibili");
                addSong(song);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void addSong(Song song) {
        List<Song> list = new ArrayList<>(playlist.getValue());
        list.add(song);
        playlist.setValue(list);
        if (list.size() == 1) {
            currentSong.setValue(song);
        }
    }

    public void removeSong(Song song) {
        List<Song> list = new ArrayList<>(playlist.getValue());
        list.remove(song);
        playlist.setValue(list);

        Song current = currentSong.getValue();
        if (current != null && current.equals(song)) {
            if (list.isEmpty()) {
                currentSong.setValue(null);
                playerManager.stop();
            } else {
                playNext();
            }
        }
    }

    /** 用一批歌曲替换整个收听列表 */
    public void setPlaylist(List<Song> songs) {
        List<Song> list = new ArrayList<>(songs);
        playlist.setValue(list);
        if (!list.isEmpty()) {
            currentIndex = 0;
            currentSong.setValue(list.get(0));
        } else {
            currentIndex = -1;
            currentSong.setValue(null);
        }
        playerManager.stop();
    }
}