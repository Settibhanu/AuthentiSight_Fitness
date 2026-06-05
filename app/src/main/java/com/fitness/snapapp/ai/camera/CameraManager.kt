package com.fitness.snapapp.ai.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Thread 1 — Camera Capture
 *
 * Delivers a continuous stream of RGBA frames to [onFrameAvailable].
 * Frames are NEVER stored or written to disk.
 * The consumer MUST call [ImageProxy.close] after processing.
 */
class CameraManager(
    private val context: Context,
    private val onFrameAvailable: (ImageProxy) -> Unit
) {
    private val cameraExecutor: ExecutorService =
        Executors.newSingleThreadExecutor { r -> Thread(r, "camera-capture") }

    private var imageAnalysis: ImageAnalysis? = null

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        // Hand off to inference pipeline; consumer closes the proxy
                        onFrameAvailable(imageProxy)
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        }, context.mainExecutor)
    }

    fun stopCamera() {
        imageAnalysis?.clearAnalyzer()
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
