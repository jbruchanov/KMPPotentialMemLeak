@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.loadDataRepresentationForContentType
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosShareManager : ShareManager {

    var viewController: UIViewController? = null

    override suspend fun requestPhotos(): DeferredDataProvider {
        val viewController = requireNotNull(viewController)
        return suspendCancellableCoroutine { continuation ->
            val photoPicker = PhotoPicker(
                continuation = continuation,
                configuration = PHPickerConfiguration().apply {
                    filter = PHPickerFilter.imagesFilter()
                    setSelectionLimit(Long.MAX_VALUE)
                }
            )
            continuation.invokeOnCancellation {
                runCatching { photoPicker.viewController.dismissViewControllerAnimated(true, null) }
            }
            viewController.presentModalViewController(photoPicker.viewController, true)
        }
    }
}


class PhotoPicker(
    val continuation: CancellableContinuation<DeferredDataProvider>,
    configuration: PHPickerConfiguration
) {

    internal val delegate: PHPickerViewControllerDelegateProtocol = object : NSObject(),
        PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            if (didFinishPicking.isEmpty()) {
                continuation.cancel()
            } else {
                @Suppress("UNCHECKED_CAST")
                val items = didFinishPicking as List<PHPickerResult>
                continuation.resume(dateProviderOf(items.map { it.itemProvider() }, UTTypeImage))
            }
            picker.dismissViewControllerAnimated(true, null)
        }
    }

    val viewController = PHPickerViewController(configuration = configuration).apply {
        setDelegate(this@PhotoPicker.delegate)
    }
}

fun dateProviderOf(providers: List<NSItemProvider>, type: UTType): DeferredDataProvider {
    if (providers.isEmpty()) return DeferredDataProvider.Empty
    suspend fun load(provider: NSItemProvider): ByteArray =
        suspendCancellableCoroutine { loadDataContinuation ->
            provider.loadDataRepresentationForContentType(type) { data, error ->
                if (error != null) {
                    loadDataContinuation.resumeWithException(IllegalStateException(error.toString()))
                } else {
                    loadDataContinuation.resume(data?.toByteArray() ?: byteArrayOf())
                }
            }
        }
    return DeferredDataProvider(providers.map { uri -> { load(uri) } })
}

fun NSData.toByteArray() = ByteArray(this@toByteArray.length.toInt()).apply {
    usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}