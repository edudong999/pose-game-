package com.posegame.app.data.api;

import com.posegame.app.data.model.ApiResponse;
import com.posegame.app.data.model.GameRecord;
import com.posegame.app.data.model.GameRecordSubmitResponse;
import com.posegame.app.data.model.LeaderboardEntry;
import com.posegame.app.data.model.LeaderboardResponse;
import com.posegame.app.data.model.Level;
import com.posegame.app.data.model.LevelListResponse;
import com.posegame.app.data.model.PoseResult;
import com.posegame.app.data.model.PoseType;
import com.posegame.app.data.model.PoseTypeListResponse;
import com.posegame.app.data.model.RecordListResponse;
import com.posegame.app.data.model.RealtimePoseRequest;
import com.posegame.app.data.model.RealtimePoseResult;
import com.posegame.app.data.model.Sticker;
import com.posegame.app.data.model.StickerListResponse;
import com.posegame.app.data.model.UploadImageResponse;
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
    Call<ApiResponse<LevelListResponse>> getLevels(@Header("Authorization") String token);

    @GET("levels/{id}")
    Call<ApiResponse<Level>> getLevelDetail(@Path("id") int levelId);

    @GET("levels/pose/types")
    Call<ApiResponse<PoseTypeListResponse>> getPoseTypes();

    // ========== Pose Recognition ==========

    @POST("pose/recognize")
    Call<ApiResponse<PoseResult>> recognizePoseKeypoints(
            @Body RealtimePoseRequest keypoints,
            @Query("level_id") int levelId
    );

    @Multipart
    @POST("pose/recognize/video")
    Call<ApiResponse<PoseResult>> recognizeVideo(
            @Part MultipartBody.Part video,
            @Query("level_id") int levelId
    );

    @POST("pose/analyze/realtime")
    Call<ApiResponse<RealtimePoseResult>> analyzeRealtimePose(
            @Body RealtimePoseRequest request,
            @Query("level_id") int levelId
    );

    // ========== Media Upload ==========

    @Multipart
    @POST("game/record/media")
    Call<ApiResponse<UploadImageResponse>> uploadImage(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image);

    // ========== Game Record ==========

    @POST("game/record")
    Call<ApiResponse<GameRecordSubmitResponse>> submitRecord(
            @Header("Authorization") String token,
            @Query("levelId") int levelId,
            @Query("score") int score,
            @Query("isPass") boolean isPass,
            @Query("mediaUrl") String mediaUrl
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

// Response wrapper classes - using public classes from ResponseModels.java
// PoseTypeListResponse, RecordListResponse, LeaderboardResponse, StickerListResponse, GameRecordSubmitResponse are defined in ResponseModels.java