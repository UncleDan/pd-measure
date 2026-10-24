package it.uncledan.pdmeasure.ui

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.uncledan.pdmeasure.camera.CameraIntrinsics
import it.uncledan.pdmeasure.camera.FaceAnalyzer
import it.uncledan.pdmeasure.camera.FrameSource
import java.util.concurrent.Executors

/** Collega la fotocamera frontale all'analizzatore finché il composable è visibile. */
@Composable
fun CameraBinding(source: FrameSource) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = FaceAnalyzer(context.applicationContext, source)
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 960),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        analysis.setAnalyzer(executor, analyzer)

        var provider: ProcessCameraProvider? = null
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get()
                provider = p
                p.unbindAll()
                val camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
                source.focalRatio = CameraIntrinsics.focalRatio(camera)
            } catch (e: Exception) {
                source.error.value = e.message ?: e.javaClass.simpleName
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            analysis.clearAnalyzer()
            provider?.unbind(analysis)
            // Chiusura sullo stesso thread dell'analisi: niente corse con un fotogramma in corso
            executor.execute { analyzer.close() }
            executor.shutdown()
        }
    }
}
