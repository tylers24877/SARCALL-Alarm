package uk.mrs.saralarm.ui.help

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_help.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.rules.drawableEnd


class HelpFragment : Fragment() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_help, container, false)
        root.help_webView.settings.javaScriptEnabled = true
        root.help_webView.loadUrl(
            "https://forms.office.com/Pages/ResponsePage.aspx?id=DQSIkWdsW0yxEjajBLZtrQAAAAAAAAAAAAO__cKaX7JUQzY0MURKOFdQNEFMOVRPQThOT08yRExFMy4u"
        )
        root.help_webView_Toggle_textView.setOnClickListener {
            if (root.help_webViewLayout.visibility == View.GONE) {
                root.help_webViewLayout.visibility = View.VISIBLE
                root.help_copyright.visibility = View.GONE
                root.help_webView_Toggle_textView.drawableEnd = getDrawable(requireContext(), R.drawable.ic_baseline_expand_less_24)
            } else {
                root.help_webViewLayout.visibility = View.GONE
                root.help_copyright.visibility = View.VISIBLE
                root.help_webView_Toggle_textView.drawableEnd = getDrawable(requireContext(), R.drawable.ic_baseline_expand_more_24)

            }

        }
        return root
    }

    private fun getDrawable(context: Context, id: Int): Drawable? {
        val version = Build.VERSION.SDK_INT
        return if (version >= 21) {
            ContextCompat.getDrawable(context, id)
        } else {
            ResourcesCompat.getDrawable(context.resources, id, null)
        }
    }
}