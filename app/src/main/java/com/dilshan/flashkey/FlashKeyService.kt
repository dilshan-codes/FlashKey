package com.dilshan.flashkey

import android.animation.ValueAnimator
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView

// InputMethodService is Android's built-in keyboard base class.
// By extending it, Android knows this class IS a keyboard.
class FlashKeyService : InputMethodService() {

    // tracks whether shift is on or off
    private var isUpperCase = false

    // Handler lets us schedule repeated actions over time — used for hold-to-delete
    private var deleteHandler = Handler(Looper.getMainLooper())

    // the repeating delete action — null when finger is not holding DEL
    private var deleteRunnable: Runnable? = null

    // reference to the keyboard layout so updateKeyLabels can access it
    private var keyboardView: View? = null

    // Android calls this automatically when the keyboard needs to appear on screen.
    // It loads keyboard_view.xml, sets up all key listeners, and returns the view to Android.
    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)
        setupKeys(keyboardView!!)
        return keyboardView!!
    }

    private fun setupKeys(keyboardView: View) {

        // list of all letter keys — each pair is (view id, lowercase letter)
        val keys = listOf(
            R.id.keyQ to "q", R.id.keyW to "w", R.id.keyE to "e",
            R.id.keyR to "r", R.id.keyT to "t", R.id.keyY to "y",
            R.id.keyU to "u", R.id.keyI to "i", R.id.keyO to "o",
            R.id.keyP to "p", R.id.keyA to "a", R.id.keyS to "s",
            R.id.keyD to "d", R.id.keyF to "f", R.id.keyG to "g",
            R.id.keyH to "h", R.id.keyJ to "j", R.id.keyK to "k",
            R.id.keyL to "l", R.id.keyZ to "z", R.id.keyX to "x",
            R.id.keyC to "c", R.id.keyV to "v", R.id.keyB to "b",
            R.id.keyN to "n", R.id.keyM to "m"
        )

        // loop through every letter key, attach a touch listener
        // on press: flash the key color and type the letter
        // if shift is on, uppercase() converts "a" to "A" before typing
        for ((id, label) in keys) {
            val keyView = keyboardView.findViewById<TextView>(id)
            keyView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        flashKey(view)
                        val toType = if (isUpperCase) label.uppercase() else label
                        // commitText sends the character to whatever app is open
                        currentInputConnection?.commitText(toType, 1)
                    }
                }
                true // true means "I handled this event"
            }
        }

        // list of number keys — numbers are never affected by shift
        val numberKeys = listOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.key0 to "0"
        )

        // number keys just type the number directly, no shift logic needed
        for ((id, label) in numberKeys) {
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

        // SHIFT key — toggles between lowercase and uppercase
        val shiftKey = keyboardView.findViewById<TextView>(R.id.keyShift)
        shiftKey.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // flip the shift state
                    isUpperCase = !isUpperCase
                    flashKey(view)
                    // update all key labels to show upper or lowercase
                    updateKeyLabels(keyboardView)
                    // keep shift key green when uppercase is active so user can see the state
                    if (isUpperCase) {
                        view.setBackgroundColor(0xFF4CAF50.toInt())
                    }
                }
            }
            true
        }

        // DEL key — single tap deletes one character, hold to keep deleting
        keyboardView.findViewById<TextView>(R.id.keyDel).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view)
                    // delete one character immediately on first press
                    // deleteSurroundingText(1, 0) means delete 1 character before the cursor
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    // after 400ms holding, start repeating delete every 80ms
                    deleteRunnable = object : Runnable {
                        override fun run() {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            deleteHandler.postDelayed(this, 80)
                        }
                    }
                    deleteHandler.postDelayed(deleteRunnable!!, 400)
                }
                // when finger lifts or is cancelled, stop the repeating delete
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    deleteRunnable?.let { deleteHandler.removeCallbacks(it) }
                    deleteRunnable = null
                }
            }
            true
        }

        // SPACE key — types a single space character
        keyboardView.findViewById<TextView>(R.id.keySpace).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view)
                    currentInputConnection?.commitText(" ", 1)
                }
            }
            true
        }
    }

    // updates every letter key label on screen to match the current shift state
    // called every time shift is toggled
    private fun updateKeyLabels(keyboardView: View) {
        val letterKeys = listOf(
            R.id.keyQ to "q", R.id.keyW to "w", R.id.keyE to "e",
            R.id.keyR to "r", R.id.keyT to "t", R.id.keyY to "y",
            R.id.keyU to "u", R.id.keyI to "i", R.id.keyO to "o",
            R.id.keyP to "p", R.id.keyA to "a", R.id.keyS to "s",
            R.id.keyD to "d", R.id.keyF to "f", R.id.keyG to "g",
            R.id.keyH to "h", R.id.keyJ to "j", R.id.keyK to "k",
            R.id.keyL to "l", R.id.keyZ to "z", R.id.keyX to "x",
            R.id.keyC to "c", R.id.keyV to "v", R.id.keyB to "b",
            R.id.keyN to "n", R.id.keyM to "m"
        )
        for ((id, label) in letterKeys) {
            val keyView = keyboardView.findViewById<TextView>(id)
            // set the visible text on each key to upper or lowercase
            keyView.text = if (isUpperCase) label.uppercase() else label
        }
    }

    // flashes a key white instantly then smoothly fades back to gray
    // called on every key press to give the color animation effect
    private fun flashKey(view: View) {
        // instantly set white on touch
        view.setBackgroundColor(0xFFFFFFFF.toInt())

        // animate from white back to gray over 400ms
        // 0xFFFFFFFF = fully opaque white, 0xFF808080 = fully opaque gray
        val animator = ValueAnimator.ofArgb(
            0xFFFFFFFF.toInt(),
            0xFF808080.toInt()
        )
        animator.duration = 400
        // DecelerateInterpolator makes the fade start fast and slow down — feels natural
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            view.setBackgroundColor(anim.animatedValue as Int)
        }
        animator.start()
    }
}