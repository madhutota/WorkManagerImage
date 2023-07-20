package com.workmanagerimage

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.workmanagerimage.ui.theme.WorkManagerImageTheme
import com.workmanagerimage.workmanager.PhotoCompressionWorker

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager;
    private val viewModel by viewModels<PhotoViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContent {
            WorkManagerImageTheme {
                var workResult = viewModel.workId?.let { id ->
                    workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                }

                LaunchedEffect(key1 = workResult?.outputData) {
                    var filePath = workResult?.outputData?.getString(
                        PhotoCompressionWorker.KEY_RESULT_PATH
                    )

                    filePath?.let {
                        val bitmap = BitmapFactory.decodeFile(it)
                        viewModel.updateCompressedBitmap(bitmap)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    viewModel.unCompressedUri?.let {
                        Text(text = "UnCompressed Photo")
                        AsyncImage(model = it, contentDescription = "Image")


                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    viewModel.compressedBitmap?.let {
                        Text(text = "Compressed Photo")
                        Image(bitmap = it.asImageBitmap(), contentDescription = null)


                    }

                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else intent?.getParcelableExtra(Intent.EXTRA_STREAM)

        viewModel.updateUnCompressUri(uri)
        var request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>().setInputData(
            workDataOf(
                PhotoCompressionWorker.KEY_CONTENT_URI to uri.toString(),
                PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
            )
        ).setConstraints(
            Constraints(
                requiresStorageNotLow = true
            )
        ).build()
        viewModel.updateWorkedId(request.id)
        workManager.enqueue(request)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorkManagerImageTheme {
        Greeting("Android")
    }
}