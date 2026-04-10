package com.shanufx.hotspotx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.shanufx.hotspotx.ui.navigation.HotspotXNavGraph
import com.shanufx.hotspotx.ui.theme.HotspotXTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HotspotXTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HotspotXNavGraph()
                }
            }
        }
    }
}
