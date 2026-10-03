package com.deliciousbread481.maltmix;

import android.os.Bundle;  
import android.view.Menu;  
import android.view.View;  
import android.widget.ImageView;  
import android.widget.TextView;  
import androidx.appcompat.app.AppCompatActivity;  
import androidx.drawerlayout.widget.DrawerLayout;  
import androidx.navigation.NavController;  
import androidx.navigation.Navigation;  
import androidx.navigation.ui.AppBarConfiguration;  
import androidx.navigation.ui.NavigationUI;  
import com.bumptech.glide.Glide;  
import com.bumptech.glide.load.model.GlideUrl;  
import com.bumptech.glide.load.model.LazyHeaders;  
import com.deliciousbread481.maltmix.api.BiliApi;  
import com.deliciousbread481.maltmix.api.BiliAuthManager;  
import com.deliciousbread481.maltmix.api.NeteaseAuthManager;  
import com.deliciousbread481.maltmix.databinding.ActivityMainBinding;  
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(  
            R.id.nav_player)  
            .setOpenableLayout(drawer)  
            .build();

        NavController navController = Navigation.findNavController(
                this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        
        updateNavHeader();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(
                this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

@Override  
    protected void onResume() {  
        super.onResume();  
        updateNavHeader();  
    }  
  
    private void updateNavHeader() {  
        View header = binding.navView.getHeaderView(0);  
        if (header == null) return;  
        ImageView avatar = header.findViewById(R.id.nav_header_avatar);  
        TextView username = header.findViewById(R.id.nav_header_username);  
        TextView biliStatus = header.findViewById(R.id.nav_header_bili_status);  
        TextView neteaseStatus = header.findViewById(R.id.nav_header_netease_status);  
  
        NeteaseAuthManager na = new NeteaseAuthManager(this);  
        neteaseStatus.setText(na.isLoggedIn()  
                ? "网易云音乐：已登录" : "网易云音乐：未登录");  
  
        BiliAuthManager ba = new BiliAuthManager(this);  
        if (!ba.isLoggedIn()) {  
            username.setText("未登录");  
            biliStatus.setText("哔哩哔哩：未登录");  
            avatar.setImageResource(R.drawable.ic_account_circle);  
            return;  
        }  
        BiliApi.fetchUserInfo(ba.getSessdata(), new BiliApi.UserInfoCallback() {  
            @Override  
            public void onSuccess(long mid, String uname, String face,  
                                  String imgKey, String subKey) {  
                if (isFinishing() || isDestroyed()) return;  
                ba.setWbiKeys(imgKey, subKey);  
                username.setText(uname);  
                biliStatus.setText("哔哩哔哩：已登录");  
                if (face != null && !face.isEmpty()) {  
                    GlideUrl glideUrl = new GlideUrl(  
                            face.replace("http://", "https://"),  
                            new LazyHeaders.Builder()  
                                    .addHeader("Referer", "https://www.bilibili.com/")  
                                    .build());  
                    Glide.with(MainActivity.this)  
                            .load(glideUrl)  
                            .circleCrop()  
                            .placeholder(R.drawable.ic_account_circle)  
                            .error(R.drawable.ic_account_circle)  
                            .into(avatar);  
                }  
            }  
  
            @Override  
            public void onFailure(String error) {  
                username.setText("未登录");  
                biliStatus.setText("哔哩哔哩：登录已失效");  
                avatar.setImageResource(R.drawable.ic_account_circle);  
            }  
        });  
    }
}