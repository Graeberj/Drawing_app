package yosh.projects.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null
    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                val imageBackground: ImageView = findViewById(R.id.backgroundIv)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted){
                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                } else{
                    when {
                        permissionName != Manifest.permission.READ_EXTERNAL_STORAGE -> {
                        }
                    }
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setBrushSize(5f)

        val paintColorsLayout = findViewById<LinearLayout>(R.id.paint_color_layout)

        mImageButtonCurrentPaint = paintColorsLayout[0] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        val brushBtn: ImageButton = findViewById(R.id.ib_brush)
        brushBtn.setOnClickListener{
            showBrushSizeDialog()
        }
        val galleryBtn: ImageButton = findViewById(R.id.ib_gallery)
        galleryBtn.setOnClickListener {
            requestStoragePermission()
        }
        val undoBtn: ImageButton = findViewById(R.id.ib_undo)
        undoBtn.setOnClickListener {
            drawingView?.onClickUndo()
        }
        val saveBtn: ImageButton = findViewById(R.id.ib_save)
        saveBtn.setOnClickListener {
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.drawingViewContainer)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }

        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )){
            showRationaleDialog("Drawing App", "Drawing App" +
                    "needs to access your Storage")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.diologue_brush_size)
        brushDialog.setTitle("Brush size: ")
        brushDialog.show()
        val xSmallBtn: ImageButton = brushDialog.findViewById(R.id.ib_xSmall_brush)//5
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)//10
        val smallMedBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_medium_brush)//15
        val medBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)//20
        val medLgBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_large_brush)//25
        val lgBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)//30
        val xLgBtn: ImageButton = brushDialog.findViewById(R.id.ib_xLarge_brush)//35
        val xxLgBtn: ImageButton = brushDialog.findViewById(R.id.ib_xxLarge_brush)//40
        xSmallBtn.setOnClickListener{
            drawingView?.setBrushSize(5f)
            brushDialog.dismiss()
        }
        smallBtn.setOnClickListener{
            drawingView?.setBrushSize(10f)
            brushDialog.dismiss()
        }
        smallMedBtn.setOnClickListener{
            drawingView?.setBrushSize(15f)
            brushDialog.dismiss()
        }
        medBtn.setOnClickListener{
            drawingView?.setBrushSize(20f)
            brushDialog.dismiss()
        }
        medLgBtn.setOnClickListener{
            drawingView?.setBrushSize(25f)
            brushDialog.dismiss()
        }
        lgBtn.setOnClickListener{
            drawingView?.setBrushSize(30f)
            brushDialog.dismiss()
        }
        xLgBtn.setOnClickListener{
            drawingView?.setBrushSize(35f)
            brushDialog.dismiss()
        }
        xxLgBtn.setOnClickListener{
            drawingView?.setBrushSize(40f)
            brushDialog.dismiss()
        }
    }

    fun paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view


        }
    }
    private fun showRationaleDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel"){dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable !=null){
            bgDrawable.draw(canvas)
        } else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "drawingApp_"
                            + System.currentTimeMillis() /1000
                            + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread{
                        cancelProgressDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(
                                this@MainActivity,
                                "file saved successfully: $result",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else{
                            Toast.makeText(
                                this@MainActivity,
                                "something went wrong while saving file.",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)

                        }
                    }
                } catch (e: Exception){
                    result = ""
                    e.printStackTrace()

                }
            }
        }
        return  result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))

        }
    }
}