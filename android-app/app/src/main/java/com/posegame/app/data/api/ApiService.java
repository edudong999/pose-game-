package com.posegame.app.data.api;

import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.GameRecord;
import com.posegame.app.data.model.LeaderboardEntry;
import com.posegame.app.data.model.Level;
import com.posegame.app.data.model.LevelListResponse;
import com.posegame.app.data.model.PoseResult;
import com.posegame.app.data.model.PoseType;
import com.posegame.app.data.model.Sticker;
import com.posegame.app.data.model.UserInfo;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API service interface for Retrofit
 */
public interface ApiService {

    // ========== User Module ==========

    @POST("user/register")
    Call<ApiResponse<UserInfo>> register(@Body Map<String, String> params);

    @POST("user/login")
    Call<ApiResponse<UserInfo>> login(@Body Map<String, String> params);

    @GET("user/info")
    Call<ApiResponse<UserInfo>> getUserInfo(@Header("Authorization") String token);

    // ========== Level Module ==========

    @GET("levels")
    Call<ApiResponse<LevelListResponse>> getLevels();

    @GET("levels/{id}")
    Call<ApiResponse<Level>> getLevelDetail(@Path("id") int levelId);

    @GET("levels/pose/types")
    Call<ApiResponse<PoseTypeListResponse>> getPoseTypes();

    // ========== Pose Recognition ==========

    @Multipart
    @POST("pose/recognize")
    Call<ApiResponse<PoseResult>> recognizePose(
            @Part MultipartBody.Part image,
            @Query("level_id") int levelId
    );

    @Multipart
    @POST("pose/recognize/video")
    Call<ApiResponse<PoseResult>> recognizeVideo(
            @Part MultipartBody.Part video,
            @Query("level_id") int levelId
    );

    // ========== Game Record ==========

    @POST("game/record")
    Call<ApiResponse<GameRecordSubmitResponse>> submitRecord(
            @Header("Authorization") String token,
            @Body Map<String, Object> params
    );

    @GET("game/records")
    Call<ApiResponse<RecordListResponse>> getRecords(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("pageSize") int pageSize
    );

    @GET("game/leaderboard/{levelId}")
    Call<ApiResponse<LeaderboardResponse>> getLeaderboard(@Path("levelId") int levelId);

    // ========== Sticker ==========

    @GET("stickers")
    Call<ApiResponse<StickerListResponse>> getMyStickers(@Header("Authorization") String token);

    @GET("stickers/all")
    Call<ApiResponse<StickerListResponse>> getAllStickers();
}

// Response wrapper classes
class PoseTypeListResponse {
    private List<PoseType> list;
    public List<PoseType> getList() { return list; }
    public void setList(List<PoseType> list) { this.list = list; }
}

class RecordListResponse {
    private List<GameRecord> list;
    private int total;
    private int page;
    private int pageSize;
    public List<GameRecord> getList() { return list; }
    public void setList(List<GameRecord> list) { this.list = list; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}

class LeaderboardResponse {
    private int levelId;
    private String levelName;
    private List<LeaderboardEntry> ranking;
    public int getLevelId() { return levelId; }
    public void setLevelId(int levelId) { this.levelId = levelId; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public List<LeaderboardEntry> getRanking() { return ranking; }
    public void setRanking(List<LeaderboardEntry> ranking) { this.ranking = ranking; }
}

class StickerListResponse {
    private List<Sticker> list;
    private int total;
    public List<Sticker> getList() { return list; }
    public void setList(List<Sticker> list) { this.list = list; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}

class GameRecordSubmitResponse {
    private int recordId;
    private int score;
    private boolean isPass;
    private String stickerUnlocked;
    private List<String> newStickers;
    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isPass() { return isPass; }
    public void setPass(boolean pass) { isPass = pass; }
    public String getStickerUnlocked() { return stickerUnlocked; }
    public void setStickerUnlocked(String stickerUnlocked) { this.stickerUnlocked = stickerUnlocked; }
    public List<String> getNewStickers() { return newStickers; }
    public void setNewStickers(List<String> newStickers) { this.newStickers = newStickers; }
}