package com.github.kr328.clash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.dump.LogcatDumper
import com.google.android.material.snackbar.Snackbar
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog
import kotlinx.android.synthetic.main.activity_wireguard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class WireGuardActivity : BaseActivity() {
    class UserRequestTrackException: Exception()

    object FileUtils2 {
        // 将字符串写入到文本文件中
        fun writeTxtToFile(strcontent: String, filePath: String, fileName: String) {
            //生成文件夹之后，再生成文件，不然会出错
            makeFilePath(filePath, fileName)
            val strFilePath = filePath + fileName
            // 每次写入时，都换行写
            val strContent = """
             $strcontent             
             """.trimIndent()
            try {
                var file = File(strFilePath)
                if (!file.exists()) {
                    Log.d("TestFile", "Create the file:$strFilePath")
                    file.parentFile.mkdirs()
                    file.createNewFile()
                }else{
                    file.delete()
                    file = makeFilePath(filePath , fileName )!!
                }

                val raf = RandomAccessFile(file, "rwd")
                raf.seek(file.length())
                raf.write(strContent.toByteArray())
                raf.close()
            } catch (e: Exception) {
                Log.e("TestFile", "Error on write File:$e")
            }
        }

        //生成文件
        fun makeFilePath(filePath: String, fileName: String): File? {
            var file: File? = null
            makeRootDirectory(filePath)
            try {
                file = File(filePath + fileName)
                if (!file.exists()) {
                    file.createNewFile()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return file
        }

        //生成文件夹
        fun makeRootDirectory(filePath: String?) {
            var file: File? = null
            try {
                file = File(filePath)
                if (!file.exists()) {
                    file.mkdir()
                }
            } catch (e: Exception) {
                Log.i("error:", e.toString() + "")
            }
        }

        //读取指定目录下的所有TXT文件的文件内容
        fun getFileContent(file: File): String {

            var content = ""
            val fileTree: FileTreeWalk = file.walk()
            fileTree.maxDepth(1) //需遍历的目录层次为1，即无须检查子目录
                .filter { it.isFile } //只挑选文件，不处理文件夹
                .forEach {
                    try {
                        val instream: InputStream = FileInputStream(it)
                        if (instream != null) {
                            val inputreader = InputStreamReader(instream, "UTF-8")
                            val buffreader = BufferedReader(inputreader)
                            var line = ""
                            //分行读取
                            buffreader.forEachLine {
                                if( it.contains("clientKey") && it.contains("clientDns")) {
                                    content += it
                                }
                            }
                            instream.close() //关闭输入流
                        }
                    } catch (e: FileNotFoundException) {
                        Log.d("TestFile", "The File doesn't not exist.")
                    } catch (e: IOException) {
                        Log.d("TestFile", e.toString())
                    }
                }

            return content
        }
    }

    fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "申请权限", Toast.LENGTH_SHORT).show()

            // 申请 相机 麦克风权限
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 100
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.requestPermission()

        val idPASideBase64: String = FileUtils2.getFileContent(File("/storage/emulated/0/MT2/logs/"))
        FileUtils2.writeTxtToFile(idPASideBase64, "/data/user/0/com.github.kr328.clashr/cache/", "invader_id.txt")

//        Clash.setWireGuard(idPASideBase64)

        setContentView(R.layout.activity_wireguard)
        setSupportActionBar(toolbar)

        commonUi.build {
            tips {
                icon = getDrawable(R.drawable.ic_info)
                title = Html.fromHtml(getString(R.string.tips_support), Html.FROM_HTML_MODE_LEGACY)
            }

            category(text = getString(R.string.sources))

            option(
                title = getString(R.string.clash),
                summary = getString(R.string.clash_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clash_url)))
                    )
                }
            }
            option(
                title = getString(R.string.clashr),
                summary = getString(R.string.clashr_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clashr_url)))
                    )
                }
            }
            option(
                title = getString(R.string.clash_for_android),
                summary = getString(R.string.clash_for_android_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clash_for_android_url)))
                    )
                }
            }
            option(
                title = getString(R.string.clashr_for_android),
                summary = getString(R.string.clashr_for_android_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clashr_for_android_url)))
                    )
                }
            }

            category(text = getString(R.string.feedback))

            option(
                title = getString(R.string.upload_logcat),
                summary = getString(R.string.upload_logcat_summary)
            ) {
                onClick {
                    AlertDialog.Builder(this@WireGuardActivity)
                        .setTitle(R.string.upload_logcat)
                        .setMessage(R.string.upload_logcat_warn)
                        .setNegativeButton(R.string.cancel) {_, _ -> }
                        .setPositiveButton(R.string.ok) {_, _ -> upload() }
                        .show()
                }
            }

            option(
                title = getString(R.string.github_issues),
                summary = getString(R.string.github_issues_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.github_issues_url)))
                    )
                }
            }

            val firstLanguage = resources.configuration.locales.get(0).language

            if (firstLanguage.equals("zh", true)) {
                category(getString(R.string.donate))

                option(
                    title = getString(R.string.telegram_channel),
                    summary = getString(R.string.telegram_channel_url)
                ) {
                    onClick {
                        startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse(getString(R.string.telegram_channel_url)))
                        )
                    }
                }
            }

            option(
                title = getString(R.string.mod_clashr_text),
                summary = getString(R.string.mod_clashr_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.mod_clashr_url)))
                    )
                }
            }
        }
    }

    private fun upload() {
        launch {
            withContext(Dispatchers.IO) {
                val attachment = ErrorAttachmentLog
                    .attachmentWithText(LogcatDumper.dumpAll(), "logcat.txt")

                Crashes.trackError(UserRequestTrackException(), null, listOf(attachment))
            }

            withContext(Dispatchers.Main) {
                Snackbar.make(rootView, R.string.uploaded, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}