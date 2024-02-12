import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun App(shareManager: ShareManager) {
    MaterialTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            val scope = rememberCoroutineScope()
            val listOfImages = remember { mutableStateListOf<List<ImageBitmap>>() }
            var currentJob by remember { mutableStateOf<Job?>(null) }
            val sumOfMem by derivedStateOf {
                listOfImages.sumOf { it.sumOf { im -> im.sizeKB() } }
            }

            Text("SumMem:${sumOfMem / 1024f}MB")

            Button(onClick = {
                currentJob?.cancel()
                currentJob = scope.launch {
                    val data = shareManager.requestPhotos()
                    withContext(Dispatchers.IO) {
                        val row = mutableStateListOf<ImageBitmap>()
                        listOfImages.add(row)
                        data.forEach {
                            if (!isActive) return@withContext
                            val rawData = data.load(it)
                            val imageBitmap = rawData.toImageBitmap()
                            println("RawData:${(rawData.size / 1024f).roundToInt()}kB, ImageMemSize:${imageBitmap.sizeKB()}kB, Resolution:${imageBitmap.width}x${imageBitmap.height} ")
                            row.add(imageBitmap)
                        }
                    }
                }
            }) {
                Text("Click me!")
            }

            listOfImages.forEach { images ->
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    images.forEach {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            modifier = Modifier
                                .height(200.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun ImageBitmap.sizeKB() = (width * height * (when (config) {
    ImageBitmapConfig.Alpha8 -> 1
    ImageBitmapConfig.Argb8888 -> 4
    ImageBitmapConfig.Rgb565 -> 2
    ImageBitmapConfig.F16 -> 8
    else -> -1
}) / 1024f).roundToInt()
