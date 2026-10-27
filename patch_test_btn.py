import os

filepath = "app/src/main/java/com/example/ui/SettingsContent.kt"
with open(filepath, "r") as f:
    content = f.read()

# add imports
imports = """import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OutOfQuotaPolicy
import com.example.data.broadcast.TelegramBroadcastWorker
"""
if "import androidx.work.WorkManager" not in content:
    content = content.replace("import androidx.compose.foundation.background", imports + "import androidx.compose.foundation.background")

# Add button inside the telegram card or below it
telegram_card = """                        onClick = { showTelegramDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("সেটিংস", fontSize = 13.sp)
                    }
                }"""

new_telegram_card = telegram_card + """
                Spacer(modifier = Modifier.height(12.dp))
                val context = LocalContext.current
                Button(
                    onClick = {
                        val req = OneTimeWorkRequestBuilder<TelegramBroadcastWorker>().build()
                        WorkManager.getInstance(context).enqueue(req)
                        Toast.makeText(context, "টেস্ট ব্রডকাস্ট শুরু হয়েছে! কিছুক্ষণ পর টেলিগ্রাম চেক করুন।", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorContainer, contentColor = OnErrorContainer)
                ) {
                    Text("এখনই টেস্ট নিউজ পাঠান")
                }"""
content = content.replace(telegram_card, new_telegram_card)

with open(filepath, "w") as f:
    f.write(content)
