package ng.com.stemcoders.campusspace.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AlertUtil
{
    public static AlertDialog buildAlert (Context context, String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("Ok", new AlertDialog.OnClickListener()
                {
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        return dialog;
    }

    public static AlertDialog buildAlert (Context context, View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true);
        AlertDialog dialog = builder.create();
        return dialog;
    }

    public static AlertDialog buildConfirmDialog (Context context, String title, String message, final Runnable onConfirm)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Cancel", new AlertDialog.OnClickListener()
                {
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Yes", new AlertDialog.OnClickListener()
                {
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        onConfirm.run();
                    }
                });

        AlertDialog dialog = builder.create();
        return dialog;
    }

    public static void showAlert (Context context, String title, String message)
    {
        buildAlert(context, title, message).show();
    }

    public static void showConfirmDialog (Context context, String title, String message, final Runnable onConfirm)
    {
        buildConfirmDialog(context, title, message, onConfirm).show();
    }
}
