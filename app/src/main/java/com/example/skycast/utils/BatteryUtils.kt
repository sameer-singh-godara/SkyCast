package com.example.skycast.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class BatteryUtils {
    companion object {
        @Composable
        fun observeBatteryLevel(context: Context): Int {
            var batteryPercentage by remember { mutableStateOf(-1) }

            val batteryReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level != -1 && scale != -1) {
                        batteryPercentage = (level * 100 / scale)
                    }
                }
            }

            DisposableEffect(Unit) {
                val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context.registerReceiver(batteryReceiver, intentFilter)

                onDispose {
                    context.unregisterReceiver(batteryReceiver)
                }
            }

            return batteryPercentage
        }

        fun shouldAutoRefresh(batteryPercentage: Int): Boolean {
            return batteryPercentage >= 30
        }

    }
}