package com.gd.aiwnext.deal.Support.Views

import android.content.Context
import android.content.res.Resources.Theme
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.SwitchCompat
import com.gd.aiwnext.deal.R

class CompatSwitch : SwitchCompat {

    private lateinit var theme: Theme

    fun setTheme(theme: Theme) {
        this.theme = theme
        buildSelector()
    }

    private fun buildSelector() {
        val checked = IntArray(1)
        checked[0] = android.R.attr.state_checked
        val unchecked = IntArray(1)
        unchecked[0] = android.R.attr.state_checked.inv()

        val thumbD = StateListDrawable()
        thumbD.addState(checked, getThumbOn())
        thumbD.addState(unchecked, getThumbOff())
        thumbDrawable = thumbD
        val trackD = StateListDrawable()
        trackD.addState(checked, getTrackOn())
        trackD.addState(unchecked, getTrackOff())
        trackDrawable = trackD
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private fun getThumbOn(): LayerDrawable {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
        val drawable = resources.getDrawable(R.drawable.switch_thumb_on) as LayerDrawable
        val shape = drawable.findDrawableByLayerId(R.id.thumbOn) as GradientDrawable
        shape.setColor(typedValue.data)
        return drawable
    }

    private fun getThumbOff(): Drawable {
        return resources.getDrawable(R.drawable.switch_thumb_off)
    }

    private fun getTrackOn(): Drawable {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        val drawable = resources.getDrawable(R.drawable.switch_track_on) as GradientDrawable
        drawable.setColor(typedValue.data)
        return drawable
    }

    private fun getTrackOff(): Drawable {
        return resources.getDrawable(R.drawable.switch_track_off)
    }
}
