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
import uk.mrs.saralarm.R
import uk.mrs.saralarm.databinding.FragmentHelpBinding
import uk.mrs.saralarm.ui.settings.extra_ui.rules.drawableEnd


class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        binding.apply {
            helpWebView.settings.javaScriptEnabled = true
            helpWebView.loadUrl(
                "https://forms.office.com/Pages/ResponsePage.aspx?id=DQSIkWdsW0yxEjajBLZtrQAAAAAAAAAAAAO__cKaX7JUQzY0MURKOFdQNEFMOVRPQThOT08yRExFMy4u"
            )
            helpWebViewToggleTextView.setOnClickListener {
                if (helpWebViewLayout.visibility == View.GONE) {
                    helpWebViewLayout.visibility = View.VISIBLE
                    helpCopyright.visibility = View.GONE
                    helpWebViewToggleTextView.drawableEnd = getDrawable(requireContext(), R.drawable.ic_baseline_expand_less_24)
                } else {
                    helpWebViewLayout.visibility = View.GONE
                    helpCopyright.visibility = View.VISIBLE
                    helpWebViewToggleTextView.drawableEnd = getDrawable(requireContext(), R.drawable.ic_baseline_expand_more_24)

                }

            }
            setHasOptionsMenu(true)
            enterTransition = MaterialFadeThrough()
            exitTransition = MaterialFadeThrough()
        }
        return binding.root
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}