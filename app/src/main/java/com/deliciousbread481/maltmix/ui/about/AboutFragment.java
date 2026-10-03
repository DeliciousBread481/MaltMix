package com.deliciousbread481.maltmix.ui.about;  
  
import android.content.Intent;  
import android.content.pm.PackageInfo;  
import android.content.pm.PackageManager;  
import android.net.Uri;  
import android.os.Bundle;  
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;  
  
import androidx.annotation.NonNull;  
import androidx.annotation.Nullable;  
import androidx.fragment.app.Fragment;  
  
import com.deliciousbread481.maltmix.databinding.FragmentAboutBinding;  
  
public class AboutFragment extends Fragment {  
  
    private FragmentAboutBinding binding;  
  
    @Nullable  
    @Override  
    public View onCreateView(@NonNull LayoutInflater inflater,  
                             @Nullable ViewGroup container,  
                             @Nullable Bundle savedInstanceState) {  
        binding = FragmentAboutBinding.inflate(inflater, container, false);  
        return binding.getRoot();  
    }  
  
    @Override  
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {  
        super.onViewCreated(view, savedInstanceState);  
        String version = "未知";  
        try {  
            PackageInfo info = requireContext().getPackageManager()  
                    .getPackageInfo(requireContext().getPackageName(), 0);  
            version = info.versionName;  
        } catch (PackageManager.NameNotFoundException ignored) {}  
        binding.textVersion.setText("版本 " + version);  
        binding.btnAuthor.setOnClickListener(v ->  
                startActivity(new Intent(Intent.ACTION_VIEW,  
                        Uri.parse("https://github.com/DeliciousBread481"))));
        binding.btnGithub.setOnClickListener(v ->  
                startActivity(new Intent(Intent.ACTION_VIEW,  
                        Uri.parse("https://github.com/DeliciousBread481/MaltMix"))));
    }  
  
    @Override  
    public void onDestroyView() {  
        super.onDestroyView();  
        binding = null;  
    }  
}