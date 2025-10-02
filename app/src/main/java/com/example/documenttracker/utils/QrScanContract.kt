package com.example.documenttracker.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QrScanContract : ActivityResultContract<ScanOptions, String?>() {
    override fun createIntent(context: Context, input: ScanOptions): Intent {
        return ScanContract().createIntent(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        val result = ScanContract().parseResult(resultCode, intent)
        return result.contents
    }
}
