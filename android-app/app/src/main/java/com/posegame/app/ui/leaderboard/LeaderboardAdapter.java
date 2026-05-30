package com.posegame.app.ui.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posegame.app.R;
import com.posegame.app.data.model.LeaderboardEntry;

import java.util.List;

/**
 * Leaderboard Adapter
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.RankViewHolder> {

    private List<LeaderboardEntry> ranking;

    public LeaderboardAdapter(List<LeaderboardEntry> ranking) {
        this.ranking = ranking;
    }

    public void updateData(List<LeaderboardEntry> ranking) {
        this.ranking = ranking;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RankViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new RankViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankViewHolder holder, int position) {
        holder.bind(ranking.get(position));
    }

    @Override
    public int getItemCount() {
        return ranking.size();
    }

    static class RankViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivRankBadge;
        private final TextView tvRank;
        private final ImageView ivAvatar;
        private final TextView tvNickname;
        private final TextView tvScore;

        RankViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRankBadge = itemView.findViewById(R.id.ivRankBadge);
            tvRank = itemView.findViewById(R.id.tvRank);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            tvScore = itemView.findViewById(R.id.tvScore);
        }

        void bind(LeaderboardEntry entry) {
            int rank = entry.getRank();
            tvRank.setText(String.valueOf(rank));
            tvNickname.setText(entry.getNickname());
            tvScore.setText(entry.getScore() + "分");

            // Rank badge
            if (rank == 1) {
                ivRankBadge.setImageResource(R.drawable.ic_rank_gold);
                tvRank.setVisibility(View.GONE);
                ivRankBadge.setVisibility(View.VISIBLE);
            } else if (rank == 2) {
                ivRankBadge.setImageResource(R.drawable.ic_rank_silver);
                tvRank.setVisibility(View.GONE);
                ivRankBadge.setVisibility(View.VISIBLE);
            } else if (rank == 3) {
                ivRankBadge.setImageResource(R.drawable.ic_rank_bronze);
                tvRank.setVisibility(View.GONE);
                ivRankBadge.setVisibility(View.VISIBLE);
            } else {
                tvRank.setVisibility(View.VISIBLE);
                ivRankBadge.setVisibility(View.GONE);
            }
        }
    }
}