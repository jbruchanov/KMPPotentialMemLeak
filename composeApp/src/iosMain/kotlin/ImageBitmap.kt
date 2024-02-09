import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap() = Image.makeFromEncoded(this).let { image ->
    val imageBitmap = image.toComposeImageBitmap()
    //jvm/native creates a copy inside
    image.close()
    imageBitmap
}