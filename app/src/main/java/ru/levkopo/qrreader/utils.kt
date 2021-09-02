package ru.levkopo.qrreader

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.color.MaterialColors

const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT

var View.padding get() = paddingLeft
    set(padding) = setPadding(padding, padding, padding, padding)

var View.paddingLR
    get() = paddingLeft
    set(value) = setPadding(value, paddingTop, value, paddingBottom)

var View.paddingTB
    get() = paddingTop
    set(value) = setPadding(paddingLeft, value, paddingRight, value)

var View.backgroundColor
    get() = background.let {
        if(it is ColorDrawable)
            it.color
        else 0
    }

    set(value) {
        background = ColorDrawable(value)
    }

inline fun <reified V: View> ViewGroup.view(
    width: Int = WRAP_CONTENT,
    height: Int = WRAP_CONTENT,
    block: V.() -> Unit = {}) = addView(context.view(block), width, height)

inline fun <reified V: View> Context.view(
    block: V.() -> Unit = {}): V = V::class.java.getConstructor(Context::class.java).newInstance(this).apply {
    block()
}

inline fun <reified V: View> View.view(
    block: V.() -> Unit = {}): V = context.view(block)

fun <V: View> V.onClick(block: V.() -> Unit = {}) {
    setOnClickListener { this.block() }
}

fun Context.dp(dp: Int) = dp * (resources.displayMetrics.densityDpi.toFloat() /
        DisplayMetrics.DENSITY_DEFAULT).toInt()
fun View.attr(@AttrRes attr: Int) = MaterialColors.getColor(this, attr)
fun Context.color(@ColorRes color: Int) = ContextCompat.getColor(this, color)
fun Context.string(@StringRes string: Int) = getString(string)

fun ViewGroup.box(
    orientation: Int = LinearLayout.VERTICAL,
    width: Int = WRAP_CONTENT,
    height: Int = WRAP_CONTENT,
    block: LinearLayout.() -> Unit = {}) = addView(context.box(orientation, block), width, height)

fun Context.box(
    orientation: Int = LinearLayout.VERTICAL,
    block: LinearLayout.() -> Unit = {}) = view<LinearLayout> {
    this.orientation = orientation
    padding = context.dp(10)
    block()

}

inline fun <T> ViewGroup.state(
    defaultValue: T? = null,
    crossinline body: StateLayout<T?>.(T?, (T?) -> Unit) -> Unit
) = context.getAppCompatActivity()?.state(defaultValue, body)?.let {
    addView(it, MATCH_PARENT, MATCH_PARENT)
} ?: Unit

inline fun <T> AppCompatActivity.state(
    defaultValue: T? = null,
    crossinline body: StateLayout<T?>.(T?, (T?) -> Unit) -> Unit
): StateLayout<T?> {
    return view {
        liveData.observe(this@state, { value ->
            removeAllViews()
            body(value) { liveData.postValue(it) }
        })

        liveData.postValue(defaultValue)
    }
}

class StateLayout<T>(context: Context): FrameLayout(context) {
    val liveData = MutableLiveData<T>()
}

fun Context.getAppCompatActivity(): AppCompatActivity? {
    return when (this) {
        is AppCompatActivity -> this
        is ContextThemeWrapper -> baseContext.getAppCompatActivity()
        else -> null
    }
}