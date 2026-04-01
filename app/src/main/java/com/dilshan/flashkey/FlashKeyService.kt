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

    // four possible keyboard states
    enum class KeyboardState {
        LETTERS_LOWER,  // normal lowercase letters
        LETTERS_UPPER,  // uppercase letters
        SYMBOLS_1,      // first symbol set
        SYMBOLS_2       // second symbol set
    }

    // current state — starts at lowercase
    private var currentState = KeyboardState.LETTERS_LOWER

    // Handler lets us schedule repeated actions over time — used for hold-to-delete
    private var deleteHandler = Handler(Looper.getMainLooper())

    // the repeating delete action — null when finger is not holding DEL
    private var deleteRunnable: Runnable? = null

    // reference to the keyboard layout so state updates can access it
    private var keyboardView: View? = null

    // color constants — change these anytime to update the whole keyboard feel
    private val colorLetterKey = 0xFF606060.toInt()    // normal letter key color
    private val colorActionKey = 0xFF383838.toInt()    // action key color (SHF, DEL, etc)
    private val colorNumberKey = 0xFF484848.toInt()    // number key color
    private val colorFlashStart = 0xFFFFAA44.toInt()   // flash start color (warm orange)
    private val colorShiftActive = 0xFF4CAF50.toInt()  // shift/SHF green when uppercase or sym2
    private val colorSymActive = 0xFF2196F3.toInt()    // SYM blue when showing symbols

    // flash animation duration in milliseconds — change this anytime
    private val flashDuration = 600L

    // corner radius in dp — separate values for letter/action keys and number keys
    private val cornerRadiusDp = 8f        // letter and action keys
    private val cornerRadiusNumberDp = 4f  // number row keys

    // the 26 keys that change between states — row by row
    // row 1: 10 keys, row 2: 9 keys, row 3 middle: 7 keys
    private val changingKeyIds = listOf(
        // row 1
        R.id.keyQ, R.id.keyW, R.id.keyE, R.id.keyR, R.id.keyT,
        R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
        // row 2
        R.id.keyA, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
        R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL,
        // row 3 middle (between SHF and DEL)
        R.id.keyZ, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB,
        R.id.keyN, R.id.keyM
    )

    // letters lowercase
    private val lettersLower = listOf(
        "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
        "a", "s", "d", "f", "g", "h", "j", "k", "l",
        "z", "x", "c", "v", "b", "n", "m"
    )

    // letters uppercase
    private val lettersUpper = lettersLower.map { it.uppercase() }

    // symbols set 1 — most commonly used
    private val symbols1 = listOf(
        "!", "?", "#", "$", "%", "&", "*", "(", ")", "-",
        "\"", "\u201C", "\u2018", "\u2019", "+", "=", "/", "\\", ":",
        ";", "<", ">", "_", "~", "^", "|"
    )

    // symbols set 2 — less common
    private val symbols2 = listOf(
        "[", "]", "€", "£", "¥", "°", "©", "®", "™", "÷",
        "×", "±", "§", "¶", "•", "…", "–", "—", "`",
        "¿", "¡", "«", "»", "{", "}", "↑"
    )

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
            val letterKeyHeight = (letterKeyWidth * 1.2f).toInt()

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

        // number keys — never affected by any state
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
                        flashKey(view, colorNumberKey, cornerRadiusNumberDp)
                        currentInputConnection?.commitText(label, 1)
                    }
                }
                true
            }
        }

        // @ key — fixed, never changes
        keyboardView.findViewById<TextView>(R.id.keyAt).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorNumberKey, cornerRadiusNumberDp)
                    currentInputConnection?.commitText("@", 1)
                }
            }
            true
        }

        // , key — fixed, never changes
        keyboardView.findViewById<TextView>(R.id.keyComma).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorLetterKey)
                    currentInputConnection?.commitText(",", 1)
                }
            }
            true
        }

        // . key — fixed, never changes
        keyboardView.findViewById<TextView>(R.id.keyPeriod).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorLetterKey)
                    currentInputConnection?.commitText(".", 1)
                }
            }
            true
        }

        // DEL key — tap to delete one, hold to keep deleting — never changes
        keyboardView.findViewById<TextView>(R.id.keyDel).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorActionKey)
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    deleteRunnable = object : Runnable {
                        override fun run() {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            deleteHandler.postDelayed(this, 80)
                        }
                    }
                    deleteHandler.postDelayed(deleteRunnable!!, 400)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    deleteRunnable?.let { deleteHandler.removeCallbacks(it) }
                    deleteRunnable = null
                }
            }
            true
        }

        // SPACE key — never changes
        keyboardView.findViewById<TextView>(R.id.keySpace).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorActionKey)
                    currentInputConnection?.commitText(" ", 1)
                }
            }
            true
        }

        // SYM key — toggles between letters and symbols
        keyboardView.findViewById<TextView>(R.id.keySym).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // toggle between letters and symbols
                    currentState = when (currentState) {
                        KeyboardState.LETTERS_LOWER -> KeyboardState.SYMBOLS_1
                        KeyboardState.LETTERS_UPPER -> KeyboardState.SYMBOLS_1
                        KeyboardState.SYMBOLS_1 -> KeyboardState.LETTERS_LOWER
                        KeyboardState.SYMBOLS_2 -> KeyboardState.LETTERS_LOWER
                    }
                    flashKey(view,
                        if (currentState == KeyboardState.SYMBOLS_1 ||
                            currentState == KeyboardState.SYMBOLS_2)
                            colorSymActive else colorActionKey
                    )
                    updateKeyboardState(keyboardView)
                }
            }
            true
        }

        // SHF key — toggles lower/upper OR symbols1/symbols2
        keyboardView.findViewById<TextView>(R.id.keyShift).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentState = when (currentState) {
                        KeyboardState.LETTERS_LOWER -> KeyboardState.LETTERS_UPPER
                        KeyboardState.LETTERS_UPPER -> KeyboardState.LETTERS_LOWER
                        KeyboardState.SYMBOLS_1 -> KeyboardState.SYMBOLS_2
                        KeyboardState.SYMBOLS_2 -> KeyboardState.SYMBOLS_1
                    }
                    flashKey(view,
                        if (currentState == KeyboardState.LETTERS_UPPER ||
                            currentState == KeyboardState.SYMBOLS_2)
                            colorShiftActive else colorActionKey
                    )
                    updateKeyboardState(keyboardView)
                }
            }
            true
        }

        // set up the 26 changing keys with touch listeners
        for ((index, id) in changingKeyIds.withIndex()) {
            val keyView = keyboardView.findViewById<TextView>(id)
            keyView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        flashKey(view, colorLetterKey)
                        // get the label for current state at this index
                        val label = getLabelForCurrentState(index)
                        currentInputConnection?.commitText(label, 1)
                    }
                }
                true
            }
        }

        // enter key refreshes per app
        updateEnterKey(keyboardView)

        // set initial state
        updateKeyboardState(keyboardView)
    }

    // returns the correct label for a key index based on current state
    private fun getLabelForCurrentState(index: Int): String {
        return when (currentState) {
            KeyboardState.LETTERS_LOWER -> lettersLower[index]
            KeyboardState.LETTERS_UPPER -> lettersUpper[index]
            KeyboardState.SYMBOLS_1 -> symbols1[index]
            KeyboardState.SYMBOLS_2 -> symbols2[index]
        }
    }

    // updates all 26 changing keys and action key colors based on current state
    private fun updateKeyboardState(keyboardView: View) {
        // update all 26 changing key labels
        for ((index, id) in changingKeyIds.withIndex()) {
            val keyView = keyboardView.findViewById<TextView>(id)
            keyView.text = getLabelForCurrentState(index)
        }

        // update SHF color
        val shiftKey = keyboardView.findViewById<TextView>(R.id.keyShift)
        val shiftColor = when (currentState) {
            KeyboardState.LETTERS_UPPER -> colorShiftActive  // green
            KeyboardState.SYMBOLS_2 -> colorShiftActive      // green
            else -> colorActionKey                            // gray
        }
        setKeyColor(shiftKey, shiftColor)

        // update SYM color
        val symKey = keyboardView.findViewById<TextView>(R.id.keySym)
        val symColor = when (currentState) {
            KeyboardState.SYMBOLS_1 -> colorSymActive   // blue
            KeyboardState.SYMBOLS_2 -> colorSymActive   // blue
            else -> colorActionKey                       // gray
        }
        setKeyColor(symKey, symColor)
    }

    // sets a key background color while keeping correct corner radius
    private fun setKeyColor(view: View, color: Int) {
        val cornerRadiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp,
            resources.displayMetrics
        )
        val drawable = GradientDrawable()
        drawable.cornerRadius = cornerRadiusPx
        drawable.setColor(color)
        view.background = drawable
    }

    // updates enter key label and action based on what the current app needs
    private fun updateEnterKey(keyboardView: View) {
        val enterKey = keyboardView.findViewById<TextView>(R.id.keyEnter)
        val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
        val imeAction = imeOptions.and(EditorInfo.IME_MASK_ACTION)
        val noEnterAction = imeOptions.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

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

        enterKey.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flashKey(view, colorActionKey)
                    if (noEnterAction ||
                        imeAction == EditorInfo.IME_ACTION_NONE ||
                        imeAction == EditorInfo.IME_ACTION_UNSPECIFIED) {
                        currentInputConnection?.commitText("\n", 1)
                    } else {
                        currentInputConnection?.performEditorAction(imeAction)
                    }
                }
            }
            true
        }
    }

    // flashes a key with warm orange then fades back to its normal color
    // cornerDp defaults to cornerRadiusDp — pass cornerRadiusNumberDp for number keys
    private fun flashKey(view: View, endColor: Int, cornerDp: Float = cornerRadiusDp) {
        val cornerRadiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, cornerDp,
            resources.displayMetrics
        )
        val drawable = GradientDrawable()
        drawable.cornerRadius = cornerRadiusPx
        drawable.setColor(colorFlashStart)
        view.background = drawable

        val animator = ValueAnimator.ofArgb(colorFlashStart, endColor)
        animator.duration = flashDuration
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            drawable.setColor(anim.animatedValue as Int)
        }
        animator.start()
    }
}