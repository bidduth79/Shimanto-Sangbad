import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddKeywordSection(onAdd: (String) -> Unit) {
  var text by remember { mutableStateOf("") }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedTextField(
      value = text,
      onValueChange = { text = it },
      modifier = Modifier.weight(1f),
      placeholder = { Text("নতুন কিওয়ার্ড যোগ করুন...") },
      singleLine = true,
      shape = RoundedCornerShape(24.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    IconButton(
      onClick = { 
        if (text.isNotBlank()) {
          onAdd(text)
          text = ""
        }
      },
      modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
    ) {
      Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary)
    }
  }
}
