package xyz.ivaniskandar.composefastscroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.composefastscroller.ui.theme.ComposeFastScrollerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeFastScrollerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val listState = rememberLazyListState()
                    val padding = PaddingValues(top = 16.dp, bottom = 36.dp, end = 0.dp)
                    VerticalFastScroller(
                        listState = listState,
                        topContentPadding = padding.calculateTopPadding(),
                        endContentPadding = padding.calculateEndPadding(LocalLayoutDirection.current)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(1f),
                            contentPadding = padding,
                            reverseLayout = false,
                        ) {
                            item(contentType = "header") {
                                Text(
                                    text = "What supposed to be a header item",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = .8f))
                                        .padding(vertical = 96.dp)
                                )
                            }
                            (1 until 25).forEach {
                                item(contentType = "item") {
                                    Text(
                                        text = "Item $it",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.LightGray.copy(alpha = .8f))
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            }
                            item(contentType = "intermezzo") {
                                Text(
                                    text = "Intermezzo item",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = .8f))
                                        .padding(vertical = 48.dp)
                                )
                            }
                            (25 until 50).forEach {
                                item(contentType = "item") {
                                    Text(
                                        text = "Item $it",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.LightGray.copy(alpha = .8f))
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
