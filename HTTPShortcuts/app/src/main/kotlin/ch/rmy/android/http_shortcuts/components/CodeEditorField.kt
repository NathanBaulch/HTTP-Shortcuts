package ch.rmy.android.http_shortcuts.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.utils.SyntaxHighlighter

@Composable
fun CodeEditorField(
    language: String,
    value: TextFieldValue,
    placeholder: String,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val useDarkTheme = isSystemInDarkTheme()
    val syntaxHighlighter = remember(language, useDarkTheme) {
        SyntaxHighlighter(language, useDarkTheme)
    }

    val textFieldColors = if (useDarkTheme) {
        val backgroundColor = colorResource(R.color.textarea_background)
        TextFieldDefaults.colors(
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            disabledContainerColor = backgroundColor,
        )
    } else {
        TextFieldDefaults.colors()
    }

    TextField(
        modifier = modifier,
        colors = textFieldColors,
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
        ),
        placeholder = {
            Text(text = placeholder)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
        ),
        visualTransformation = {
            TransformedText(syntaxHighlighter.format(it.text), OffsetMapping.Identity)
        },
    )
}
