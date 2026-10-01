package com.deliciousbread481.maltmix.ui.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.deliciousbread481.maltmix.R;
import com.deliciousbread481.maltmix.model.Song;

public class PlaylistAdapter extends ListAdapter<Song, PlaylistAdapter.ViewHolder> {

    private final PlayerViewModel viewModel;

    private static final DiffUtil.ItemCallback<Song> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Song>() {
                @Override
                public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getArtist().equals(newItem.getArtist());
                }
            };

    public PlaylistAdapter(PlayerViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = getItem(position);
        holder.textSongName.setText(song.getTitle());
        holder.textArtist.setText(song.getArtist());

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                viewModel.removeSong(getItem(pos));
            }
        });

        holder.itemView.setOnClickListener(v -> viewModel.playSong(song));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textSongName;
        final TextView textArtist;
        final ImageButton btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSongName = itemView.findViewById(R.id.textItemSongName);
            textArtist = itemView.findViewById(R.id.textItemArtist);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}