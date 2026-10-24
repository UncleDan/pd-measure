package it.uncledan.pdmeasure.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import it.uncledan.pdmeasure.measure.FaceGeometry
import it.uncledan.pdmeasure.measure.Pt
import java.io.Closeable
import kotlin.math.max

/**
 * Analizzatore CameraX: ruota il fotogramma in verticale, esegue MediaPipe Face Landmarker
 * (478 punti con iridi) e pubblica il risultato su [FrameSource].
 * Tutto avviene sul dispositivo: nessuna immagine viene salvata o inviata.
 */
class FaceAnalyzer(
    private val context: Context,
    private val source: FrameSource,
) : ImageAnalysis.Analyzer, Closeable {

    private var landmarker: FaceLandmarker? = null
    private var failed = false
    private var lastTs = 0L

    private fun createLandmarker(): FaceLandmarker {
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
            .setRunningMode(RunningMode.VIDEO)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        return FaceLandmarker.createFromOptions(context, options)
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (failed) return
            val lm = landmarker ?: createLandmarker().also { landmarker = it }
            val bmp = image.toUprightBitmap()

            var ts = SystemClock.uptimeMillis()
            if (ts <= lastTs) ts = lastTs + 1
            lastTs = ts

            val result = lm.detectForVideo(BitmapImageBuilder(bmp).build(), ts)
            val w = bmp.width
            val h = bmp.height
            val geometry = result.faceLandmarks().firstOrNull()?.let { face ->
                FaceGeometry.fromLandmarks(face.map { Pt(it.x() * w, it.y() * h) }, w, h)
            }
            val focalPx = source.focalRatio?.let { it * max(w, h) }
            source.frames.value = LiveFrame(bmp, geometry, focalPx)
        } catch (t: Throwable) {
            Log.e(TAG, "Errore di analisi", t)
            if (landmarker == null) failed = true
            source.error.value = t.message ?: t.javaClass.simpleName
        } finally {
            image.close()
        }
    }

    override fun close() {
        landmarker?.close()
        landmarker = null
    }

    private fun ImageProxy.toUprightBitmap(): Bitmap {
        val raw = toBitmap()
        val rotation = imageInfo.rotationDegrees
        if (rotation == 0) return raw
        val m = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
    }

    companion object {
        private const val TAG = "FaceAnalyzer"
        const val MODEL_ASSET = "face_landmarker.task"
    }
}
