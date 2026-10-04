package app.orbz.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The few pieces every screen is built from, in the console's look.

@Composable
fun Sheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Ink.bg2),
        border = BorderStroke(1.dp, Ink.line),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("● ", color = Ink.ember, style = Label)
        Text(text.uppercase(), style = Label)
    }
}

/** One fact row: a dim key on the left, a mono value on the right, orange when it deserves a second look. */
@Composable
fun Fact(k: String, v: String, warn: Boolean = false, ok: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(k.uppercase(), style = Label.copy(color = Ink.dust), modifier = Modifier.padding(end = 12.dp, top = 2.dp))
            Text(v, style = Mono.copy(color = if (warn) Ink.emberGlow else if (ok) Ink.marbleLight else Ink.marble), textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
        }
        HorizontalDivider(color = Ink.line)
    }
}

@Composable
fun Primary(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, tag: String? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink.marbleLight, contentColor = Ink.bg, disabledContainerColor = Ink.marbleLight.copy(alpha = 0.4f), disabledContentColor = Ink.bg),
        modifier = modifier.then(if (tag != null) Modifier.semantics { testTag = tag } else Modifier),
    ) { Text(text, color = if (enabled) Ink.bg else Ink.bg.copy(alpha = 0.45f), fontWeight = FontWeight.Bold) }
}

@Composable
fun Ghost(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, tag: String? = null, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Ink.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink.marble),
        modifier = modifier.then(if (tag != null) Modifier.semantics { testTag = tag } else Modifier),
    ) { Text(text, color = if (enabled) Ink.marble else Ink.dust, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, mono: Boolean = false, single: Boolean = true, tag: String? = null) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = single, maxLines = if (single) 1 else 5,
        placeholder = { Text(placeholder, color = Ink.dust) },
        textStyle = if (mono) Mono.copy(color = Ink.marbleLight) else MaterialTheme.typography.bodyLarge.copy(color = Ink.marbleLight),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Ink.night, unfocusedContainerColor = Ink.night,
            focusedBorderColor = Ink.brass, unfocusedBorderColor = Ink.line, cursorColor = Ink.ember,
        ),
        modifier = modifier.then(if (tag != null) Modifier.semantics { testTag = tag } else Modifier),
    )
}

@Composable
fun ErrorLine(text: String?) {
    if (!text.isNullOrBlank()) Text(text, color = Ink.emberGlow, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
}

/** The line under an answer: tokens, cost, balance. */
@Composable
fun ReceiptLine(text: String) {
    Text(text, style = Mono.copy(color = Ink.dust, fontSize = 11.5.sp), modifier = Modifier.padding(top = 4.dp))
}
