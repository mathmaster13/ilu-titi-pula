package io.github.mathmaster13.ilutitipula

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton

// TODO is it safe to store currentInputConnection/editorinfo? is it any better to do so?

class IluTitiPula : InputMethodService() {
    private var actionId: Int = EditorInfo.IME_ACTION_UNSPECIFIED // default is press enter
    private lateinit var enterKey: ImageButton // effectively val

    // TODO name mode
    var nameMode = false

    // TODO I do not know the behavior the application should have for onStartInput(restarting = true),
    // since I cannot think of a scenario where this happens.

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard, null)

        // Standard keys
        for (key in GlyphKey.entries) {
            view.findViewById<Button>(key.id).run {
                val name = key.name.lowercase()
                setOnClickListener {
                    if (prevChar()?.shouldHaveSpaceAfter() == true)
                        currentInputConnection.commitText(" ", 1)
                    currentInputConnection.commitText(name, 1)
                }
                transformationMethod = null // super ultra do not change my text!
                text = name
            }
        }

        // TODO maybe remove colon opentype ligature
        for (key in PunctKey.entries) {
            view.findViewById<Button>(key.id).setOnClickListener {
                currentInputConnection.commitText((it as Button).text, 1)
            }
        }

        // Special keys
        enterKey = view.findViewById(R.id.ret)
        enterKey.setOnClickListener {
            // credit to toki pona keyboard's code for helping me figure this out
            // FIXME maybe this is wrong - there aren't good online resources
            when (val actionId = this.actionId) { // I promise it won't change!
                EditorInfo.IME_ACTION_UNSPECIFIED -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                EditorInfo.IME_ACTION_NONE -> currentInputConnection.commitText("\n", 1)
                else -> currentInputConnection.performEditorAction(actionId) // bad custom IDs are your skill issue
            }
        }

        view.findViewById<ImageButton>(R.id.backspace).setOnClickListener {
            if (currentInputConnection.getSelectedText(0).isNullOrEmpty()) {
                // TODO should this be a standard or word-based delete?
                val length = prevChar()?.length ?: return@setOnClickListener
                currentInputConnection.deleteSurroundingText(length, 0)
                // TODO long press
            } else currentInputConnection.commitText("", 1)
        }
        return view
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        // steal the enter key's functionality:

        // If the editor says don't customize the enter key, I will listen.
        if (editorInfo == null
            || editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        ) {
            this.actionId = EditorInfo.IME_ACTION_UNSPECIFIED
            enterKey.setImageResource(R.drawable.ret)
            return
        }
        // If the editor has a special action, use that.
        // Otherwise, do what we're told. What could go wrong?
        // If you decide to make your actionId zero, that is your problem.
        val actionId = if (editorInfo.actionLabel != null) editorInfo.actionId
        else editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION

        this.actionId = actionId

        enterKey.setImageResource(
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> R.drawable.send
                EditorInfo.IME_ACTION_SEARCH -> R.drawable.search
                EditorInfo.IME_ACTION_DONE -> R.drawable.done
                EditorInfo.IME_ACTION_NEXT -> R.drawable.next
                EditorInfo.IME_ACTION_PREVIOUS -> R.drawable.prev
                EditorInfo.IME_ACTION_GO -> R.drawable.go
                else -> R.drawable.ret // sadly custom action labels on the main keyboard aren't supported.
            }
        )
    }

    // Use the way Studio recommends if it's supported, otherwise do not.
    // Ironically, Studio's recommendation does not work on my Android R Waydroid device.
    private fun prevChar(): SurrogateCharacter? =
        currentInputConnection.getTextBeforeCursor(2, 0)?.let {
            when (it.length) {
                0 -> null
                1 -> SurrogateCharacter(Character.codePointAt(it, 0), 1)
                else -> {
                    val offset = if (it.hasSurrogatePairAt(it.lastIndex - 1)) 1 else 0
                    SurrogateCharacter(Character.codePointAt(it, it.lastIndex - offset), offset + 1)
                }
            }
        }
}

private data class SurrogateCharacter(val codePoint: Int, val length: Int) {
    fun isLetter() = Character.isLetter(codePoint)
    fun shouldHaveSpaceAfter() = isLetter() || when (codePoint) {
        ','.code, '.'.code, ':'.code, '?'.code, '!'.code -> true
        else -> false
    }
}

private enum class GlyphKey(val id: Int) {
    A(R.id.a), ALA(R.id.ala), I(R.id.i), IKU(R.id.iku), ILU(R.id.ilu), KA(R.id.ka), KATI(R.id.kati),
    KI(R.id.ki), KIKU(R.id.kiku), KU(R.id.ku), LA(R.id.la), LAPI(R.id.lapi), LI(R.id.li),
    LIKA(R.id.lika), LILI(R.id.lili), LU(R.id.lu), LUPA(R.id.lupa), MI(R.id.mi), MUKU(R.id.muku),
    MUTI(R.id.muti), PAKA(R.id.paka), PALI(R.id.pali), PUKA(R.id.puka), PULA(R.id.pula), TAKA(R.id.taka),
    TAMA(R.id.tama), TIKI(R.id.tiki), TIKU(R.id.tiku), TILA(R.id.tila), TILU(R.id.tilu), TIMI(R.id.timi),
    TIPI(R.id.tipi), TITI(R.id.titi), TU(R.id.tu), TUKI(R.id.tuki), TULA(R.id.tula), TULU(R.id.tulu),
    ULI(R.id.uli), UPI(R.id.upi);
}

private enum class PunctKey(val id: Int) {
    SPACE(R.id.space), QUESTION(R.id.question_mark), EXCL(R.id.excl_mark), COMMA(R.id.comma),
    COLON(R.id.colon), PERIOD(R.id.period)
}


