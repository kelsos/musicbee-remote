package com.kelsos.mbrc.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.kelsos.mbrc.R
import com.kelsos.mbrc.networking.connections.ConnectionSettingsEntity
import kotterknife.bindView

class SettingsDialogFragment : DialogFragment() {

  private val hostEdit: EditText by bindView(R.id.settings_dialog__hostname_edit)
  private val nameEdit: EditText by bindView(R.id.settings_dialog__name_edit)
  private val portEdit: EditText by bindView(R.id.settings_dialog__port_edit)

  private var saveListener: SettingsSaveListener? = null
  private lateinit var settings: ConnectionSettingsEntity
  private var edit: Boolean = false

  private lateinit var fm: FragmentManager

  private fun setConnectionSettings(settings: ConnectionSettingsEntity) {
    this.settings = settings
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val context = requireContext()
    val builder = AlertDialog.Builder(context)
    with(builder) {
      setView(R.layout.ui_dialog_settings)
      setTitle(if (edit) R.string.settings_dialog_edit else R.string.settings_dialog_add)

      setNegativeButton(android.R.string.cancel) { dialogInterface, _ ->
        dialogInterface.dismiss()
      }

      val resId = if (edit) R.string.settings_dialog_save else R.string.settings_dialog_add

      setPositiveButton(resId) { dialog, _ ->
        var shouldIClose = true
        val hostname = hostEdit.text.toString()
        val computerName = nameEdit.text.toString()

        if (hostname.isEmpty() || computerName.isEmpty()) {
          shouldIClose = false
        }

        val portText = portEdit.text.toString()

        val portNum = if (TextUtils.isEmpty(portText)) 0 else Integer.parseInt(portText)
        if (isValid(portNum) && shouldIClose) {
          settings.apply {
            name = computerName
            address = hostname
            port = portNum
          }

          saveListener?.onSave(settings)
          dialog.dismiss()
        }
      }
    }
    return builder.create()
  }

  override fun onStart() {
    super.onStart()
    nameEdit.setText(settings.name)
    hostEdit.setText(settings.address)

    if (settings.port > 0) {
      portEdit.setText(settings.port.toString())
    }
  }

  private fun isValid(port: Int): Boolean = if (port < MIN_PORT || port > MAX_PORT) {
    val context = context ?: error("null context")
    AlertDialog.Builder(context)
      .setTitle(R.string.alert_invalid_range)
      .setMessage(R.string.alert_invalid_port_number)
      .setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
      .show()
    false
  } else {
    true
  }

  fun show(listener: SettingsSaveListener) {
    this.saveListener = listener
    show(fm, "settings_dialog")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!edit) {
      settings = ConnectionSettingsEntity()
    }
  }

  interface SettingsSaveListener {
    fun onSave(settings: ConnectionSettingsEntity)
  }

  companion object {

    private const val MAX_PORT = 65535
    private const val MIN_PORT = 1

    fun newInstance(
      settings: ConnectionSettingsEntity,
      fm: FragmentManager
    ): SettingsDialogFragment {

      return SettingsDialogFragment().apply {
        this.fm = fm
        setConnectionSettings(settings)
        edit = true
      }
    }

    fun create(fm: FragmentManager): SettingsDialogFragment {
      return SettingsDialogFragment().apply { this.fm = fm }
    }
  }
}