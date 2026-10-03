package com.deliciousbread481.maltmix.ui.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;  
import com.deliciousbread481.maltmix.databinding.FragmentPlaylistBinding;
import com.deliciousbread481.maltmix.R;

public class PlaylistFragment extends Fragment {

    private FragmentPlaylistBinding binding;
    private PlayerViewModel playerViewModel;
    private PlaylistAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerViewModel = PlayerViewModel.getInstance(
            requireActivity().getApplication());

        adapter = new PlaylistAdapter(playerViewModel, () -> {  
            Fragment parent = getParentFragment();  
            if (parent != null && parent.getView() != null) {  
                ViewPager2 vp = parent.getView().findViewById(R.id.viewPager);  
                if (vp != null) {  
                    vp.setCurrentItem(0, true);  
                }  
            }  
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
        
        playerViewModel.getPlaylist().observe(getViewLifecycleOwner(), songs -> {
            adapter.submitList(songs);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}