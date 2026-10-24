package it.uncledan.pdmeasure.camera

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import kotlin.math.max

object CameraIntrinsics {
    /**
     * Rapporto focale/lato lungo del sensore. Moltiplicato per il lato lungo dell'immagine (px)
     * fornisce la focale in pixel (assumendo un'immagine 4:3 a campo pieno).
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun focalRatio(camera: Camera): Double? = try {
        val info = Camera2CameraInfo.from(camera.cameraInfo)
        val focal = info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull()
        val sensor = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        if (focal != null && sensor != null && focal > 0f) {
            val longSide = max(sensor.width, sensor.height)
            if (longSide > 0f) focal.toDouble() / longSide else null
        } else null
    } catch (e: Exception) {
        null
    }
}
