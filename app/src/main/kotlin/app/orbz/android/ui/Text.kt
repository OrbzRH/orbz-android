package app.orbz.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Bold, inline code, bullets and short headings from a model's answer. Text only: nothing in it can run. */
private fun inline(line: String): AnnotatedString = buildAnnotatedString {
    val re = Regex("\\*\\*[^*]+\\*\\*|`[^`]+`")
    var at = 0
    for (m in re.findAll(line)) {
        append(line.substring(at, m.range.first))
        val t = m.value
        if (t.startsWith("**")) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Ink.marbleLight)) { append(t.removeSurrounding("**")) }
        else withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Ink.night)) { append(t.removeSurrounding("`")) }
        at = m.range.last + 1
    }
    append(line.substring(at))
}

@Composable
fun Rich(text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        for (raw in text.split("\n")) {
            val line = raw.trimEnd()
            if (line.isBlank()) { Spacer(Modifier.height(6.dp)); continue }
            val heading = Regex("^#{1,4}\\s+(.*)$").find(line)?.groupValues?.get(1)
                ?: Regex("^\\*\\*([^*]+)\\*\\*:?$").find(line.trim())?.groupValues?.get(1)
            if (heading != null) {
                Text(heading.removeSuffix(":").uppercase(), style = Label, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                continue
            }
            val bullet = Regex("^\\s*[*-]\\s+(.*)$").find(line)?.groupValues?.get(1)
            if (bullet != null) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("•", color = Ink.ember, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(inline(bullet), style = MaterialTheme.typography.bodyMedium)
                }
                continue
            }
            Text(inline(line), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}
