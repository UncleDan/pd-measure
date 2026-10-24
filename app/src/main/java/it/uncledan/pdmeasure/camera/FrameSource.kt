package it.uncledan.pdmeasure.camera

import android.graphics.Bitmap
import it.uncledan.pdmeasure.measure.FaceGeometry
import kotlinx.coroutines.flow.MutableStateFlow

/** Fotogramma analizzato: immagine dritta (non specchiata) + geometria del volto. */
class LiveFrame(
    val bitmap: Bitmap,
    val geometry: FaceGeometry?,
    /** Lunghezza focale in pixel per questa immagine, se nota. */
    val focalPx: Double?,
)

/** Ponte tra l'analizzatore (thread di background) e la UI Compose. */
class FrameSource {
    val frames = MutableStateFlow<LiveFrame?>(null)
    val error = MutableStateFlow<String?>(null)

    /** focale(mm) / lato lungo del sensore(mm): moltiplicato per il lato lungo in px dà la focale in px. */
    @Volatile
    var focalRatio: Double? = null
}
