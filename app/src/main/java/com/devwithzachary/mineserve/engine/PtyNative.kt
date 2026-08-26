package com.devwithzachary.mineserve.engine

import android.os.ParcelFileDescriptor
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

class PtyProcess(
    val masterFdInt: Int,
    val pid: Int
) {
    val parcelFd: ParcelFileDescriptor = ParcelFileDescriptor.adoptFd(masterFdInt)
    val inputStream: FileInputStream = FileInputStream(parcelFd.fileDescriptor)
    val outputStream: FileOutputStream = FileOutputStream(parcelFd.fileDescriptor)

    fun updateWindowSize(cols: Int, rows: Int) {
        if (masterFdInt >= 0) {
            PtyNative.setPtyWindowSize(masterFdInt, cols, rows)
        }
    }

    fun destroy() {
        try {
            outputStream.close()
        } catch (_: Exception) {}
        try {
            inputStream.close()
        } catch (_: Exception) {}
        try {
            parcelFd.close()
        } catch (_: Exception) {}
        PtyNative.closeFd(masterFdInt)
    }
}

object PtyNative {
    init {
        try {
            System.loadLibrary("pty")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("PtyNative", "Failed to load pty native library", e)
        }
    }

    external fun createSubprocess(
        cmdPath: String,
        args: Array<String>,
        env: Array<String>?,
        cwdPath: String?,
        cols: Int,
        rows: Int,
        outPid: IntArray
    ): Int

    external fun setPtyWindowSize(masterFd: Int, cols: Int, rows: Int)

    external fun closeFd(masterFd: Int)

    external fun waitForProcess(pid: Int): Int
}
