package com.deliciousbread481.maltmix.ui.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.deliciousbread481.maltmix.databinding.FragmentPlayerBinding;
import com.deliciousbread481.maltmix.model.Song;

import java.util.List;

public class PlayerFragment extends Fragment {

    private FragmentPlayerBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PlayerViewModel vm = PlayerViewModel.getInstance(
            requireActivity().getApplication());
        List<Song> list = vm.getPlaylist().getValue();
        if (list == null || list.isEmpty()) {
            vm.addBiliSong("BV1g5kjBTEV7");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        binding.viewPager.setAdapter(new PlayerPagerAdapter(this));
        binding.viewPager.setCurrentItem(0, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class PlayerPagerAdapter extends FragmentStateAdapter {
        public PlayerPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new NowPlayingFragment();
            } else {
                return new PlaylistFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}