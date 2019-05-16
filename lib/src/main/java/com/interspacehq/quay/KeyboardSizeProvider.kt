package com.interspacehq.quay

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * @author dhleong
 */
class KeyboardSizeProvider private constructor(
    private val activity: Activity,
    debug: Boolean
) : PopupWindow(activity) {

    val heights: Observable<Int> = Observable.defer<Int> {

        showAtLocation(activity.findViewById(android.R.id.content), Gravity.NO_GRAVITY, 0, 0)

        contentView.fitsSystemWindows = false
        contentView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        Observable.create { emitter ->
            val disposable = heightEvents.distinctUntilChanged()
                .subscribe(emitter::onNext)

            emitter.setCancellable {
                disposable.dispose()
                dismiss()
            }
        }
    }.share()

    private val screenSize = Point()
    private val popupRect = Rect()

    private val heightEvents = PublishSubject.create<Int>()

    init {
        contentView = View(activity)
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        inputMethodMode = INPUT_METHOD_NEEDED

        width = 0
        height = WindowManager.LayoutParams.MATCH_PARENT

        if (debug) {
            width = 100
            contentView.setBackgroundColor(0xFFff0000.toInt())
        }

        contentView.viewTreeObserver.addOnGlobalLayoutListener(this::onGlobalLayout)
    }

    private fun onGlobalLayout() {
        activity.windowManager.defaultDisplay.getSize(screenSize)
        contentView.getWindowVisibleDisplayFrame(popupRect)

        // NOTE: the topCutout, if there is any, seems to offset the screen "top" as
        // used in the computation of getSize() above, so we need to take that into
        // account when computing the *actual* screen height
        val effectiveScreenHeight = screenSize.y + topCutoutHeight

        val keyboardHeight = effectiveScreenHeight - popupRect.bottom
        heightEvents.onNext(keyboardHeight)
    }

    private val topCutoutHeight by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // no cutouts
            return@lazy 0
        }

        val decor = activity.window.peekDecorView() ?: return@lazy 0
        val cutout = decor.rootWindowInsets.displayCutout ?: return@lazy 0

        cutout.boundingRects.maxBy {
            if (it.top == 0) it.height()
            else 0
        }?.height() ?: 0
    }

    companion object {
        @Suppress("unused")
        fun create(activity: Activity, debug: Boolean = false) = KeyboardSizeProvider(activity, debug).heights
    }
}
