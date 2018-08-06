package com.kevingt.imagetexteditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.util.DisplayMetrics
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

class ImageEditText @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyleAttr: Int = android.R.attr.editTextStyle)
    : AppCompatEditText(context, attrs, defStyleAttr) {

    companion object {
        private const val IMAGE_SPAN = "\n \n"
    }

    private lateinit var rectProgress: RectF
    private lateinit var paintProgress: Paint

    private val progressStrokeWidth = 16f
    private val progressWidth = 160

    private var spanHashCode = 0
    private var screenWidth = 0
    private var image: Bitmap? = null
    private var imageCopy: Bitmap? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val display = DisplayMetrics()
        (context as MainActivity).windowManager.defaultDisplay.getMetrics(display)
        screenWidth = display.widthPixels
        initSetting()
    }

    override fun onDetachedFromWindow() {
        image?.recycle()
        image = null
        imageCopy?.recycle()
        imageCopy = null
        super.onDetachedFromWindow()
    }

    /**
     * @param uri Pass the uri of photo from intent data
     */
    fun insertImage(uri: Uri) {
        val requestOptions = RequestOptions()
                .override((screenWidth * 0.8).toInt())
                .fitCenter()

        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(requestOptions)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        image = Bitmap.createBitmap(resource)
                        initUploadView()
                        setupPlaceHolder()
                    }
                })

        //TODO: upload image and update progress
    }

    /**
     * @param progress Upload progress between 0-100
     */
    fun updateUploadProgress(progress: Int) {
        if (progress >= 100) {      //upload success
            replaceImage(image)
            return
        }
        if (progress >= 0) {
            drawProgress(progress)
        }
    }

    private fun initSetting() {
        paintProgress = Paint()
        paintProgress.isAntiAlias = true
        paintProgress.style = Paint.Style.STROKE
        paintProgress.strokeWidth = progressStrokeWidth
        paintProgress.color = ContextCompat.getColor(context, R.color.colorAccent)
        addTextChangedListener(textWatcher)
    }

    private fun initUploadView() {
        rectProgress = RectF()
        rectProgress.left = (image!!.width / 2).toFloat() - (progressWidth / 2)
        rectProgress.top = (image!!.height / 2).toFloat() - (progressWidth / 2)
        rectProgress.right = (image!!.width / 2).toFloat() + (progressWidth / 2)
        rectProgress.bottom = (image!!.height / 2).toFloat() + (progressWidth / 2)
    }

    private fun setupPlaceHolder() {
        imageCopy = Bitmap.createBitmap(image)
        val canvas = Canvas(imageCopy)
        canvas.drawARGB(128, 255, 255, 255)

        val imageSpan = ImageSpan(context, imageCopy)
        val builder = SpannableStringBuilder(text)

        val cursorPosition = selectionStart
        builder.replace(cursorPosition, selectionEnd, IMAGE_SPAN)
        builder.setSpan(imageSpan, cursorPosition + 1,
                cursorPosition + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text = builder
        setSelection(cursorPosition + IMAGE_SPAN.length)

        spanHashCode = imageSpan.hashCode()
    }

    /**
     * @param progress Upload progress between 0-100
     */
    private fun drawProgress(progress: Int) {
        if (image == null || image!!.isRecycled) {
            return
        }
        val imageProgress = Bitmap.createBitmap(imageCopy)
        val canvas = Canvas(imageProgress)
        val angle = progress * 360 / 100f
        canvas.drawArc(rectProgress, 270f, angle, false, paintProgress)
        replaceImage(imageProgress)
    }

    /**
     * @param bitmap Update progress or replace to original image when upload done
     */
    private fun replaceImage(bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        val spans = text.getSpans(0, length(), ImageSpan::class.java)
        if (spans.isEmpty()) {
            return
        }
        val span = ImageSpan(context, bitmap)
        val builder = SpannableStringBuilder(text)
        val cursorPosition = selectionStart
        spans.forEach {
            if (it.hashCode() == spanHashCode) {
                val st = text.getSpanStart(it)
                val en = text.getSpanEnd(it)
                builder.setSpan(span, st, en, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        text = builder
        setSelection(cursorPosition)
    }

    /**
     * Handle image delete and typing
     * Always let image occupied one line without any text
     */
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.isNullOrEmpty()) {
                return
            }
            val spans = text.getSpans(0, length() - 1, ImageSpan::class.java)
            if (spans.isEmpty()) {
                return
            }
            removeTextChangedListener(this)
            spans.forEach {
                val st = text.getSpanStart(it)
                val en = text.getSpanEnd(it)
                if (st < 0 || en < 0) {
                    return
                }
                if (before > 0 && start == en) {                // delete the line below image
                    text = text.delete(st, en)
                    setSelection(st)
                } else if (st > 1 && text[st - 1] != '\n') {    // type before the image
                    text = text.insert(st, "\n")
                    setSelection(st)
                } else if (text[en] != '\n') {                  // type after the image
                    text = text.insert(en, "\n")
                    setSelection(en + 2)
                }
            }
            addTextChangedListener(this)
        }
    }

}