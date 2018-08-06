package com.kevingt.imagetexteditor

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_CHOOSE_IMAGE = 1
    }

    private val r = Random()
    private var uploadProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CHOOSE_IMAGE && data != null) {
            uploadProgress = 0
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
        uploadProgress += 10 + r.nextInt(20)
        et_main.updateUploadProgress(uploadProgress)
    }
}
