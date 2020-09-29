package com.gd.aiwnext.deal.Support.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import androidx.appcompat.widget.AppCompatEditText;

import com.gd.aiwnext.deal.Support.Utils.Listener;

public class SEditText extends AppCompatEditText {

    private Listener onBack;
    private Listener onDelete;

    public void setOnBackListener(Listener onBack) {
        this.onBack = onBack;
    }

    public void setOnDeleteListener(Listener onDelete) { this.onDelete = onDelete; }

    public SEditText(Context context) {
        super(context);
    }

    public SEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new SConnection(super.onCreateInputConnection(outAttrs), true);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (onBack != null) {
                onBack.onEvent();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    class SConnection extends InputConnectionWrapper {

        public SConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            // Try catch because it crashes sometimes on Huawei devices
            try {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    if (getSelectionStart() == 0) {
                        if (onDelete != null) {
                            onDelete.onEvent();
                        }
                    }
                }
            } catch (Exception ignored) { }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // Try catch because it crashes sometimes on Huawei devices
            try {
                if (getSelectionStart() == 0) {
                    sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                    return false;
                }
            } catch (Exception ignored) { }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}
