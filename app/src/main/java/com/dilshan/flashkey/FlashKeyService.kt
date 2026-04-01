package com.dilshan.flashkey

import android.animation.ValueAnimator
import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView

// InputMethodService is Android's built-in keyboard base class. By extending it, Android knows this class IS a keyboard.
class FlashKeyService : InputMethodService() {

    // Android calls this (override fun onCreateInputView(): View) automatically when the keyboard needs to appear on screen. It loads your keyboard_view.xml layout and inflates it.
    override fun onCreateInputView(): View {
        val keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)
        setupKeys(keyboardView)
        return keyboardView
    }

    private fun setupKeys(keyboardView: View) {
        val keys = listOf(
            // R.id.keyQ to "Q" means "keyQ view sends the letter Q"
            R.id.keyQ to "Q", R.id.keyW to "W", R.id.keyE to "E",
            R.id.keyR to "R", R.id.keyT to "T", R.id.keyY to "Y",
            R.id.keyU to "U", R.id.keyI to "I", R.id.keyO to "O",
            R.id.keyP to "P", R.id.keyA to "A", R.id.keyS to "S",
            R.id.keyD to "D", R.id.keyF to "F", R.id.keyG to "G",
            R.id.keyH to "H", R.id.keyJ to "J", R.id.keyK to "K",
            R.id.keyL to "L", R.id.keyZ to "Z", R.id.keyX to "X",
            R.id.keyC to "C", R.id.keyV to "V", R.id.keyB to "B",
            R.id.keyN to "N", R.id.keyM to "M"
        )

        // Loops through every key, finds the view by its id, and sets a touch listener on it. When finger touches down it does two things - calls flashKey to animate the color, and calls commitText to actually type the letter into whatever app is open.
        // currentInputConnection is the connection to the app the user is typing in - it's provided automatically by InputMethodService.
        for ((id, label) in keys) {
            val keyView = keyboardView.findViewById<TextView>(id)
            keyView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        flashKey(view)
                        currentInputConnection?.commitText(label, 1)
                    }
                }
                true
            }
        }

        // DEL key
        keyboardView.findViewById<TextView>(R.id.keyDel).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view)
                    // DEL uses deleteSurroundingText(1, 0) - delete 1 character before the cursor.
                    currentInputConnection?.deleteSurroundingText(1, 0)
                }
            }
            true
        }

        // SPACE key
        keyboardView.findViewById<TextView>(R.id.keySpace).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view)
                    // SPACE uses commitText(" ", 1) — commits a space character.
                    currentInputConnection?.commitText(" ", 1)
                }
            }
            true
        }
    }

    private fun flashKey(view: View) {
        view.setBackgroundColor(0xFFFFFFFF.toInt())

        val animator = ValueAnimator.ofArgb(
            0xFFFFFFFF.toInt(),
            0xFF808080.toInt()
        )
        animator.duration = 400
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            view.setBackgroundColor(anim.animatedValue as Int)
        }
        animator.start()
    }
}