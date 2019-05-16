package com.interspacehq.quay.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.interspacehq.quay.KeyboardSizeProvider
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private val subs = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onPostResume() {
        super.onPostResume()

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
        }
    }

    override fun onStop() {
        super.onStop()
        subs.clear()
    }
}
