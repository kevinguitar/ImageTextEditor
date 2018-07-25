package com.kevingt.imagetexteditor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_CHOOSE_IMAGE = 1
        private const val IMAGE_SPAN = "\n \n"
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.isNullOrEmpty()) {
                return
            }
            val spans = et_main.text.getSpans(0, et_main.length(), ImageSpan::class.java)
            if (spans.isEmpty()) {
                return
            }
            et_main.removeTextChangedListener(this)
            spans.forEach {
                val st = et_main.text.getSpanStart(it)
                val en = et_main.text.getSpanEnd(it)
                //TODO: deal with delete issue
                if (et_main.text[st - 1] != '\n') {
                    et_main.text = et_main.text.insert(st, "\n")
                    et_main.setSelection(st)
                } else if (et_main.text[en] != '\n') {
                    et_main.text = et_main.text.insert(en, "\n")
                    et_main.setSelection(en + 2)
                }
            }
            et_main.addTextChangedListener(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et_main.addTextChangedListener(textWatcher)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CHOOSE_IMAGE && data != null) {
            parseUri(data.data)
        }
    }

    private fun parseUri(uri: Uri) {
        val requestOptions = RequestOptions()
                .override(800)
                .fitCenter()
        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(requestOptions)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        insertImage(resource)
                    }
                })
    }

    private fun insertImage(bitmap: Bitmap) {
        val imageSpan = ImageSpan(this, bitmap)
        val builder = SpannableStringBuilder(et_main.text)

        val cursorPosition = et_main.selectionStart
        builder.replace(cursorPosition, et_main.selectionEnd, IMAGE_SPAN)
        builder.setSpan(imageSpan, cursorPosition + 1,
                cursorPosition + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        et_main.text = builder
        et_main.setSelection(cursorPosition + IMAGE_SPAN.length)
    }

    fun chooseImage(v: View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQ_CHOOSE_IMAGE)
    }

    fun takePhoto(v: View) {

    }
}
