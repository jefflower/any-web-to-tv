package com.jefflower.anywebtotv.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.jefflower.anywebtotv.R
import com.jefflower.anywebtotv.data.Bookmark
import com.jefflower.anywebtotv.util.UrlUtil

class AddSiteDialog : DialogFragment() {

    var existing: Bookmark? = null
    var onSave: ((url: String, name: String) -> Unit)? = null

    override fun getTheme(): Int = R.style.Theme_AnyWebToTv_Dialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_add_site, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editUrl = view.findViewById<EditText>(R.id.edit_url)
        val editName = view.findViewById<EditText>(R.id.edit_name)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        val btnSave = view.findViewById<Button>(R.id.btn_save)

        existing?.let {
            editUrl.setText(it.url)
            editName.setText(it.name)
        }

        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener {
            val raw = editUrl.text?.toString().orEmpty()
            val normalized = UrlUtil.normalize(raw)
            if (normalized == null) {
                Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = editName.text?.toString()?.trim().orEmpty().ifBlank { UrlUtil.deriveName(normalized) }
            onSave?.invoke(normalized, name)
            dismiss()
        }

        editUrl.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.55).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
