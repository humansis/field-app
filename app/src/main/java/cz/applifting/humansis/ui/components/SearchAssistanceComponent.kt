package cz.applifting.humansis.ui.components

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.hideSoftKeyboard
import kotlinx.android.synthetic.main.component_search.view.*
import quanti.com.kotlinlog.Log

class SearchAssistanceComponent @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.component_search, this, true)
        orientation = HORIZONTAL
        et_search.imeOptions = EditorInfo.IME_ACTION_SEARCH
        et_search.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // just close keyboard, we already search on text changed
                    et_search.hideSoftKeyboard()
                    return true
                }
                return false
            }
        })
    }

    internal fun onTextChanged(search: (String) -> Unit?) {
        et_search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                search(s.toString())
            }
        })
    }

    internal fun onSort(sort: () -> Unit) {
        btn_sort.setOnClickListener {
            Log.d(TAG, "Sort button clicked")
            sort()
        }
    }

    internal fun changeSortIcon(sort: Sort) {
        val textResId = when (sort) {
            Sort.DEFAULT -> R.string.sort_newest
            Sort.AZ -> R.string.sort_az
            Sort.ZA -> R.string.sort_za
        }

        btn_sort.text = context.getString(textResId)
    }

    internal fun clearSearch() {
        et_search.text.clear()
    }

    companion object {
        private val TAG = SearchAssistanceComponent::class.java.simpleName
    }
}
