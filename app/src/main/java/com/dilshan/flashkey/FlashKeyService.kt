package com.dilshan.flashkey

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
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

    // color constants — change these anytime to update the whole keyboard feel
    private val colorLetterKey = 0xFF606060.toInt()    // normal letter key color
    private val colorActionKey = 0xFF383838.toInt()    // action key color (SHF, DEL, etc)
    private val colorNumberKey = 0xFF484848.toInt()    // number key color
    private val colorFlashStart = 0xFFFFAA44.toInt()   // flash start color (warm orange)
    private val colorShiftActive = 0xFF4CAF50.toInt()  // shift key color when uppercase on

    // flash animation duration in milliseconds — change this anytime
    private val flashDuration = 600L

    // corner radius in dp — separate values for letter/action keys and number keys
    // change these anytime to adjust roundness
    private val cornerRadiusDp = 8f        // letter and action keys
    private val cornerRadiusNumberDp = 4f  // number row keys — matches key_background_number.xml

    // Android calls this automatically when the keyboard needs to appear on screen.
    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)

        keyboardView!!.post {
            val screenWidth = keyboardView!!.width

            // 6dp total margin per key (3dp on each side)
            val marginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6f,
                resources.displayMetrics
            ).toInt()

            // letter key width decided by row 1 — 10 keys sharing full width
            val letterKeyWidth = (screenWidth - marginPx * 10) / 10

            // letter key height = width * 1.3
            // change 1.3f here anytime to adjust key height
            val letterKeyHeight = (letterKeyWidth * 1.3f).toInt()

            // number key width decided by row 0 — 11 keys sharing full width
            val numberKeyWidth = (screenWidth - marginPx * 11) / 11

            // number key height = width (square)
            val numberKeyHeight = numberKeyWidth

            // set height only — width is handled by layout_weight in XML
            val letterKeyIds = listOf(
                R.id.keyQ, R.id.keyW, R.id.keyE, R.id.keyR, R.id.keyT,
                R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
                R.id.keyA, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
                R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL, R.id.keyZ,
                R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB, R.id.keyN,
                R.id.keyM, R.id.keyComma, R.id.keyPeriod,
                R.id.keyShift, R.id.keyDel, R.id.keySym,
                R.id.keySpace, R.id.keyEnter
            )
            for (id in letterKeyIds) {
                keyboardView!!.findViewById<TextView>(id)
                    .layoutParams.height = letterKeyHeight
            }

            // number keys — height only, square
            val numberKeyIds = listOf(
                R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5,
                R.id.key6, R.id.key7, R.id.key8, R.id.key9, R.id.key0,
                R.id.keyAt
            )
            for (id in numberKeyIds) {
                keyboardView!!.findViewById<TextView>(id)
                    .layoutParams.height = numberKeyHeight
            }
        }

        setupKeys(keyboardView!!)
        return keyboardView!!
    }

    // called every time keyboard appears in any app — refreshes enter key
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView?.let { updateEnterKey(it) }
    }

    private fun setupKeys(keyboardView: View) {

        // list of all letter keys — each pair is (view id, lowercase letter)
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

        // loop through every letter key, attach a touch listener
        // on press: flash the key and type the letter
        // if shift is on, uppercase() converts "a" to "A" before typing
        for ((id, label) in letterKeys) {
            val keyView = keyboardView.findViewById<TextView>(id)
            keyView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        flashKey(view, colorLetterKey)
                        val toType = if (isUpperCase) label.uppercase() else label
                        currentInputConnection?.commitText(toType, 1)
                    }
                }
                true
            }
        }

        // number keys — never affected by shift
        val numberKeys = listOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.key0 to "0"
        )

        for ((id, label) in numberKeys) {
            val keyView = keyboardView.findViewById<TextView>(id)
            keyView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // pass cornerRadiusNumberDp so number keys fade back with correct radius
                        flashKey(view, colorNumberKey, cornerRadiusNumberDp)
                        currentInputConnection?.commitText(label, 1)
                    }
                }
                true
            }
        }

        // @ key — same style as number keys
        keyboardView.findViewById<TextView>(R.id.keyAt).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorNumberKey, cornerRadiusNumberDp)
                    currentInputConnection?.commitText("@", 1)
                }
            }
            true
        }

        // , key
        keyboardView.findViewById<TextView>(R.id.keyComma).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorLetterKey)
                    currentInputConnection?.commitText(",", 1)
                }
            }
            true
        }

        // . key
        keyboardView.findViewById<TextView>(R.id.keyPeriod).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorLetterKey)
                    currentInputConnection?.commitText(".", 1)
                }
            }
            true
        }

        // SHIFT key — toggles between lowercase and uppercase
        val shiftKey = keyboardView.findViewById<TextView>(R.id.keyShift)
        shiftKey.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isUpperCase = !isUpperCase
                    flashKey(view, if (isUpperCase) colorShiftActive else colorActionKey)
                    updateKeyLabels(keyboardView)
                }
            }
            true
        }

        // DEL key — tap to delete one, hold to keep deleting
        keyboardView.findViewById<TextView>(R.id.keyDel).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorActionKey)
                    // delete one character immediately on first press
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    // after 400ms start repeating delete every 80ms
                    deleteRunnable = object : Runnable {
                        override fun run() {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            deleteHandler.postDelayed(this, 80)
                        }
                    }
                    deleteHandler.postDelayed(deleteRunnable!!, 400)
                }
                // stop deleting when finger lifts
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    deleteRunnable?.let { deleteHandler.removeCallbacks(it) }
                    deleteRunnable = null
                }
            }
            true
        }

        // SYM key — placeholder for future symbols page
        keyboardView.findViewById<TextView>(R.id.keySym).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorActionKey)
                }
            }
            true
        }

        // SPACE key
        keyboardView.findViewById<TextView>(R.id.keySpace).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorActionKey)
                    currentInputConnection?.commitText(" ", 1)
                }
            }
            true
        }

        // enter key is handled by updateEnterKey() which refreshes on every app switch
        updateEnterKey(keyboardView)
    }

    // updates enter key label and action based on what the current app needs
    // called every time keyboard shows in a new app via onStartInputView
    private fun updateEnterKey(keyboardView: View) {
        val enterKey = keyboardView.findViewById<TextView>(R.id.keyEnter)
        val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
        val imeAction = imeOptions.and(EditorInfo.IME_MASK_ACTION)
        val noEnterAction = imeOptions.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

        // set label based on what the app needs
        val enterLabel = when {
            noEnterAction -> "ENT"
            imeAction == EditorInfo.IME_ACTION_SEARCH -> "🔍"
            imeAction == EditorInfo.IME_ACTION_SEND -> "SEND"
            imeAction == EditorInfo.IME_ACTION_DONE -> "DONE"
            imeAction == EditorInfo.IME_ACTION_NEXT -> "NEXT"
            imeAction == EditorInfo.IME_ACTION_GO -> "GO"
            else -> "ENT"
        }
        enterKey.text = enterLabel

        // update touch listener with fresh IME action for current app
        enterKey.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorActionKey)
                    if (noEnterAction ||
                        imeAction == EditorInfo.IME_ACTION_NONE ||
                        imeAction == EditorInfo.IME_ACTION_UNSPECIFIED) {
                        // type a real newline
                        currentInputConnection?.commitText("\n", 1)
                    } else {
                        currentInputConnection?.performEditorAction(imeAction)
                    }
                }
            }
            true
        }
    }

    // updates every letter key label to match current shift state
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
            keyView.text = if (isUpperCase) label.uppercase() else label
        }

        // keep shift key color correct after labels update
        val shiftKey = keyboardView.findViewById<TextView>(R.id.keyShift)
        if (isUpperCase) {
            val cornerRadiusPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp,
                resources.displayMetrics
            )
            val drawable = GradientDrawable()
            drawable.cornerRadius = cornerRadiusPx
            drawable.setColor(colorShiftActive)
            shiftKey.background = drawable
        }
    }

    // flashes a key with warm orange then fades back to its normal color
    // cornerDp defaults to cornerRadiusDp — pass cornerRadiusNumberDp for number keys
    private fun flashKey(view: View, endColor: Int, cornerDp: Float = cornerRadiusDp) {
        // convert dp to pixels — matches XML drawable radius exactly
        val cornerRadiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, cornerDp,
            resources.displayMetrics
        )

        val drawable = GradientDrawable()
        drawable.cornerRadius = cornerRadiusPx
        drawable.setColor(colorFlashStart)
        view.background = drawable

        // flashDuration defined at top — change anytime
        val animator = ValueAnimator.ofArgb(colorFlashStart, endColor)
        animator.duration = flashDuration
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            drawable.setColor(anim.animatedValue as Int)
        }
        animator.start()
    }
}