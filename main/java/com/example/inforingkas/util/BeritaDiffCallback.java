package com.example.inforingkas.util;

import androidx.recyclerview.widget.DiffUtil;

import com.example.inforingkas.model.Berita;

import java.util.List;
import java.util.Objects;

public class BeritaDiffCallback extends DiffUtil.Callback {

    private final List<Berita> oldList;
    private final List<Berita> newList;

    public BeritaDiffCallback(List<Berita> oldList, List<Berita> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    /**
     * Dipanggil untuk memeriksa apakah dua item mewakili objek yang sama.
     * Kita menggunakan ID unik (articleId) untuk ini.
     */
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getArticleId().equals(newList.get(newItemPosition).getArticleId());
    }

    /**
     * Dipanggil HANYA JIKA areItemsTheSame() mengembalikan true.
     * Ini memeriksa apakah konten visual dari item tersebut telah berubah.
     * Misalnya, apakah status favoritnya berubah? Atau judulnya?
     */
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Berita oldBerita = oldList.get(oldItemPosition);
        Berita newBerita = newList.get(newItemPosition);

        // Bandingkan semua field yang mungkin berubah dan memengaruhi UI.
        return oldBerita.isFavorite() == newBerita.isFavorite() &&
                Objects.equals(oldBerita.getTitle(), newBerita.getTitle()) &&
                Objects.equals(oldBerita.getRangkuman(), newBerita.getRangkuman());
    }
}