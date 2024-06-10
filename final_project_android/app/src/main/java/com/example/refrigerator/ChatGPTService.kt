package com.example.refrigerator

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class ChatGPTRequest(val model: String, val prompt: String, val max_tokens: Int, val temperature: Double)
data class ChatGPTResponse(val choices: List<Choice>)
data class Choice(val text: String)

interface ChatGPTService {
    @Headers("Content-Type: application/json", "Authorization: Bearer ${Constants.CHATGPT_API_KEY}")
    @POST("v1/completions")
    fun getRecipe(@Body request: ChatGPTRequest): Call<ChatGPTResponse>
}
