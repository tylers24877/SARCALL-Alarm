package uk.mrs.saralarm.ui.help

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.android.synthetic.main.fragment_help.view.*
import uk.mrs.saralarm.R
import uk.mrs.saralarm.ui.settings.extra_ui.rules.drawableEnd


class HelpFragment : Fragment() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_help, container, false)
        root.help_web_view.settings.javaScriptEnabled = true
        root.help_web_view.loadUrl(
            "https://forms.office.com/Pages/ResponsePage.aspx?id=DQSIkWdsW0yxEjajBLZtrQAAAAAAAAAAAAO__cKaX7JUQzY0MURKOFdQNEFMOVRPQThOT08yRExFMy4u"
        )
        root.help_web_view_toggle_text_view.setOnClickListener {
            if (root.help_web_view_layout.visibility == View.GONE) {
                root.help_web_view_layout.visibility = View.VISIBLE
                root.help_copyright.visibility = View.GONE
                root.help_web_view_toggle_text_view.drawableEnd = getDrawable(requireContext(), R.drawable.ic_baseline_expand_less_24)
            } else {
                root.help_web_view_layout.visibility = View.GONE
                root.help_copyright.visibility = View.VISIBLE
                root.help_web_view_toggle_text_view.drawableEnd = getDrawable(requireContext(), R.drawable.ic_baseline_expand_more_24)

            }

        }
        setHasOptionsMenu(true)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // You can hide the state of the menu item here if you call getActivity().supportInvalidateOptionsMenu(); somewhere in your code
        val menuItem: MenuItem = menu.findItem(R.id.action_bar_help)
        menuItem.isVisible = false
    }
}