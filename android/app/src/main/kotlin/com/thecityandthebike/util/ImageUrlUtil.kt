package com.thecityandthebike.util

import android.net.Uri
import androidx.core.net.toUri
import com.thecityandthebike.BuildConfig

fun imageUrlToUri(url: String): Uri =
    if (url.startsWith("http")) url.toUri()
    else (BuildConfig.BASE_URL + url).toUri()
