import os

filepath = "app/src/main/java/com/example/ui/ProfileContent.kt"
with open(filepath, "r") as f:
    content = f.read()

if "WindowInsets" not in content:
    content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp\nimport androidx.compose.foundation.layout.WindowInsets\nimport androidx.compose.foundation.layout.imePadding")
    content = content.replace("Column(modifier = modifier.fillMaxSize().padding(16.dp))", "Column(modifier = modifier.fillMaxSize().padding(16.dp).imePadding())")

with open(filepath, "w") as f:
    f.write(content)

filepath = "app/src/main/java/com/example/ui/SettingsContent.kt"
with open(filepath, "r") as f:
    content = f.read()

if "WindowInsets" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.layout.imePadding")
    content = content.replace("Column(modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState))", "Column(modifier = modifier.fillMaxSize().padding(16.dp).imePadding().verticalScroll(scrollState))")

with open(filepath, "w") as f:
    f.write(content)

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

if "contentWindowInsets" not in content:
    content = content.replace("bottomBar = { BottomNavBar(selectedTab = uiState.selectedBottomTab, onTabSelected = { viewModel.selectBottomTab(it) }) }", "bottomBar = { BottomNavBar(selectedTab = uiState.selectedBottomTab, onTabSelected = { viewModel.selectBottomTab(it) }) },\n          contentWindowInsets = ScaffoldDefaults.contentWindowInsets")

with open(filepath, "w") as f:
    f.write(content)
