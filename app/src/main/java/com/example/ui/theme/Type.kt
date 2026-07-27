package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlayfairRegularFamily = FontFamily(
    Font(R.font.playfairdisplay_regular, FontWeight.Normal)
)

val PlayfairBoldFamily = FontFamily(
    Font(R.font.playfairdisplay_bold, FontWeight.Bold)
)

val PlayfairMediumItalicFamily = FontFamily(
    Font(R.font.playfairdisplay_mediumitalic, FontWeight.Medium, FontStyle.Italic)
)

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
  )

