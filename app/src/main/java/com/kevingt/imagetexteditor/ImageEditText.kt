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
import java.util.*

class ImageEditText @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyleAttr: Int = android.R.attr.editTextStyle)
    : AppCompatEditText(context, attrs, defStyleAttr) {

    companion object {
        private const val IMAGE_SPAN = "\n \n"
    }

    private lateinit var paintProgress: Paint

    private val progressStrokeWidth = 16f
    private val progressWidth = 160

    private val imageSpanHash = Stack<Int>()
    private val imageStack = Stack<Bitmap?>()
    private val imageCopyStack = Stack<Bitmap?>()
    private val rectProgress = Stack<RectF>()

    private var screenWidth = 0
    private var listener: Listener? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        DisplayMetrics().apply {
            (context as MainActivity).windowManager.defaultDisplay.getMetrics(this)
            screenWidth = this.widthPixels
        }
        initSetting()
    }

    override fun onDetachedFromWindow() {
        imageStack.forEach { it?.recycle() }
        imageStack.clear()
        imageCopyStack.forEach { it?.recycle() }
        imageCopyStack.clear()
        super.onDetachedFromWindow()
    }

    fun setupListener(listener: Listener) {
        this.listener = listener
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
                        imageStack.push(Bitmap.createBitmap(resource))
                        initUploadView()
                        setupPlaceHolder()
                    }
                })
    }

    /**
     * @param hashCode The HashCode of ImageSpan
     * @param progress Upload progress between 0-100
     */
    fun updateUploadProgress(hashCode: Int, progress: Int) {
        imageSpanHash.indexOf(hashCode).apply {
            if (progress >= 100) {
                replaceImage(hashCode, imageStack[this])
                imageStack.removeAt(this)
                imageCopyStack.removeAt(this)?.recycle()
                rectProgress.removeAt(this)
                imageSpanHash.remove(hashCode)
                return
            }
            if (progress >= 0) {
                drawProgress(hashCode, progress)
            }
        }
    }

    private fun initSetting() {
        paintProgress = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = progressStrokeWidth
            color = ContextCompat.getColor(context, R.color.colorAccent)
        }
        addTextChangedListener(textWatcher)
    }

    private fun initUploadView() {
        rectProgress.push(RectF().apply {
            left = (imageStack.peek()!!.width / 2).toFloat() - (progressWidth / 2)
            top = (imageStack.peek()!!.height / 2).toFloat() - (progressWidth / 2)
            right = left + progressWidth
            bottom = top + progressWidth
        })
    }

    private fun setupPlaceHolder() {
        Bitmap.createBitmap(imageStack.peek()).apply {
            imageCopyStack.push(this)
            Canvas(this).apply { drawARGB(128, 255, 255, 255) }
        }

        val imageSpan = ImageSpan(context, imageCopyStack.peek())
        val cursorPosition = selectionStart

        val builder = SpannableStringBuilder(text).apply {
            replace(cursorPosition, selectionEnd, IMAGE_SPAN)
            setSpan(imageSpan, cursorPosition + 1, cursorPosition + 2,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        text = builder
        setSelection(cursorPosition + IMAGE_SPAN.length)

        imageSpanHash.push(imageSpan.hashCode())
        listener?.onHashCodeGenerated(imageSpan.hashCode())
    }

    /**
     * @param hashCode The HashCode of ImageSpan
     * @param progress Upload progress between 0-100
     */
    private fun drawProgress(hashCode: Int, progress: Int) {
        imageSpanHash.indexOf(hashCode).apply {
            if (imageStack[this] == null || imageStack[this]!!.isRecycled) {
                return
            }
            val imageProgress = Bitmap.createBitmap(imageCopyStack[this])
            val canvas = Canvas(imageProgress)
            val angle = progress * 360 / 100f
            canvas.drawArc(rectProgress[this], 270f, angle, false, paintProgress)
            replaceImage(hashCode, imageProgress)
        }
    }

    /**
     * @param bitmap Update progress or replace to original image when upload done
     */
    private fun replaceImage(hashCode: Int, bitmap: Bitmap?) {
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
            if (it.hashCode() == hashCode) {
                val st = text.getSpanStart(it)
                val en = text.getSpanEnd(it)
                builder.setSpan(span, st, en, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        text = builder
        setSelection(cursorPosition)
    }

    /**
     * Using Listener to pass HashCode because imageSpan need to wait for bitmap generated
     * Upload image when you get hashCode to avoid unnecessary error
     */
    interface Listener {
        fun onHashCodeGenerated(hashCode: Int)
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