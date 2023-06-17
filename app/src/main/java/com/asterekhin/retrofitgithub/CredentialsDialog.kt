package com.asterekhin.retrofitgithub

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment


class CredentialsDialog : DialogFragment() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var listener: ICredentialsDialogListener

    interface ICredentialsDialogListener {
        fun onDialogPositiveClick(username: String, password: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is ICredentialsDialogListener) {
            listener = (activity as ICredentialsDialogListener?)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View =
            requireActivity().getLayoutInflater().inflate(R.layout.dialog_credentials, null)
        usernameEditText = view.findViewById<View>(R.id.username_edittext) as EditText
        passwordEditText = view.findViewById<View>(R.id.password_edittext) as EditText
        usernameEditText.setText(requireArguments().getString("username"))
        passwordEditText.setText(requireArguments().getString("password"))
        val builder: AlertDialog.Builder = android.app.AlertDialog.Builder(getActivity())
            .setView(view)
            .setTitle("Credentials")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue"
            ) { _, _ ->
                listener.onDialogPositiveClick(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
        return builder.create()
    }
}