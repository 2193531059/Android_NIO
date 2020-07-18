package com.example.niodemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.niodemo.server.NIOServer
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
    private val TAG = "NIOClient"
    private var mBroadcastReceiver: NIOServerStartBroadCast? = null

    private var socketChannel: SocketChannel? = null
    private var selector: Selector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = NIOServerStartBroadCast()
            val filter = IntentFilter()
            filter.addAction("SERVER_START_SUCCESS")
            registerReceiver(mBroadcastReceiver, filter)
        }

        startServer()
    }

    private fun startServer(){
        val intent = Intent(this, NIOServer::class.java)
        startService(intent)
    }

    private fun initNio(){
        //创建selector
        selector = Selector.open()
        //创建socketChannel
        socketChannel = SocketChannel.open(InetSocketAddress("localhost", 8000))
        //设置为非阻塞方式
        socketChannel?.configureBlocking(false)
//        //连接服务器
//        socketChannel.connect()
        //注册读事件，读取服务端消息
        socketChannel?.register(selector, SelectionKey.OP_READ)
        Log.e(TAG, "initNio---启动NIOClient成功，开始监听----")

        MyReadThread(selector).start()

        if (socketChannel!!.finishConnect()) {
            Log.e(TAG, "initNio---socketChannel结束连接，开始监听----")
            if (socketChannel!!.isConnected) {
                Log.e(TAG, "initNio---socketChannel结束连接，发送数据----")
                socketChannel?.write(Charset.forName("utf-8").encode("您好啊啊啊啊啊啊啊啊啊啊啊啊啊啊"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver)
        }
        socketChannel?.close()
        selector?.close()
    }

    inner class NIOServerStartBroadCast: BroadcastReceiver(){
        private val TAG = "NIOServerStartBroadCast"
        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.e(TAG, "action = ${p1?.action}")
            if (p1?.action == "SERVER_START_SUCCESS") {
                Thread(Runnable { initNio() }).start()
            }
        }
    }
}
