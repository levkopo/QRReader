package ru.levkopo.qrreader

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeImageAnalyzer : ImageAnalysis.Analyzer {
    lateinit var onBarcode: (Barcode) -> Unit
    var lastValue: String? = null

    override fun analyze(imageProxy: ImageProxy) {
        scanBarcode(imageProxy)
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun scanBarcode(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnCompleteListener {
                    imageProxy.close()
                    if (it.isSuccessful) {
                        readBarcodeData(it.result as List<Barcode>)
                    }
                }
        }
    }

    private fun readBarcodeData(barcodes: List<Barcode>) {
        if(barcodes.isNotEmpty()) {
            val barcode = barcodes.last()
            if(lastValue!=barcode.rawValue) {
                onBarcode(barcode)
                lastValue = barcode.rawValue
            }
        }
    }
}