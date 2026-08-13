package com.codetivelab.fieldcalc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.codetivelab.fieldcalc.R

/** Semantic key actions emitted by the keypad. */
sealed interface Key {
    data class Digit(val value: Int) : Key
    data object Dot : Key
    data object Sign : Key
    data object Up : Key
    data object Down : Key
    data object Left : Key
    data object Right : Key
    data object Clear : Key
    data object Enter : Key
    data object Back : Key
    data object Menu : Key
    data object Solve : Key
}

/** Stable test tags for the keys (labels are localized, so tests address these instead). */
object KeypadTags {
    const val CLEAR = "key_clear"
    const val ENTER = "key_enter"
    const val BACK = "key_back"
    const val MENU = "key_menu"
    const val SOLVE = "key_solve"
    const val UP = "key_up"
    const val DOWN = "key_down"
    const val LEFT = "key_left"
    const val RIGHT = "key_right"
    const val SIGN = "key_sign"
    const val DOT = "key_dot"
    fun digit(value: Int) = "key_digit_$value"
}

/**
 * Physical-calculator-style keypad from the brief:
 *   7 8 9 UP / 4 5 6 DOWN / 1 2 3 LEFT / +/- 0 . RIGHT
 * plus CLEAR ENTER BACK MENU and one large SOLVE.
 */
@Composable
fun Keypad(onKey: (Key) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(stringResource(R.string.key_clear), Modifier.weight(1f), tag = KeypadTags.CLEAR) { onKey(Key.Clear) }
            RuggedButton(stringResource(R.string.key_enter), Modifier.weight(1f), tag = KeypadTags.ENTER) { onKey(Key.Enter) }
            RuggedButton(stringResource(R.string.key_back), Modifier.weight(1f), tag = KeypadTags.BACK) { onKey(Key.Back) }
            RuggedButton(stringResource(R.string.key_menu), Modifier.weight(1f), tag = KeypadTags.MENU) { onKey(Key.Menu) }
        }

        KeyRow(
            onKey,
            listOf(
                KeySpec(Key.Digit(7), "7", KeypadTags.digit(7)),
                KeySpec(Key.Digit(8), "8", KeypadTags.digit(8)),
                KeySpec(Key.Digit(9), "9", KeypadTags.digit(9)),
                KeySpec(Key.Up, stringResource(R.string.key_up), KeypadTags.UP)
            )
        )
        KeyRow(
            onKey,
            listOf(
                KeySpec(Key.Digit(4), "4", KeypadTags.digit(4)),
                KeySpec(Key.Digit(5), "5", KeypadTags.digit(5)),
                KeySpec(Key.Digit(6), "6", KeypadTags.digit(6)),
                KeySpec(Key.Down, stringResource(R.string.key_down), KeypadTags.DOWN)
            )
        )
        KeyRow(
            onKey,
            listOf(
                KeySpec(Key.Digit(1), "1", KeypadTags.digit(1)),
                KeySpec(Key.Digit(2), "2", KeypadTags.digit(2)),
                KeySpec(Key.Digit(3), "3", KeypadTags.digit(3)),
                KeySpec(Key.Left, stringResource(R.string.key_left), KeypadTags.LEFT)
            )
        )
        KeyRow(
            onKey,
            listOf(
                KeySpec(Key.Sign, stringResource(R.string.key_sign), KeypadTags.SIGN),
                KeySpec(Key.Digit(0), "0", KeypadTags.digit(0)),
                KeySpec(Key.Dot, stringResource(R.string.key_dot), KeypadTags.DOT),
                KeySpec(Key.Right, stringResource(R.string.key_right), KeypadTags.RIGHT)
            )
        )

        RuggedButton(
            label = stringResource(R.string.key_solve),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            accent = true,
            tag = KeypadTags.SOLVE
        ) { onKey(Key.Solve) }
    }
}

private data class KeySpec(val key: Key, val label: String, val tag: String)

@Composable
private fun KeyRow(onKey: (Key) -> Unit, keys: List<KeySpec>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { spec ->
            RuggedButton(spec.label, Modifier.weight(1f), tag = spec.tag) { onKey(spec.key) }
        }
    }
}
