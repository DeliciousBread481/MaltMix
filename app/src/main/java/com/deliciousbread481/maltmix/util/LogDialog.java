package com.deliciousbread481.maltmix.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class LogDialog {
    public static void show(Context context, String title, String message) {
        if (context == null) return;
        String fullText = title + "\n\n" + message;
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("复制全部", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("MaltMixLog", fullText));
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    public static void warn(Context context, String message) {
        show(context, "提示", message);
    }

    public static void error(Context context, String message) {
        show(context, "错误", message);
    }
}