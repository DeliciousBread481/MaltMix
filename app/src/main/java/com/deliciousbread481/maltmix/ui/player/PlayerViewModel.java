package com.deliciousbread481.maltmix.ui.player;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.Player;

import com.deliciousbread481.maltmix.api.BiliApi;
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
        } else {
            errorMessage.setValue("暂不支持的音源：" + song.getSource());
        }
    }

    private void fetchBiliAndPlay(Song song) {
        String[] parts = song.getId().split(":");
        if (parts.length < 2) {
            errorMessage.setValue("歌曲 ID 格式错误");
            return;
        }
        String bvid = parts[0];
        long cid;
        try {
            cid = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            errorMessage.setValue("cid 解析失败");
            return;
        }

        isLoading.setValue(true);
        BiliApi.fetchAudioUrl(bvid, cid, new BiliApi.AudioUrlCallback() {
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
        BiliApi.fetchVideoInfo(bvid, new BiliApi.VideoInfoCallback() {
            @Override
            public void onSuccess(String title, String cover, long cid) {
                isLoading.setValue(false);
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