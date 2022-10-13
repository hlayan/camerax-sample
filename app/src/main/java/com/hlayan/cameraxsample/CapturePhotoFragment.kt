package com.hlayan.cameraxsample

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.Surface.ROTATION_0
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.hlayan.cameraxsample.databinding.FragmentCapturePhotoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CapturePhotoFragment : Fragment() {

    private val imageCapture: ImageCapture = ImageCapture.Builder().build()

    private val binding: FragmentCapturePhotoBinding by lazy {
        FragmentCapturePhotoBinding.inflate(layoutInflater)
    }

    private var camera: Camera? = null

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.entries.all { it.value }
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Permissions denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            openCamera()
        } else {
            permissionsLauncher.launch(REQUIRED_PERMISSIONS.toTypedArray())
        }

        binding.btnTakePhoto.setOnClickListener {
            capturePhoto {
                binding.ivPreview.setImageBitmap(it)
                enableSubmitView()
            }
        }

        binding.progressIndicator.hide()

        binding.btnRetake.setOnClickListener {
            enableSubmitView(false)
            binding.progressIndicator.hide()
        }

        binding.btnSubmit.setOnClickListener {
            binding.btnSubmit.text = ""
            binding.progressIndicator.show()
        }

        binding.btnFlash.setOnClickListener {
            decideTorchState()
        }
    }

    private fun enableSubmitView(enable: Boolean = true) {
        binding.previewView.isInvisible = enable
        binding.ivPreview.isInvisible = !enable
        binding.groupTakePhoto.isInvisible = enable
        binding.groupSubmit.isInvisible = !enable
    }

    private fun decideTorchState() {
        camera?.apply {
            if (!cameraInfo.hasFlashUnit()) return
            when (cameraInfo.torchState.value) {
                TorchState.OFF -> cameraControl.enableTorch(true)
                TorchState.ON -> cameraControl.enableTorch(false)
            }
        }
    }

    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val aspectRatio = Rational(295, 193)
            val viewPort = ViewPort.Builder(aspectRatio, ROTATION_0).build()

            val useCaseGroup = UseCaseGroup.Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build()

            try {
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup
                )

            } catch (exc: Exception) {
                Toast.makeText(context, exc.message, Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhotoWithContentValue() {

        val photoName =
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, photoName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireActivity().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(requireContext(), output.savedUri.toString(), Toast.LENGTH_SHORT)
                        .show()

                    binding.ivPreview.setImageURI(output.savedUri)
                    enableSubmitView()
                }
            }
        )
    }

    private fun takePhotoWithFile() {

        val photoFile = File(requireContext().filesDir, "Nrc.jpg")

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .setMetadata(ImageCapture.Metadata())
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), exc.message, Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(requireContext(), output.savedUri.toString(), Toast.LENGTH_SHORT)
                        .show()

                    binding.ivPreview.setImageURI(output.savedUri)
                    enableSubmitView()
                }
            }
        )
    }

    private fun capturePhoto(onCaptured: (Bitmap?) -> Unit) {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    super.onCaptureSuccess(imageProxy)

                    imageProxy.use {
                        val aspectRatio = Rational(295, 193)
                        onCaptured(
                            it.toBitmap().cropCenter(aspectRatio)
                        )
                    }
                }
            }
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            listOf(
                Manifest.permission.CAMERA,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    }
}