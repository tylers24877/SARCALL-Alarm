package uk.mrs.saralarm.support

import android.text.Html
import android.text.Spanned

object Util {
    fun fromHtml(string: String): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(string)
        }
    }
}