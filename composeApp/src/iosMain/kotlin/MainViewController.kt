import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val shareManager = IosShareManager()
    return ComposeUIViewController {
        App(shareManager)
    }.also {
        shareManager.viewController = it
    }
}