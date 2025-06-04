package com.example.inforingkas.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.inforingkas.R;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.util.ThemeUtils; // Helper class untuk mendapatkan warna tema
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

    public void setBeritaList(List<Berita> beritaList) {
        this.beritaList.clear();
        if (beritaList != null) {
            this.beritaList.addAll(beritaList);
        }
        notifyDataSetChanged(); // Atau gunakan DiffUtil untuk performa lebih baik
    }

    public void updateFavoriteStatus(int position, boolean isFavorite) {
        if (position >= 0 && position < beritaList.size()) {
            beritaList.get(position).setFavorite(isFavorite);
            notifyItemChanged(position, "payload_favorite_changed"); // Payload untuk partial update
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
        TextView textViewJudul, textViewTanggal, textViewSumber, textViewKategori;
        MaterialButton buttonLihat, buttonRangkum;
        ImageButton buttonFavorite;
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
            textViewKategori = itemView.findViewById(R.id.text_view_kategori);
            buttonLihat = itemView.findViewById(R.id.button_lihat_berita);
            buttonRangkum = itemView.findViewById(R.id.button_rangkum_berita);
        }

        void bind(final Berita berita, final OnBeritaClickListener listener) {
            textViewJudul.setText(berita.getTitle());
            textViewTanggal.setText(berita.getPubDate()); // Format tanggal mungkin perlu disesuaikan
            textViewSumber.setText(String.format(context.getString(R.string.label_sumber), berita.getSourceName()));

            // Load image berita
            Glide.with(context)
                    .load(berita.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image) // Placeholder kustom
                    .error(R.drawable.ic_placeholder_image) // Error placeholder kustom
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            imageViewBerita.setBackgroundColor(ThemeUtils.getThemeColor(context, R.attr.colorSurface)); // Set background if image fails
                            return false; // penting untuk return false agar error() drawable ditampilkan
                        }
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            imageViewBerita.setBackgroundColor(Color.TRANSPARENT); // Hapus background jika gambar berhasil dimuat
                            return false;
                        }
                    })
                    .into(imageViewBerita);

            // Load source icon
            Glide.with(context)
                    .load(berita.getSourceIcon())
                    .placeholder(R.drawable.ic_placeholder_source_icon)
                    .error(R.drawable.ic_placeholder_source_icon)
                    .into(imageViewSourceIcon);

            // Kategori
            if (berita.getCategory() != null && !berita.getCategory().isEmpty()) {
                textViewKategori.setText(String.format(context.getString(R.string.label_kategori), TextUtils.join(", ", berita.getCategory())));
                textViewKategori.setVisibility(View.VISIBLE);
            } else {
                textViewKategori.setVisibility(View.GONE);
            }

            // Tombol Rangkum
            if (!TextUtils.isEmpty(berita.getRangkuman())) {
                buttonRangkum.setText(R.string.label_telah_dirangkum);
                // buttonRangkum.setEnabled(false); // Opsional: disable jika sudah dirangkum
            } else {
                buttonRangkum.setText(R.string.label_rangkum_berita);
                // buttonRangkum.setEnabled(true);
            }

            updateFavoriteIcon(berita.isFavorite());

            buttonLihat.setOnClickListener(v -> listener.onLihatBeritaClick(berita));
            buttonRangkum.setOnClickListener(v -> listener.onRangkumBeritaClick(berita));
            buttonFavorite.setOnClickListener(v -> listener.onFavoriteClick(berita, getAdapterPosition()));
        }

        void updateFavoriteIcon(boolean isFavorite) {
            if (isFavorite) {
                buttonFavorite.setImageResource(R.drawable.ic_baseline_favorite_24); // Ikon terisi
                ImageViewCompat.setImageTintList(buttonFavorite, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.favorite_active)));
            } else {
                buttonFavorite.setImageResource(R.drawable.ic_baseline_favorite_border_24); // Ikon border
                // Menggunakan warna dari atribut tema untuk inactive state
                int inactiveColor = ThemeUtils.getThemeColor(context, R.attr.favoriteInactiveColor);
                ImageViewCompat.setImageTintList(buttonFavorite, ColorStateList.valueOf(inactiveColor));
            }
        }
    }
}