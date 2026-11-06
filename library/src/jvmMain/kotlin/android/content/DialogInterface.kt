package android.content

/**
 * JVM stub for Android's [android.content.DialogInterface].
 *
 * Plugins reference this for dialog callback registration. On JVM desktop
 * we don't show Android dialogs — these stubs let bytecode link.
 */
interface DialogInterface {
    interface OnClickListener {
        fun onClick(dialog: DialogInterface?, which: Int)
    }
    interface OnMultiChoiceClickListener {
        fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean)
    }
    interface OnDismissListener {
        fun onDismiss(dialog: DialogInterface?)
    }
    interface OnCancelListener {
        fun onCancel(dialog: DialogInterface?)
    }

    fun cancel()
    fun dismiss()
}

/** Stub DialogInterface implementation. */
object NoopDialogInterface : DialogInterface {
    override fun cancel() {}
    override fun dismiss() {}
}
