package cz.applifting.humansis.extensions

import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

fun ImageView.tintedDrawable(@DrawableRes drawableRes: Int, @ColorRes colorRes: Int) {
    val vectorDrawable = VectorDrawableCompat.create(
        this.context.resources,
        drawableRes,
        null
    )

    val drawable = DrawableCompat.wrap(vectorDrawable!!)

    val color = ContextCompat.getColor(
        this.context,
        colorRes
    )
    DrawableCompat.setTint(drawable.mutate(), color)
    this.setImageDrawable(drawable)
}

fun ImageView.simpleDrawable(@DrawableRes drawableRes: Int) {
    val drawable = ContextCompat.getDrawable(this.context, drawableRes)
    this.setImageDrawable(drawable)
}