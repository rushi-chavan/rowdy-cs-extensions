package com.rowdyCSExtensions

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass. Use the [BlankFragment.newInstance] factory method to create an
 * instance of this fragment.
 */
class UltimaFragment(val plugin: UltimaPlugin) : BottomSheetDialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private fun getDrawable(name: String): Drawable? {
        val id =
                plugin.resources!!.getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        return ResourcesCompat.getDrawable(plugin.resources!!, id, null)
    }

    private fun getString(name: String): String? {
        val id = plugin.resources!!.getIdentifier(name, "string", BuildConfig.LIBRARY_PACKAGE_NAME)
        return plugin.resources!!.getString(id)
    }

    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val settingsLayoutId =
                plugin.resources!!.getIdentifier("settings", "layout", "com.rowdyCSExtensions")
        val settingsLayout = plugin.resources!!.getLayout(settingsLayoutId)
        val settings = inflater.inflate(settingsLayout, container, false)
        val checkBoxLayout = settings.findView<LinearLayout>("sections_list")

        val elementsLayoutId =
                plugin.resources!!.getIdentifier("checkbox", "layout", "com.rowdyCSExtensions")

        val providers = plugin.fetchSections()
        Log.d("Rushi: ", providers.toString())
        providers.forEach { provider ->
            val parentElementLayout = plugin.resources!!.getLayout(elementsLayoutId)
            val parentCheckBoxView = inflater.inflate(parentElementLayout, container, false)
            val parentCheckBoxBtn = parentCheckBoxView.findView<CheckBox>("checkbox")
            parentCheckBoxBtn.text = provider.name
            parentCheckBoxBtn.id = View.generateViewId()
            checkBoxLayout.addView(parentCheckBoxView)

            provider.sections?.forEach { section ->
                val ChildElementLayout = plugin.resources!!.getLayout(elementsLayoutId)
                val childCheckBoxView = inflater.inflate(ChildElementLayout, container, false)
                val childCheckBoxBtn = childCheckBoxView.findView<CheckBox>("checkbox")
                // var param = childCheckBoxBtn.layoutParams as ViewGroup.MarginLayoutParams
                // param.setMargins(20, 0, 0, 0)
                // childCheckBoxBtn.layoutParams = param
                childCheckBoxBtn.text = section.name
                childCheckBoxBtn.isChecked = section.enabled ?: false
                childCheckBoxBtn.id = View.generateViewId()
                checkBoxLayout.addView(childCheckBoxView)
            }
        }

        return settings
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // val imageView = view.findView<ImageView>("imageView")
        // val imageView2 = view.findView<ImageView>("imageView2")
        // val textView = view.findView<TextView>("textView")
        // val textView2 = view.findView<TextView>("textView2")

        // textView.text = getString("hello_fragment")
        // textView.setTextAppearance(view.context, R.style.ResultInfoText)
        // textView2.text = view.context.resources.getText(R.string.legal_notice_text)
        // // textView2.text = readAllHomes()

        // imageView.setImageDrawable(getDrawable("ic_android_24dp"))
        // imageView.imageTintList = ColorStateList.valueOf(view.context.getColor(R.color.white))

        // imageView2.setImageDrawable(getDrawable("ic_android_24dp"))
        // imageView2.imageTintList =
        //         ColorStateList.valueOf(view.context.colorFromAttribute(R.attr.white))
    }
}