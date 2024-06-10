package com.example.refrigerator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

data class DetectionResult(val label: String, val confidence: Float, val location: RectF, val labelIndex: Int)

class ObjectDetectionHelper(context: Context) {
    private val interpreter: Interpreter
    private val labels: List<String>
    private val imageProcessor: ImageProcessor

    init {
        val model = FileUtil.loadMappedFile(context, "best_float32.tflite")
        interpreter = Interpreter(model)
        labels = FileUtil.loadLabels(context, "label.txt")
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    fun detectObjects(bitmap: Bitmap): List<DetectionResult> {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val inputBuffer = processedImage.buffer

        // 출력 버퍼 준비 - 모델의 실제 출력 형식에 맞게 조정
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 66, 8400), DataType.FLOAT32)

        // 모델 실행
        interpreter.run(inputBuffer, outputBuffer.buffer.rewind())

        // 결과 처리
        val results = mutableListOf<DetectionResult>()
        val outputArray = outputBuffer.floatArray
        val numDetections = 8400
        val numClasses = 66 - 5

        for (i in 0 until numDetections) {
            val confidence = outputArray[i * 66 + 4]
            if (confidence > 0.5) { // 임계값 조정
                val x = outputArray[i * 66]
                val y = outputArray[i * 66 + 1]
                val width = outputArray[i * 66 + 2]
                val height = outputArray[i * 66 + 3]
                val left = x - width / 2
                val top = y - height / 2
                val right = x + width / 2
                val bottom = y + height / 2
                val location = RectF(left, top, right, bottom)
                val classScores = outputArray.sliceArray(i * 66 + 5 until i * 66 + 66)
                val maxScoreIndex = classScores.indices.maxByOrNull { classScores[it] } ?: -1
                val labelIndex = maxScoreIndex
                val label = if (labelIndex != -1 && labelIndex < labels.size) labels[labelIndex] else "Unknown"
                results.add(DetectionResult(label, confidence, location, labelIndex))
            }
        }

        return results
    }
}
