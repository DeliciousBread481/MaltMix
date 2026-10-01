package com.deliciousbread481.maltmix.ui.player;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.deliciousbread481.maltmix.databinding.FragmentNowPlayingBinding;
import com.deliciousbread481.maltmix.util.LogDialog;

public class NowPlayingFragment extends Fragment {

    private static final String TAG = "MaltMix";
    private FragmentNowPlayingBinding binding;
    private PlayerViewModel playerViewModel;

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
                    isPlaying ? android.R.drawable.ic_media_pause
                              : android.R.drawable.ic_media_play);
        });

        playerViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                LogDialog.error(requireContext(), msg);
            }
        });

        binding.btnPlayPause.setOnClickListener(v -> playerViewModel.togglePlayPause());
        binding.btnPrevious.setOnClickListener(v -> playerViewModel.playPrevious());
        binding.btnNext.setOnClickListener(v -> playerViewModel.playNext());
    }

    private void loadCover(String coverUrl) {
        Log.d(TAG, "loadCover: " + coverUrl);

        if (coverUrl == null || coverUrl.isEmpty()) {
            LogDialog.warn(requireContext(), "封面 URL 为空，B站接口未返回 pic 字段");
            binding.imageCover.setImageResource(android.R.drawable.ic_menu_gallery);
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
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
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
                        return false; // false 表示继续走 error() 占位图
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}