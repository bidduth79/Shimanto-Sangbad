import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Replace the filter/map code to use explicit names instead of 'it' where type inference might fail
content = content.replace(
    "val bgbNews = allArticles.filter { it.category == \"bgb\" }\n                .map { it.copy(isSaved = savedLinks.contains(it.link)) }",
    "val bgbNews = allArticles.filter { article -> article.category == \"bgb\" }\n                .map { article -> article.copy(isSaved = savedLinks.contains(article.link)) }"
)

content = content.replace(
    "val nationalNews = allArticles.filter { it.category == \"national\" }\n                .map { it.copy(isSaved = savedLinks.contains(it.link)) }",
    "val nationalNews = allArticles.filter { article -> article.category == \"national\" }\n                .map { article -> article.copy(isSaved = savedLinks.contains(article.link)) }"
)

content = content.replace(
    "val latest = (bgbNews + nationalNews).sortedByDescending { it.timestamp }.take(10)",
    "val latest = (bgbNews + nationalNews).sortedByDescending { article -> article.timestamp }.take(10)"
)

with open(filepath, "w") as f:
    f.write(content)
