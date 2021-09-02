package ru.levkopo.qrreader

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity: AppCompatActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private val analyzer = BarcodeImageAnalyzer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        setContentView(box {
            view<PreviewView> {
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(this, cameraProvider)
                }, ContextCompat.getMainExecutor(this@MainActivity))
            }

            state<Barcode> { barcode, setBarcode ->
                analyzer.onBarcode = {
                    setBarcode(it)
                }

                view<ScrollView> {
                    box {
                        barcode?.let {
                            when(it.valueType){
                                Barcode.TYPE_URL -> startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(barcode.url?.url)))
                                Barcode.TYPE_TEXT -> view<AppCompatTextView> {
                                    text = barcode.rawValue
                                }
                                Barcode.TYPE_EMAIL -> startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("mailto:"
                                        + it.email?.address
                                        + "?subject=" + it.email?.subject + "&body=" + it.email?.body)))
                                Barcode.TYPE_GEO -> startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("geo:${barcode.geoPoint?.lat}," +
                                            "${barcode.geoPoint?.lng}")))
                            }
                        }
                    }
                }
            }
        })
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindPreview(previewView: PreviewView, cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            preview
        )

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

    override fun onPause() {
        super.onPause()
        analyzer.lastValue = null
    }
}