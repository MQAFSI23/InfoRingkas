package com.example.inforingkas.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

public class ThemeUtils {

    @ColorInt
    public static int getThemeColor(Context context, @AttrRes int attributeColor) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[]{attributeColor});
        int color = typedArray.getColor(0, 0); // 0 adalah default jika atribut tidak ditemukan
        typedArray.recycle();
        return color;
    }
}