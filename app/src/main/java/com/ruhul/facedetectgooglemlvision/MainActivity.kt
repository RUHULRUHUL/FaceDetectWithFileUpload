package com.ruhul.facedetectgooglemlvision

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.ruhul.facedetectgooglemlvision.databinding.ActivityMainBinding
import dmax.dialog.SpotsDialog
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val logTag: String = "MainActivity-Debug:"
    private lateinit var binding: ActivityMainBinding
    private lateinit var alertDialog: SpotsDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAlertDialog()
        binding.fileUpload.setOnClickListener {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            } else {
                Dexter.withContext(this@MainActivity)
                    .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ).withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                            if (report.areAllPermissionsGranted()) {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                            token: PermissionToken?
                        ) {
                            token?.continuePermissionRequest()
                        }
                    }).check()
            }


        }
    }

    private fun initAlertDialog() {
        alertDialog = (SpotsDialog.Builder().setContext(this@MainActivity)
            .setMessage("Detect Face Wait..")
            .setCancelable(false)
            .build() as SpotsDialog?)!!
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                try {
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                    binding.imageView.setImageBitmap(bitmap)
                    processFaceImg(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }


    private fun processFaceImg(bitmap: Bitmap?) {
        alertDialog.show()
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.2f)
            .build()

        val image = bitmap?.let { InputImage.fromBitmap(it, 0) }
        val detector = FaceDetection.getClient(options)

        image?.let { it ->
            detector.process(it)
                .addOnSuccessListener {
                    if (it.isEmpty()) {
                        Toast.makeText(this, "No Face Detected", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                        Log.d(logTag, "addOnSuccessListener - " + "No Face Detected")
                    } else {
                        alertDialog.dismiss()
                        getFaceResult(it)
                    }
                }
                .addOnFailureListener {
                    alertDialog.dismiss()
                    Toast.makeText(this, "No detect Problem", Toast.LENGTH_SHORT).show()
                    Log.d(logTag, "addOnFailureListener error - " + it.message)
                }
        }
    }

    private fun getFaceResult(it: List<Face>) {
        val face = it[0]
        val rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR)
        val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

        if (rightEar != null && leftEar != null && nose != null && mouthLeft != null && mouthRight != null && mouthBottom != null) {
            Log.d(logTag, "LandMark Check success ")
            val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
            val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points
            val lowerLitBottomContour = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points

            if (leftEyeContour != null && upperLipBottomContour != null && lowerLitBottomContour != null) {
                Log.d(logTag, "contour Check success ")

                val snackBar =
                    Snackbar.make(
                        binding.root,
                        "Face Detect Success: ",
                        Snackbar.LENGTH_LONG
                    )
                snackBar.show()
            }
        }
    }
}