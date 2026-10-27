import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

old_start_auto = """    private fun startAutoUpdate() {
        // BGB keywords update every 10 minutes
        viewModelScope.launch {
            while (true) {
                delay(10 * 60 * 1000) // 10 minutes
                if (_uiState.value.isAutoUpdating) {
                    fetchBgbNews()
                }
            }
        }
        
        // National keywords update every 20 minutes
        viewModelScope.launch {
            while (true) {
                delay(20 * 60 * 1000) // 20 minutes
                if (_uiState.value.isAutoUpdating) {
                    fetchNationalNews()
                }
            }
        }
    }"""

new_start_auto = """    private fun startAutoUpdate() {
        // BGB keywords update every 10 minutes
        viewModelScope.launch {
            while (true) {
                delay(10 * 60 * 1000) // 10 minutes
                if (_uiState.value.isAutoUpdating) {
                    fetchBgbNews()
                }
            }
        }
    }"""
    
content = content.replace(old_start_auto, new_start_auto)

with open(filepath, "w") as f:
    f.write(content)
