package com.deliciousbread481.maltmix.ui.favorites;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.deliciousbread481.maltmix.R;
import com.deliciousbread481.maltmix.model.Song;
import com.deliciousbread481.maltmix.util.LocalFavStore;
import com.deliciousbread481.maltmix.util.LogDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;  

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavDetailAdapter extends RecyclerView.Adapter<FavDetailAdapter.ViewHolder> {

    private final Context context;
    private final LocalFavStore localStore;
    private final List<Song> songs = new ArrayList<>();
    private String folderName;
    private boolean reorderMode = false;
    private ItemTouchHelper touchHelper;

    public FavDetailAdapter(Context context) {
        this.context = context;
        this.localStore = new LocalFavStore(context);
    }
    
    public void setFolderName(String folderName) {  
        this.folderName = folderName;  
    }

    public void attachTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
    }

    public void setSongs(List<Song> newSongs) {
        songs.clear();
        songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public boolean toggleReorderMode() {
        reorderMode = !reorderMode;
        notifyDataSetChanged();
        return reorderMode;
    }

    public void moveItem(int from, int to) {
        if (from < 0 || to < 0 || from >= songs.size() || to >= songs.size()) return;
        Song s = songs.remove(from);
        songs.add(to, s);
        notifyItemMoved(from, to);
    }
    
    public void reverseAll() {  
        Collections.reverse(songs);  
        notifyDataSetChanged();  
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_song_with_menu, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Song song = songs.get(position);
        h.textTitle.setText(song.getTitle());
        h.textArtist.setText(song.getArtist());

        if (reorderMode) {
            h.btnDrag.setVisibility(View.VISIBLE);
            h.btnMenu.setVisibility(View.GONE);
            h.btnDrag.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (touchHelper != null) {
                        touchHelper.startDrag(h);
                    }
                }
                return false;
            });
        } else {
            h.btnDrag.setVisibility(View.GONE);
            h.btnMenu.setVisibility(View.VISIBLE);
            h.btnMenu.setOnClickListener(v -> showMenu(h, song));
        }
    }

    private void showMenu(ViewHolder h, Song song) {  
        BottomSheetDialog sheet = new BottomSheetDialog(context);  
        View content = LayoutInflater.from(context)  
                .inflate(R.layout.bottom_sheet_song_options, null);  
        sheet.setContentView(content);  
  
        TextView title = content.findViewById(R.id.sheetTitle);  
        title.setText(song.getTitle() + " - " + song.getArtist());  
  
        content.findViewById(R.id.optionAddToFolder)  
                .setOnClickListener(v -> {  
                    sheet.dismiss();  
                    showAddToFolderDialog(song);  
                });  
  
        View optionRemove = content.findViewById(R.id.optionRemove);  
        if (folderName == null) {  
            optionRemove.setVisibility(View.GONE);  
        } else {  
            optionRemove.setOnClickListener(v -> {  
                sheet.dismiss();  
                new AlertDialog.Builder(context)  
                        .setTitle("删除曲目")  
                        .setMessage("确定从「" + folderName + "」中删除\n"  
                                + song.getTitle() + " 吗？")  
                        .setPositiveButton("删除", (d, w) -> removeSong(h, song))  
                        .setNegativeButton("取消", null)  
                        .show();  
            });  
        }  
  
        sheet.show();  
    }  
  
    private void removeSong(ViewHolder h, Song song) {  
        int pos = h.getBindingAdapterPosition();  
        if (pos == RecyclerView.NO_POSITION) return;  
        songs.remove(pos);  
        notifyItemRemoved(pos);  
        localStore.removeSong(folderName, song);  
        LogDialog.toast(context, "已删除「" + song.getTitle() + "」");  
    }

    private void showAddToFolderDialog(Song song) {
        List<String> folders = localStore.getFolderNames();
        if (folders.isEmpty()) {
            LogDialog.warn(context, "还没有本地收藏夹，请先在收藏夹页新建");
            return;
        }
        String[] arr = folders.toArray(new String[0]);
        new AlertDialog.Builder(context)
                .setTitle("收藏到")
                .setItems(arr, (d, which) -> {  
                    String folderName = arr[which];  
                    boolean ok = localStore.addSong(folderName, song);  
                    if (ok) {  
                        LogDialog.toast(context, "已加入「" + folderName + "」");  
                    } else {  
                        LogDialog.toast(context, "「" + folderName + "」里已有此歌曲");  
                    }  
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textArtist;
        final ImageButton btnMenu;
        final ImageView btnDrag;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textArtist = itemView.findViewById(R.id.textArtist);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            btnDrag = itemView.findViewById(R.id.btnDrag);
        }
    }
}