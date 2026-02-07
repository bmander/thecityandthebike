package com.thecityandthebike.util

import android.net.Uri
import com.thecityandthebike.BuildConfig

fun imageUrlToUri(url: String): Uri =
    if (url.startsWith("http")) Uri.parse(url)
    else Uri.parse(BuildConfig.BASE_URL + url)
