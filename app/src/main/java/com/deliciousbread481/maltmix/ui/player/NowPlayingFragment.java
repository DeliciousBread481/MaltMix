package com.deliciousbread481.maltmix.ui.player;  
  
import android.graphics.drawable.Drawable;  
import android.os.Bundle;  
import android.os.Handler;  
import android.os.Looper;  
import android.util.Log;  
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;  
import android.widget.PopupMenu;  
import android.widget.SeekBar;  
  
import androidx.annotation.NonNull;  
import androidx.annotation.Nullable;  
import androidx.appcompat.app.AlertDialog;  
import androidx.fragment.app.Fragment;  
import androidx.media3.common.C;  
import androidx.media3.common.Player;  
  
import com.bumptech.glide.Glide;  
import com.bumptech.glide.load.DataSource;  
import com.bumptech.glide.load.engine.GlideException;  
import com.bumptech.glide.load.model.GlideUrl;  
import com.bumptech.glide.load.model.LazyHeaders;  
import com.bumptech.glide.request.RequestListener;  
import com.bumptech.glide.request.target.Target;  
import com.deliciousbread481.maltmix.R;  
import com.deliciousbread481.maltmix.databinding.FragmentNowPlayingBinding;  
import com.deliciousbread481.maltmix.model.Song;  
import com.deliciousbread481.maltmix.util.LocalFavStore;  
import com.deliciousbread481.maltmix.util.LogDialog;  
import com.google.android.material.bottomsheet.BottomSheetDialog;
  
import java.util.List;  
  
public class NowPlayingFragment extends Fragment {  
  
    private static final String TAG = "MaltMix";  
    private FragmentNowPlayingBinding binding;  
    private PlayerViewModel playerViewModel;  
  
    private final Handler progressHandler = new Handler(Looper.getMainLooper());  
    private boolean userSeeking = false;  
  
    private final Runnable progressRunnable = new Runnable() {  
        @Override  
        public void run() {  
            updateSeekBar();  
            progressHandler.postDelayed(this, 500);  
        }  
    };  
  
    private final Player.Listener durationListener = new Player.Listener() {  
        @Override  
        public void onPlaybackStateChanged(int playbackState) {  
            if (playbackState == Player.STATE_READY) {  
                updateSeekBar();  
            }  
        }  
    };  
  
    @Nullable  
    @Override  
    public View onCreateView(@NonNull LayoutInflater inflater,  
                             @Nullable ViewGroup container,  
                             @Nullable Bundle savedInstanceState) {  
        binding = FragmentNowPlayingBinding.inflate(inflater, container, false);  
        return binding.getRoot();  
    }  
  
    @Override  
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {  
        super.onViewCreated(view, savedInstanceState);  
  
        playerViewModel = PlayerViewModel.getInstance(  
                requireActivity().getApplication());  
  
        binding.textSongName.setSelected(true);  
        binding.textArtist.setSelected(true);  
  
        playerViewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {  
            if (song != null) {  
                binding.textSongName.setText(song.getTitle());  
                binding.textArtist.setText(song.getArtist());  
                loadCover(song.getCoverUrl());  
            } else {  
                binding.textSongName.setText("暂无歌曲");  
                binding.textArtist.setText("");  
            }  
        });  
  
        playerViewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {  
            binding.btnPlayPause.setImageResource(  
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);  
        });  
  
        playerViewModel.getPlayMode().observe(getViewLifecycleOwner(), mode -> {  
            int icon;  
            if (mode == PlayerViewModel.MODE_SHUFFLE) {  
                icon = R.drawable.ic_shuffle;  
            } else if (mode == PlayerViewModel.MODE_REPEAT_ONE) {  
                icon = R.drawable.ic_repeat_one;  
            } else {  
                icon = R.drawable.ic_repeat;  
            }  
            binding.btnPlayMode.setImageResource(icon);  
        });  
  
        playerViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {  
            if (msg != null && !msg.isEmpty()) {  
                LogDialog.error(requireContext(), msg);  
            }  
        });  
  
        binding.btnPlayPause.setOnClickListener(v -> playerViewModel.togglePlayPause());  
        binding.btnPrevious.setOnClickListener(v -> playerViewModel.playPrevious());  
        binding.btnNext.setOnClickListener(v -> playerViewModel.playNext());  
        binding.btnPlayMode.setOnClickListener(v -> playerViewModel.cyclePlayMode());  
        binding.btnMore.setOnClickListener(v -> showOptionsMenu(v));  
  
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {  
            @Override  
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {  
                if (fromUser) {  
                    binding.textPosition.setText(formatTime(progress));  
                }  
            }  
  
            @Override  
            public void onStartTrackingTouch(SeekBar seekBar) {  
                userSeeking = true;  
            }  
  
            @Override  
            public void onStopTrackingTouch(SeekBar seekBar) {  
                playerViewModel.getPlayerManager().getPlayer()  
                        .seekTo(seekBar.getProgress());  
                userSeeking = false;  
                updateSeekBar();  
            }  
        });  
  
        playerViewModel.getPlayerManager().getPlayer().addListener(durationListener);  
        progressHandler.post(progressRunnable);  
    }  
  
    private void updateSeekBar() {  
        if (binding == null || userSeeking) return;  
        Player p = playerViewModel.getPlayerManager().getPlayer();  
        long duration = p.getDuration();  
        long position = p.getCurrentPosition();  
        if (duration != C.TIME_UNSET && duration > 0) {  
            binding.seekBar.setMax((int) duration);  
            binding.seekBar.setProgress((int) Math.min(position, duration));  
            binding.textDuration.setText(formatTime(duration));  
            binding.textPosition.setText(formatTime(position));  
        } else {  
            binding.seekBar.setMax(0);  
            binding.seekBar.setProgress(0);  
            binding.textDuration.setText("0:00");  
            binding.textPosition.setText("0:00");  
        }  
    }  
  
    private String formatTime(long ms) {  
        long totalSec = ms / 1000;  
        return (totalSec / 60) + ":" + String.format("%02d", totalSec % 60);  
    }  
  
    private void loadCover(String coverUrl) {  
        Log.d(TAG, "loadCover: " + coverUrl);  
  
        if (coverUrl == null || coverUrl.isEmpty()) {  
            LogDialog.warn(requireContext(), "封面 URL 为空，B站接口未返回 pic 字段");  
            binding.imageCover.setImageResource(R.drawable.ic_music_note);  
            return;  
        }  
  
        String httpsUrl = coverUrl.replace("http://", "https://");  
  
        GlideUrl glideUrl = new GlideUrl(  
                httpsUrl,  
                new LazyHeaders.Builder()  
                        .addHeader("Referer", "https://www.bilibili.com/")  
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "  
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "  
                                + "Chrome/120.0.0.0 Safari/537.36")  
                        .build());  
  
        Glide.with(this)  
                .load(glideUrl)  
                .placeholder(R.drawable.ic_music_note)  
                .error(R.drawable.ic_music_note)  
                .listener(new RequestListener<Drawable>() {  
                    @Override  
                    public boolean onLoadFailed(@Nullable GlideException e,  
                                                Object model,  
                                                @NonNull Target<Drawable> target,  
                                                boolean isFirstResource) {  
                        String err = "封面加载失败\n"  
                                + "URL: " + httpsUrl + "\n"  
                                + "错误: " + (e != null ? e.getMessage() : "未知");  
                        Log.e(TAG, err, e);  
                        LogDialog.error(requireContext(), err);  
                        return false;  
                    }  
  
                    @Override  
                    public boolean onResourceReady(@NonNull Drawable resource,  
                                                   @NonNull Object model,  
                                                   Target<Drawable> target,  
                                                   @NonNull DataSource dataSource,  
                                                   boolean isFirstResource) {  
                        Log.d(TAG, "封面加载成功，来源: " + dataSource);  
                        return false;  
                    }  
                })  
                .into(binding.imageCover);  
    }  
  
    private void showOptionsMenu(View anchor) {  
        Song song = playerViewModel.getCurrentSong().getValue();  
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());  
        View content = LayoutInflater.from(requireContext())  
                .inflate(R.layout.bottom_sheet_options, null);  
        sheet.setContentView(content);  
  
        android.widget.TextView title = content.findViewById(R.id.sheetTitle);  
        if (song != null) {  
            title.setText(song.getTitle() + " - " + song.getArtist());  
        }  
  
        content.findViewById(R.id.optionAddToFolder)  
                .setOnClickListener(v -> {  
                    sheet.dismiss();  
                    showAddToFolderDialog();  
                });  
  
        sheet.show();  
    }
  
    private void showAddToFolderDialog() {  
        Song song = playerViewModel.getCurrentSong().getValue();  
        if (song == null) {  
            LogDialog.warn(requireContext(), "当前没有正在播放的歌曲");  
            return;  
        }  
        LocalFavStore store = new LocalFavStore(requireContext());  
        List<String> folders = store.getFolderNames();  
        if (folders.isEmpty()) {  
            LogDialog.warn(requireContext(), "还没有本地收藏夹，请先在收藏夹页新建");  
            return;  
        }  
        String[] arr = folders.toArray(new String[0]);  
        new AlertDialog.Builder(requireContext())  
                .setTitle("收藏到")  
                .setItems(arr, (d, which) -> {  
                    boolean ok = store.addSong(arr[which], song);  
                    LogDialog.toast(requireContext(),  
                            ok ? "已加入「" + arr[which] + "」"  
                               : "「" + arr[which] + "」里已有此歌曲");
                })  
                .setNegativeButton("取消", null)  
                .show();  
    }  
  
    @Override  
    public void onDestroyView() {  
        super.onDestroyView();  
        progressHandler.removeCallbacks(progressRunnable);  
        if (playerViewModel != null) {  
            playerViewModel.getPlayerManager().getPlayer().removeListener(durationListener);  
        }  
        binding = null;  
    }  
}