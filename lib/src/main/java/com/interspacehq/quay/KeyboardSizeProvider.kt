package com.interspacehq.quay

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
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
    private val activity: Activity
) : PopupWindow(activity) {

    val heights: Observable<Int> = Observable.defer<Int> {

        showAtLocation(activity.findViewById(android.R.id.content), Gravity.NO_GRAVITY, 0, 0)

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
        inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED

        width = 0
        height = WindowManager.LayoutParams.MATCH_PARENT

        contentView.viewTreeObserver.addOnGlobalLayoutListener(this::onGlobalLayout)
    }

    private fun onGlobalLayout() {
        activity.windowManager.defaultDisplay.getSize(screenSize)
        contentView.getWindowVisibleDisplayFrame(popupRect)

        val keyboardHeight = screenSize.y - popupRect.bottom
        heightEvents.onNext(keyboardHeight)
    }

    companion object {
        @Suppress("unused")
        fun create(activity: Activity) = KeyboardSizeProvider(activity).heights
    }
}
