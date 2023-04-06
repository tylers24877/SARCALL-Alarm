/*
 *  Copyright (C) Tyler Simmonds - All Rights Reserved
 *  Unauthorised copying of this file, via any medium is prohibited
 *  Written by Tyler Simmonds on behalf of SARCALL LTD, 2021
 *
 */

package uk.mrs.saralarm.support

import android.text.Html
import android.text.Spanned

object Util {
    fun fromHtml(string: String): Spanned {
        return Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY)
    }
}