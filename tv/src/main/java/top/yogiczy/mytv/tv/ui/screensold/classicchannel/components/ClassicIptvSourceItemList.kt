package top.yogiczy.mytv.tv.ui.screensold.classicchannel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yogiczy.mytv.core.data.entities.iptvsource.IptvSource
import top.yogiczy.mytv.core.data.entities.iptvsource.IptvSourceList
import top.yogiczy.mytv.tv.ui.material.rememberDebounceState
import top.yogiczy.mytv.tv.ui.screen.settings.settingsVM
import top.yogiczy.mytv.tv.ui.theme.MyTvTheme
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunchedSaveable
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents
import top.yogiczy.mytv.tv.ui.utils.ifElse
import top.yogiczy.mytv.tv.ui.utils.saveFocusRestorer
import top.yogiczy.mytv.tv.ui.utils.saveRequestFocus
import kotlin.math.max

@Composable
fun ClassicIptvSourceItemList(
    modifier: Modifier = Modifier,
    iptvSourceListProvider: () -> IptvSourceList = { IptvSourceList() },
    currentIptvSourceProvider: () -> IptvSource = { IptvSource() },
    onIptvSourceFocused: (IptvSource) -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val iptvSourceList = iptvSourceListProvider()
    val currentIptvSource = currentIptvSourceProvider()
    val itemFocusRequesterList = List(iptvSourceList.size) { FocusRequester() }

    var focusedIptvSource by remember { mutableStateOf(currentIptvSource) }
    
    val listState = rememberLazyListState(max(0, iptvSourceList.indexOf(currentIptvSource)))
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { _ ->
                onUserAction()
            }
    }

    val coroutineScope = rememberCoroutineScope()
    val firstFocusRequester = remember { FocusRequester() }
    val lastFocusRequester = remember { FocusRequester() }
    fun scrollToFirst() {
        coroutineScope.launch {
            listState.scrollToItem(0)
            firstFocusRequester.saveRequestFocus()
        }
    }
    fun scrollToLast() {
        coroutineScope.launch {
            listState.scrollToItem(iptvSourceList.lastIndex)
            lastFocusRequester.saveRequestFocus()
        }
    }

    LazyColumn(
        modifier = modifier
            .width(140.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(0.9f))
            .ifElse(
                settingsVM.uiFocusOptimize,
                Modifier.saveFocusRestorer {
                    itemFocusRequesterList.getOrElse(iptvSourceList.indexOf(focusedIptvSource)) { FocusRequester.Default }
                },
            ),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(iptvSourceList) { index, iptvSource ->
            val isSelected by remember { derivedStateOf { iptvSource == focusedIptvSource } }

            ClassicIptvSourceItem(
                modifier = Modifier
                    .ifElse(iptvSource == currentIptvSource, Modifier.focusOnLaunchedSaveable())
                    .focusRequester(itemFocusRequesterList[index])
                    .ifElse(
                        index == 0,
                        Modifier
                            .focusRequester(firstFocusRequester)
                            .handleKeyEvents(onUp = { scrollToLast() })
                    )
                    .ifElse(
                        index == iptvSourceList.lastIndex,
                        Modifier
                            .focusRequester(lastFocusRequester)
                            .handleKeyEvents(onDown = { scrollToFirst() })
                    ),
                iptvSourceProvider = { iptvSource },
                isSelectedProvider = { isSelected },
                onFocused = {
                    focusedIptvSource = iptvSource
                    onIptvSourceFocused(iptvSource)
                },
            )
        }
    }
}

@Composable
private fun ClassicIptvSourceItem(
    modifier: Modifier = Modifier,
    iptvSourceProvider: () -> IptvSource = { IptvSource() },
    isSelectedProvider: () -> Boolean = { false },
    onFocused: () -> Unit = {},
) {
    val iptvSource = iptvSourceProvider()

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    DenseListItem(
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
                if (isFocused) onFocused()
            }
            .handleKeyEvents(
                isFocused = { isFocused },
                focusRequester = focusRequester,
                onSelect = {},
            ),
        colors = ListItemDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        selected = isSelectedProvider(),
        onClick = {},
        headlineContent = {
            Text(
                text = iptvSource.name,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .ifElse(isFocused, Modifier.basicMarquee()),
            )
        },
    )
}

@Preview
@Composable
private fun ClassicIptvSourceItemListPreview() {
    MyTvTheme {
        ClassicIptvSourceItemList(
            iptvSourceListProvider = { IptvSourceList.EXAMPLE },
            currentIptvSourceProvider = { IptvSourceList.EXAMPLE.first() },
        )
    }
} 