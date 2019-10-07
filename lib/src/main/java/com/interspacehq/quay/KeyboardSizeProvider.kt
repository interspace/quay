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
    private val realSize = Point()
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
        contentView.getWindowVisibleDisplayFrame(popupRect)

        val effectiveScreenHeight = computeEffectiveScreenHeight()

        val keyboardHeight = effectiveScreenHeight - popupRect.bottom
        heightEvents.onNext(keyboardHeight)
    }

    private fun computeEffectiveScreenHeight(): Int {
        val screenHeight = computeScreenHeight()

        // NOTE: the topCutout, if there is any, seems to offset the screen "top" as
        // used in the computation of getSize() above, so we need to take that into
        // account when computing the *actual* screen height
        return screenHeight + topCutoutHeight
    }

    private fun computeScreenHeight(): Int {
        // NOTE: Display.getSize is not reliable on all devices (I'm looking at you, Xiaomi).
        activity.windowManager.defaultDisplay.getSize(screenSize)
        val screenHeight = screenSize.y

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // hope for the best; no obvious way to handle OEM bugs...
            return screenHeight
        }

        val insets = activity.window.peekDecorView()?.rootWindowInsets
            ?: return screenHeight // hopefully doesn't happen? not much we can do

        // Beware, terrible hacks lie below:

        activity.windowManager.defaultDisplay.getRealSize(realSize)
        val navBarHeight = navBarHeight()

        if (screenHeight + navBarHeight == realSize.y && insets.stableInsetTop > 0) {
            // HACKS for MIUI: MIUI's getSize() method, for whatever reason, *always* returns
            // `realSize - navBarHeight()`, regardless of whether the navBar is actually being
            // shown or not. Since you can disable the navbar and use "fullscreen" gesture nav,
            // this is obviously a problem.

            // NOTE 2: this branch actually can also get triggered on sane devices when
            // using FULLSCREEN mode (to render below the status bar) but luckily it
            // *seems* to work there as well. We *could* always use this branch, but I
            // prefer to keep it conditional since it's less battle-tested. Using
            // stableInset may also be hacks and only work if you have that flag set
            // on the window; experimentally, the Pixel will include the keyboard size
            // in systemWindowInsets in this case. Future work could explore taking
            // adnvatage of this...?
            return realSize.y - insets.stableInsetBottom
        }

        // phew, normal case
        return screenHeight
    }

    private fun navBarHeight(): Int {
        val resources = activity.resources
        val hasNavBarId = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        if (hasNavBarId == 0 || !resources.getBoolean(hasNavBarId)) {
            // no navBar
            return 0
        }

        val heightId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (heightId == 0) return 0

        return resources.getDimensionPixelSize(heightId)
    }

    private val topCutoutHeight: Int
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                // no cutouts
                return 0
            }

            val decor = activity.window.peekDecorView() ?: return 0
            val cutout = decor.rootWindowInsets?.displayCutout ?: return 0

            return cutout.boundingRects.maxBy {
                if (it.top == 0) it.height()
                else 0
            }?.height() ?: 0
        }

    companion object {
        @Suppress("unused")
        fun create(activity: Activity, debug: Boolean = false) = KeyboardSizeProvider(activity, debug).heights
    }
}
