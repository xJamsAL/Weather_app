package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText

object DialogManager {
    fun locationSettingsDialog(context: Context, listener: Listener){
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Enable location?")
        dialog.setMessage("Location disabled, do you want enable location?")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK"){ _,_ ->
            listener.onClick(null )
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel"){ _,_ ->
            dialog.dismiss()
        }
        dialog.show()
    }

    fun searchByNameDialog(context: Context, listener: Listener){
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_search_by_name, null)
        val edName = view.findViewById<EditText>(R.id.edtCityName)
        val builder = AlertDialog.Builder(context,R.style.MyAlertDialogStyle )

//            .setTitle("City name:")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                listener.onClick(edName.text.toString())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }


    interface Listener{
        fun onClick(name: String?)
    }
}