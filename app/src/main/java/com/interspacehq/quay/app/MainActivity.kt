package com.interspacehq.quay.app

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.interspacehq.quay.KeyboardSizeProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private val subs = CompositeDisposable()
    private var fullscreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.apply {
            systemUiVisibility =
                systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        findViewById<View>(R.id.force_keyboard).setOnClickListener {
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.showSoftInput(findViewById(R.id.input), InputMethodManager.SHOW_FORCED)
        }

        findViewById<View>(R.id.switch_window_mode).setOnClickListener {

            window.decorView.apply {
                fullscreen = !fullscreen
                systemUiVisibility = if (fullscreen) {
                    systemUiVisibility or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                } else {
                    systemUiVisibility and (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            ).inv()
                }
            }


        }
    }

    override fun onStart() {
        super.onStart()

        findViewById<View>(android.R.id.content).post {
            subs.add(
                KeyboardSizeProvider.create(this, debug = true)
                    .subscribe { keyboardHeight ->
                        label.text = "${keyboardHeight}px"
                        keyboard_height.updateLayoutParams {
                            height = keyboardHeight
                        }
                    }
            )

            // quick test to check disposing the subscription from a background thread
            val heights = KeyboardSizeProvider.create(this).subscribe()
            subs.add(
                Observable.timer(100, TimeUnit.MILLISECONDS)
                    .subscribe { heights.dispose() }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        subs.clear()
    }
}
