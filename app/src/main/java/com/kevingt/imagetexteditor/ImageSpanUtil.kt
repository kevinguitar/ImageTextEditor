package com.kevingt.imagetexteditor

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.EditText

class ImageSpanUtil {

    companion object {
        private const val IMAGE_SPAN = "\n \n"

        fun setPlaceHolderSpan(editText: EditText, drawable: Drawable): Int {
            val span = ImageSpan(drawable)
            val builder = SpannableStringBuilder(editText.text)

            val cursorPosition = editText.selectionStart
            builder.replace(cursorPosition, editText.selectionEnd, IMAGE_SPAN)
            builder.setSpan(span, cursorPosition + 1,
                    cursorPosition + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            editText.text = builder
            editText.setSelection(cursorPosition + IMAGE_SPAN.length)

            return span.hashCode()
        }

        fun replaceLoadedImage(editText: EditText, bitmap: Bitmap, hashCode: Int) {
            val spans = editText.text.getSpans(0, editText.length(), ImageSpan::class.java)
            if (spans.isEmpty()) {
                return
            }
            val span = ImageSpan(editText.context, bitmap)
            val builder = SpannableStringBuilder(editText.text)
            val cursorPosition = editText.selectionStart
            spans.forEach {
                if (it.hashCode() == hashCode) {
                    val st = editText.text.getSpanStart(it)
                    val en = editText.text.getSpanEnd(it)
                    builder.setSpan(span, st, en, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            editText.text = builder
            editText.setSelection(cursorPosition)
        }
    }
}