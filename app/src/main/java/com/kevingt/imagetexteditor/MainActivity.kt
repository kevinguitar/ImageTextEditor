package com.kevingt.imagetexteditor

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), ImageEditText.Listener {

    companion object {
        private const val REQ_CHOOSE_IMAGE = 1
    }

    private val r = Random()

    //Use Stack to store multiple image state
    private val imageHashStack = Stack<Int>()
    private val progressStack = Stack<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et_main.setupListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CHOOSE_IMAGE && data != null) {
            et_main.insertImage(data.data)
        }
    }

    fun chooseImage(v: View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQ_CHOOSE_IMAGE)
    }

    fun takePhoto(v: View) {
        if (imageHashStack.size == 0) {
            return
        }
        r.nextInt(progressStack.size).apply {
            progressStack[this] += 10 + r.nextInt(20)
            et_main.updateUploadProgress(imageHashStack[this], progressStack[this])
            if (progressStack[this] >= 100) {
                imageHashStack.removeAt(this)
                progressStack.removeAt(this)
            }
        }
    }

    override fun onHashCodeGenerated(hashCode: Int) {
        imageHashStack.push(hashCode)
        progressStack.push(0)
        //TODO: upload image and update progress
    }
}
