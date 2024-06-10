package com.example.refrigerator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.refrigerator.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var objectDetectionHelper: ObjectDetectionHelper
    private lateinit var cameraExecutor: ExecutorService
    private val detectedItems = mutableListOf<DetectionResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        get_permission()

        objectDetectionHelper = ObjectDetectionHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.btnRecipe.setOnClickListener {
            val intent = Intent(this, RecipeActivity::class.java)
            val detectedLabels = ArrayList(detectedItems.map { it.label })
            intent.putStringArrayListExtra("detectedItems", detectedLabels)
            startActivity(intent)
        }

        binding.btnMarket.setOnClickListener {
            val intent = Intent(this, MarketActivity::class.java)
            startActivity(intent)
        }

    }

    fun get_permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            get_permission()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer { bitmap ->
                        val detectedResults = objectDetectionHelper.detectObjects(bitmap)
                        synchronized(detectedItems) {
                            detectedItems.clear()
                            detectedItems.addAll(detectedResults)
                        }
                        runOnUiThread { updateUIWithDetectedItems() }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateUIWithDetectedItems() {
        val container = binding.detectedItemsContainer
        container.removeAllViews()

        synchronized(detectedItems) {
            for (item in detectedItems) {
                val itemLayout = FrameLayout(this)
                val itemLayoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                itemLayout.layoutParams = itemLayoutParams

                val imageView = ImageView(this)
                val drawableId = getDrawableIdByName(item.label)
                if (drawableId != null) {
                    imageView.setImageResource(drawableId)
                }
                val imageViewParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
                )
                imageView.layoutParams = imageViewParams

                val textView = TextView(this).apply {
                    text = item.label
                    val textViewParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(16, 16, 16, 16)
                    }
                    layoutParams = textViewParams
                }

                itemLayout.addView(imageView)
                itemLayout.addView(textView)

                container.addView(itemLayout)
            }
        }
    }

    private fun getDrawableIdByName(name: String): Int? {
        val formattedName = formatClassNameToResourceName(name)
        return resources.getIdentifier(formattedName, "drawable", packageName).takeIf { it != 0 }
    }

    private fun formatClassNameToResourceName(className: String): String {
        return className.lowercase().replace(" ", "_")
    }
}

class ImageAnalyzer(val listener: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        bitmap?.let {
            listener(it)
        }
        imageProxy.close()
    }
}

fun ImageProxy.toBitmap(): Bitmap? {
    val image = this.image ?: return null
    val planes = image.planes
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, this.width, this.height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, this.width, this.height), 100, out)
    val yuv = out.toByteArray()
    return BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
}
