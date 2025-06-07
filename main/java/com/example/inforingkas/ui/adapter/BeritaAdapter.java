package com.example.inforingkas.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.inforingkas.R;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.util.BeritaDiffCallback;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class BeritaAdapter extends RecyclerView.Adapter<BeritaAdapter.BeritaViewHolder> {

    private List<Berita> beritaList;
    private final Context context;
    private final OnBeritaClickListener listener;

    public interface OnBeritaClickListener {
        void onLihatBeritaClick(Berita berita);
        void onRangkumBeritaClick(Berita berita);
        void onFavoriteClick(Berita berita, int position);
    }

    public BeritaAdapter(Context context, OnBeritaClickListener listener) {
        this.context = context;
        this.beritaList = new ArrayList<>();
        this.listener = listener;
    }

    public void submitList(List<Berita> newList) {
        BeritaDiffCallback diffCallback = new BeritaDiffCallback(this.beritaList, newList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.beritaList.clear();
        this.beritaList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateFavoriteStatus(int position, boolean isFavorite) {
        if (position >= 0 && position < beritaList.size()) {
            beritaList.get(position).setFavorite(isFavorite);
            notifyItemChanged(position, "payload_favorite_changed");
        }
    }

    @NonNull
    @Override
    public BeritaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_berita, parent, false);
        return new BeritaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BeritaViewHolder holder, int position) {
        Berita berita = beritaList.get(position);
        holder.bind(berita, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull BeritaViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains("payload_favorite_changed")) {
            Berita berita = beritaList.get(position);
            holder.updateFavoriteIcon(berita.isFavorite());
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return beritaList.size();
    }

    static class BeritaViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewBerita, imageViewSourceIcon;
        TextView textViewJudul, textViewTanggal, textViewSumber;
        MaterialButton buttonLihat, buttonRangkum, buttonFavorite;
        Context context;

        BeritaViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            imageViewBerita = itemView.findViewById(R.id.image_view_berita);
            textViewJudul = itemView.findViewById(R.id.text_view_judul_berita);
            buttonFavorite = itemView.findViewById(R.id.button_favorite);
            textViewTanggal = itemView.findViewById(R.id.text_view_tanggal_publish);
            imageViewSourceIcon = itemView.findViewById(R.id.image_view_source_icon);
            textViewSumber = itemView.findViewById(R.id.text_view_source_name);
            buttonLihat = itemView.findViewById(R.id.button_lihat_berita);
            buttonRangkum = itemView.findViewById(R.id.button_rangkum_berita);
        }

        void bind(final Berita berita, final OnBeritaClickListener listener) {
            textViewJudul.setText(berita.getTitle());
            textViewTanggal.setText(berita.getPubDate());
            textViewSumber.setText(berita.getSourceName());

            Glide.with(context)
                    .load(berita.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .into(imageViewBerita);

            Glide.with(context)
                    .load(berita.getSourceIcon())
                    .placeholder(R.drawable.ic_placeholder_source_icon)
                    .error(R.drawable.ic_placeholder_source_icon)
                    .into(imageViewSourceIcon);

            if (!TextUtils.isEmpty(berita.getRangkuman())) {
                buttonRangkum.setText(R.string.label_telah_dirangkum);
            } else {
                buttonRangkum.setText(R.string.label_rangkum_berita);
            }

            updateFavoriteIcon(berita.isFavorite());

            buttonLihat.setOnClickListener(v -> listener.onLihatBeritaClick(berita));
            buttonRangkum.setOnClickListener(v -> listener.onRangkumBeritaClick(berita));
            buttonFavorite.setOnClickListener(v -> listener.onFavoriteClick(berita, getAdapterPosition()));
        }

        void updateFavoriteIcon(boolean isFavorite) {
            if (isFavorite) {
                buttonFavorite.setIconResource(R.drawable.ic_baseline_favorite_24);
                buttonFavorite.setIconTint(ContextCompat.getColorStateList(context, R.color.favorite_active));
            } else {
                buttonFavorite.setIconResource(R.drawable.ic_baseline_favorite_border_24);
                buttonFavorite.setIconTint(null);
            }
        }
    }
}