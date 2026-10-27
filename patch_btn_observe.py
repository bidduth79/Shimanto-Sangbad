import os

filepath = "app/src/main/java/com/example/ui/SettingsContent.kt"
with open(filepath, "r") as f:
    content = f.read()

imports = """import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
"""
if "import androidx.work.WorkInfo" not in content:
    content = content.replace("import androidx.work.WorkManager", imports + "import androidx.work.WorkManager")

old_btn = """                val context = LocalContext.current
                Button(
                    onClick = {
                        val req = OneTimeWorkRequestBuilder<TelegramBroadcastWorker>().build()
                        WorkManager.getInstance(context).enqueue(req)
                        Toast.makeText(context, "টেস্ট ব্রডকাস্ট শুরু হয়েছে! কিছুক্ষণ পর টেলিগ্রাম চেক করুন।", Toast.LENGTH_LONG).show()
                    },"""

new_btn = """                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                Button(
                    onClick = {
                        val req = OneTimeWorkRequestBuilder<TelegramBroadcastWorker>().build()
                        val wm = WorkManager.getInstance(context)
                        wm.enqueue(req)
                        Toast.makeText(context, "টেস্ট ব্রডকাস্ট শুরু হয়েছে...", Toast.LENGTH_SHORT).show()
                        wm.getWorkInfoByIdLiveData(req.id).observe(lifecycleOwner) { workInfo ->
                            if (workInfo != null && workInfo.state.isFinished) {
                                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                    Toast.makeText(context, "✅ ব্রডকাস্ট সফল হয়েছে!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "❌ ব্রডকাস্ট ফেইল করেছে! ইন্টারনেট ও API Key চেক করুন।", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },"""

content = content.replace(old_btn, new_btn)

with open(filepath, "w") as f:
    f.write(content)
