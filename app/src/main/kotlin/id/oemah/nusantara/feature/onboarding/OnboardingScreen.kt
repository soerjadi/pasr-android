package id.oemah.nusantara.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import id.oemah.nusantara.R
import id.oemah.nusantara.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(onFinish: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    OnboardingScreen(onFinish = {
        viewModel.markSeen()
        onFinish()
    })
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val titles = listOf(R.string.onboarding_1_title, R.string.onboarding_2_title)
    val bodies = listOf(R.string.onboarding_1_body, R.string.onboarding_2_body)
    val pager = rememberPagerState { titles.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == titles.lastIndex

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text(stringResource(titles[page]), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(stringResource(bodies[page]), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(titles.size) { index ->
                Box(
                    Modifier.padding(4.dp).size(8.dp).clip(CircleShape).background(
                        if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }
        }
        PrimaryButton(
            text = stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next),
            onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
