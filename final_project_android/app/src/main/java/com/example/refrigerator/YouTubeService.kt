package com.example.refrigerator

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class YouTubeSearchResponse(val items: List<YouTubeVideo>)
data class YouTubeVideo(val id: YouTubeVideoId)
data class YouTubeVideoId(val videoId: String)

interface YouTubeService {
    @GET("youtube/v3/search")
    fun searchVideos(
        @Query("part") part: String,
        @Query("q") query: String,
        @Query("key") apiKey: String = Constants.YOUTUBE_API_KEY,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 1
    ): Call<YouTubeSearchResponse>
}
