package com.example.refrigerator

import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.refrigerator.databinding.ActivityMarketBinding

class MarketActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarketBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.wvMarket.webViewClient = WebViewClient()
        binding.wvMarket.loadUrl("https://www.coupang.com")
    }
}