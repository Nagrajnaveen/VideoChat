package com.app.videochat

import MyGLSurfaceView
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.AgoraVideoFrame


class OpenGLES20Activity : AppCompatActivity() {
    private var  projectionManager : MediaProjectionManager? = null
    private val PROJECTION_REQ_CODE = 1 shl 2
    private lateinit var mVirtualDisplay: VirtualDisplay
    val VIRTUAL_DISPLAY_NAME = "Screen Share"
    val FLAG_SCREEN_WIDTH = 640
    val FLAG_SCREEN_HEIGHT = 480
    val FLAG_SCREEN_DPI = 3
    val FLAG_FRAME_RATE = 15
    val TYPE_LOCAL_VIDEO = 1
    val TYPE_SCREEN_SHARE = 2
    val TYPE_AR_CORE = 3
    private lateinit var gLView: GLSurfaceView
    var mSurface: Surface?=null
    lateinit var mSurfaceView:MyGLSurfaceView


    private lateinit var mRtcEngine: RtcEngine

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gLView = MyGLSurfaceView(this)
        setContentView(R.layout.activity_open_g_l_e_s20)
        initializeAgoraEngine()
//        mSurfaceView=findViewById(R.id.surfaceView)
//        mSurfaceView=findViewById(R.id.surfaceView)
//        mSurface=gLView.holder.surface
//        joinChannel()
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager!!.createScreenCaptureIntent(), PROJECTION_REQ_CODE)

    }

    var mMediaProjection:MediaProjection?=null
    private val LOG_TAG = OpenGLES20Activity::class.java.simpleName

    private val mRtcEventHandler = object:IRtcEngineEventHandler(){

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            runOnUiThread { setupRemoteVideo(uid) }
        }


    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setupRemoteVideo(uid: Int) {
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.
        val container = findViewById(R.id.remote_video_view_container) as FrameLayout

        if (container.childCount >= 1) {
            return
        }

        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        container.addView(surfaceView)

        setupVirtualDisplay()

    }


    private fun initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
        } catch (e: Exception) {
            Log.e(LOG_TAG, Log.getStackTraceString(e))

            throw RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e))
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setupVirtualDisplay(
        callback: VirtualDisplay.Callback
    ) {
        var w: Int = FLAG_SCREEN_WIDTH
        var h: Int = FLAG_SCREEN_HEIGHT
        val wm =
            applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getMetrics(dm)
        if (dm.widthPixels > dm.heightPixels) {
            if (w < h) {
                w = FLAG_SCREEN_HEIGHT
                h = FLAG_SCREEN_WIDTH
            }
        } else {
            if (w > h) {
                w = FLAG_SCREEN_HEIGHT
                h = FLAG_SCREEN_WIDTH
            }
        }
        val mImageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 4)
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "Android Host Screen",
            w,
            h,
            FLAG_SCREEN_DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.getSurface(), callback, null
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

      mMediaProjection=projectionManager!!.getMediaProjection(resultCode, data!!) as MediaProjection

        setupVirtualDisplay()

        mRtcEngine.pushExternalVideoFrame(AgoraVideoFrame())
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setupVirtualDisplay() {
        setupVirtualDisplay( @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        object : VirtualDisplay.Callback() {
            override fun onPaused() {}
            override fun onResumed() {
                Log.i("Virtual Display","onResume...")
            }
            override fun onStopped() {
                Log.i("Virtual Display","onStop...")
            }
        })
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onBackPressed() {
        mMediaProjection!!.stop()
        mVirtualDisplay.release()
        super.onBackPressed()
    }
}
