package com.embroidery.studio

import android.graphics.Bitmap
import android.net.Uri

data class Stitch(val x: Float, val y: Float, val colorIdx: Int)

data class Project(
    val name: String = "مشروع جديد",
    val imageUri: Uri? = null,
    val extracted: Bitmap? = null,
    val colors: List<Int> = emptyList(),
    val stitches: List<Stitch> = emptyList(),
    val widthMM: Float = 0f,
    val heightMM: Float = 0f
)
