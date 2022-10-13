package com.hlayan.cameraxsample

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.hlayan.cameraxsample.databinding.FragmentCaptureVideoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CaptureVideoFragment : Fragment() {

    private val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()

    private val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)
    private var recording: Recording? = null
    private var videoRecordEvent: VideoRecordEvent? = null

    private val binding: FragmentCaptureVideoBinding by lazy {
        FragmentCaptureVideoBinding.inflate(layoutInflater)
    }

    private val permissionsResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.entries.all { it.value }
        if (isGranted) {
            startCamera()
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
            startCamera()
        } else {
            permissionsResult.launch(REQUIRED_PERMISSIONS.toTypedArray())
        }

        binding.btnTakePhoto.setOnClickListener {
            if (videoRecordEvent == null || videoRecordEvent is VideoRecordEvent.Finalize) {
                startRecording()
            } else {
                recording?.stop()
            }
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture
                )

            } catch (exc: Exception) {
                Toast.makeText(context, exc.message, Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * For saving to external storage folder
     */
    private fun getMediaStoreOutputOptions(): MediaStoreOutputOptions {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        return MediaStoreOutputOptions
            .Builder(requireActivity().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }

    /**
     * For saving to app private data folder
     */
    private fun getFileOutputOptions(): FileOutputOptions {
        val videoFile = File(requireContext().filesDir, "face-live.mp4")
        return FileOutputOptions.Builder(videoFile).build()
    }

    private fun startRecording() {
        recording = videoCapture.output
            .prepareRecording(requireContext(), getFileOutputOptions())
            .start(ContextCompat.getMainExecutor(requireContext()), captureListener)
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { recordEvent ->
        videoRecordEvent = recordEvent
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show()
            }
            is VideoRecordEvent.Finalize -> {
                if (recordEvent.hasError().not()) {
                    val message = "Video Recorded at ${recordEvent.outputResults.outputUri}"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                } else {
                    recording?.close()
                    recording = null
                    Toast.makeText(
                        requireContext(),
                        recordEvent.cause?.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.CAMERA,
        )
    }
}