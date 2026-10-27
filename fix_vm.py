import sys

with open('app/src/main/java/com/example/ui/NewsViewModel.kt', 'r') as f:
    lines = f.readlines()

out = []
for line in lines:
    out.append(line)

# find the last "}" which closes the class
# The file ends with:
#    }
# }
#    private suspend fun fetchVideos() {
# ...
#    }
# }
# Let's just remove all lines from "    private suspend fun fetchVideos()" to the end, and the "}" before it.

# Actually, I'll just write a script to re-assemble it.
