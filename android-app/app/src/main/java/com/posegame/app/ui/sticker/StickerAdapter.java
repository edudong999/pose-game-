package com.posegame.app.ui.sticker;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posegame.app.R;
import com.posegame.app.data.model.Sticker;

import java.util.List;

/**
 * Sticker Adapter for RecyclerView
 */
public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerViewHolder> {

    private final List<Sticker> stickers;

    StickerAdapter(List<Sticker> stickers) {
        this.stickers = stickers;
    }

    @NonNull
    @Override
    public StickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sticker, parent, false);
        return new StickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StickerViewHolder holder, int position) {
        holder.bind(stickers.get(position));
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    static class StickerViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivSticker;
        private final TextView tvName;

        StickerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSticker = itemView.findViewById(R.id.ivSticker);
            tvName = itemView.findViewById(R.id.tvStickerName);
        }

        void bind(Sticker sticker) {
            tvName.setText(sticker.getName());

            if (sticker.isUnlocked()) {
                ivSticker.setColorFilter(null);
                ivSticker.setImageResource(R.drawable.ic_sticker_placeholder);
                tvName.setTextColor(0xFF000000);
            } else {
                // Gray out locked stickers
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ivSticker.setColorFilter(new ColorMatrixColorFilter(matrix));
                ivSticker.setImageResource(R.drawable.ic_lock_gray);
                tvName.setText("???");
                tvName.setTextColor(0xFF9E9E9E);
            }
        }
    }
}