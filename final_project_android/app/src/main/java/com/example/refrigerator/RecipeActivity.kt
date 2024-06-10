package com.example.refrigerator

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.refrigerator.databinding.ActivityRecipeBinding
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class RecipeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeBinding
    private lateinit var detectedItems: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detectedItems = intent.getStringArrayListExtra("detectedItems") ?: arrayListOf()

        // ChatGPT API 호출
        val retrofitChatGPT = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val chatGPTService = retrofitChatGPT.create(ChatGPTService::class.java)

        val prompt = "I have ${detectedItems?.joinToString(", ")}. Suggest some Korean recipes using these ingredients."
        val request = ChatGPTRequest(
            model = "gpt-4o",
            prompt = prompt,
            max_tokens = 150,
            temperature = 0.7
        )

        chatGPTService.getRecipe(request).enqueue(object : Callback<ChatGPTResponse> {
            override fun onResponse(call: Call<ChatGPTResponse>, response: Response<ChatGPTResponse>) {
                val recipe = response.body()?.choices?.get(0)?.text ?: "No recipe found"
                binding.tvRecipeDescription.text = recipe
                searchYouTube(recipe)
            }

            override fun onFailure(call: Call<ChatGPTResponse>, t: Throwable) {
                binding.tvRecipeDescription.text = "Failed to load recipe"
            }
        })
    }

    // YouTube API 호출
    private fun searchYouTube(query: String) {
        val retrofitYouTube = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val youTubeService = retrofitYouTube.create(YouTubeService::class.java)

        youTubeService.searchVideos("snippet", query).enqueue(object : Callback<YouTubeSearchResponse> {
            override fun onResponse(call: Call<YouTubeSearchResponse>, response: Response<YouTubeSearchResponse>) {
                val videoId = response.body()?.items?.firstOrNull()?.id?.videoId
                if (videoId != null) {
                    loadYouTubeVideo(videoId)
                } else {
                    Toast.makeText(this@RecipeActivity, "No video found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<YouTubeSearchResponse>, t: Throwable) {
                Toast.makeText(this@RecipeActivity, "Failed to load video", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // YouTube 동영상 로드
    private fun loadYouTubeVideo(videoId: String) {
        val webView = binding.youtubeWebview
        webView.webViewClient = WebViewClient()
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        val html = """
            <!DOCTYPE html>
            <html>
                <style>
                    body, html {
                        margin: 0;
                        padding: 0;
                        height: 100%;
                        overflow: hidden;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                    }
                </style>
            <body>
            <iframe src="https://www.youtube.com/embed/$videoId" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
            </body>
            </html>
        """.trimIndent()

        webView.loadData(html, "text/html", "utf-8")
    }
}
