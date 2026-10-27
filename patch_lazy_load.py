import os

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

old_block = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListWithRefresh(
    articles: List<NewsArticle>,
    isBgbTab: Boolean,
    isRefreshing: Boolean,
    visitedLinks: Set<String>,
    keywords: List<String> = emptyList(),
    onRefresh: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit
) {
  PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.fillMaxSize()
  ) {
    if (articles.isEmpty()) {"""

new_block = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListWithRefresh(
    articles: List<NewsArticle>,
    isBgbTab: Boolean,
    isRefreshing: Boolean,
    visitedLinks: Set<String>,
    keywords: List<String> = emptyList(),
    onRefresh: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit
) {
  var visibleLimit by remember { mutableIntStateOf(20) }
  
  // Reset limit when articles list completely changes or is refreshed
  LaunchedEffect(articles) {
      if (articles.size <= 20) {
          visibleLimit = 20
      }
  }

  val visibleArticles = articles.take(visibleLimit)
  val hasMore = articles.size > visibleLimit

  PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = { 
          visibleLimit = 20
          onRefresh() 
      },
      modifier = Modifier.fillMaxSize()
  ) {
    if (articles.isEmpty()) {"""

content = content.replace(old_block, new_block)

old_list = """      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
      ) {
        items(articles, key = { it.link + it.timestamp }) { article ->
          var isVisible by remember { mutableStateOf(false) }
          LaunchedEffect(article.link) {
            isVisible = true
          }
          val isVisited = article.link in visitedLinks
          AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500), initialOffsetY = { it / 2 })
          ) {
            if (isBgbTab) {
              BgbSpecialNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick, onWhatsAppClick)
            } else {
              RegularNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick, onWhatsAppClick)
            }
          }
        }
      }"""

new_list = """      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
      ) {
        items(visibleArticles, key = { it.link + it.timestamp }) { article ->
          var isVisible by remember { mutableStateOf(false) }
          LaunchedEffect(article.link) {
            isVisible = true
          }
          val isVisited = article.link in visitedLinks
          AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500), initialOffsetY = { it / 2 })
          ) {
            if (isBgbTab) {
              BgbSpecialNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick, onWhatsAppClick)
            } else {
              RegularNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick, onWhatsAppClick)
            }
          }
        }
        
        if (hasMore) {
          item {
            LaunchedEffect(visibleLimit) {
              delay(800) // Show loader for a bit to simulate loading
              visibleLimit += 20
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Primary,
                strokeWidth = 3.dp
              )
            }
          }
        }
      }"""
      
content = content.replace(old_list, new_list)

with open(filepath, "w") as f:
    f.write(content)

