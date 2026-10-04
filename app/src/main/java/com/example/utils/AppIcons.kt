package com.example.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Heart = ImageVector.Builder(
        name = "Heart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        moveTo(12f, 21.35f)
        lineToRelative(-1.45f, -1.32f)
        curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
        curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
        curveToRelative(1.74f, 0f, 3.41f, 0.81f, 4.5f, 2.09f)
        curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
        curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
        curveToRelative(0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
        lineTo(12f, 21.35f)
        close()
    }.build()

    val Comment = ImageVector.Builder(
        name = "Comment",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(21f, 15f)
        curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
        horizontalLineTo(7f)
        lineToRelative(-4f, 4f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        verticalLineToRelative(10f)
        close()
    }.build()

    val Share = ImageVector.Builder(
        name = "Share",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(18f, 16.08f)
        curveToRelative(-0.76f, 0f, -1.44f, 0.3f, -1.96f, 0.77f)
        lineTo(8.91f, 12.7f)
        curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
        reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f)
        lineToRelative(7.05f, -4.11f)
        curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
        curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
        reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
        reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f)
        curveToRelative(0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
        lineTo(8.04f, 9.81f)
        curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        curveToRelative(0.79f, 0f, 1.5f, -0.31f, 2.04f, -0.81f)
        lineToRelative(7.12f, 4.16f)
        curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
        curveToRelative(0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
        reflectiveCurveToRelative(2.92f, -1.31f, 2.92f, -2.92f)
        reflectiveCurveToRelative(-1.31f, -2.92f, -2.92f, -2.92f)
        close()
    }.build()

    val Report = ImageVector.Builder(
        name = "Report",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(15.73f, 3f)
        horizontalLineTo(8.27f)
        lineTo(3f, 8.27f)
        verticalLineToRelative(7.46f)
        lineTo(8.27f, 21f)
        horizontalLineToRelative(7.46f)
        lineTo(21f, 15.73f)
        verticalLineTo(8.27f)
        lineTo(15.73f, 3f)
        close()
        moveTo(12f, 17.3f)
        curveToRelative(-0.72f, 0f, -1.3f, -0.58f, -1.3f, -1.3f)
        reflectiveCurveToRelative(0.58f, -1.3f, 1.3f, -1.3f)
        reflectiveCurveToRelative(1.3f, 0.58f, 1.3f, 1.3f)
        reflectiveCurveToRelative(-0.58f, 1.3f, -1.3f, 1.3f)
        close()
        moveTo(13f, 13f)
        horizontalLineToRelative(-2f)
        verticalLineTo(7f)
        horizontalLineToRelative(2f)
        verticalLineTo(13f)
        close()
    }.build()
}
