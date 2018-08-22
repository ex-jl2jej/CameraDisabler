package com.gmail.jl2jej.wor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.concurrent.Callable;

public class PolicyDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle saveInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("利用に当たってのお知らせ")
                .setMessage("このアプリはカメラ機能をON/OFFするアプリです。\nこの機能のために、端末の管理者権限（カメラを無効にする）を利用しています。\nアプリへの権限付与が必要です。\n端末管理アプリの設定で権限を付与して下さい\nこのまま利用すると異常終了しますが、権限を付与すれば利用可能です")
                .setPositiveButton("ＯＫ", null );

        return builder.create();
    }

    @Override
    public void onPause() {
        super.onPause();

        //dismiss();
    }
}
