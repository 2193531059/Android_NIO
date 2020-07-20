package com.example.niodemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.niodemo.server.NIOServer
import kotlinx.android.synthetic.main.activity_main.*
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "NIOClient"
    private var mBroadcastReceiver: NIOServerStartBroadCast? = null

    private var socketChannel: SocketChannel? = null
    private var selector: Selector? = null

    private var ip: String? = null
    private var com: String? = null
    private var sendContent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = NIOServerStartBroadCast()
            val filter = IntentFilter()
            filter.addAction("SERVER_START_SUCCESS")
            filter.addAction("GET_SERVER_CONTENT")
            registerReceiver(mBroadcastReceiver, filter)
        }
        //测试用服务器
        startServer()

        con_bt.setOnClickListener(this)
        send_bt.setOnClickListener(this)

    }

    private fun startServer(){
        val intent = Intent(this, NIOServer::class.java)
        startService(intent)
    }

    private fun initNio(){
        //创建selector
        selector = Selector.open()
        //创建socketChannel
//        socketChannel = SocketChannel.open(InetSocketAddress("localhost", 8000))
        try {
            socketChannel = SocketChannel.open(InetSocketAddress(ip, Integer.parseInt(com!!)))
        } catch (e: Exception) {
            Toast.makeText(this, "连接失败，请检查ip及端口是否正确！！", Toast.LENGTH_LONG).show()
        }


        //设置为非阻塞方式
        socketChannel?.configureBlocking(false)
//        //连接服务器
//        socketChannel.connect()
        //注册读事件，读取服务端消息
        socketChannel?.register(selector, SelectionKey.OP_READ)
        Log.e(TAG, "initNio---启动NIOClient成功，开始监听----")

        MyReadThread(selector, applicationContext).start()

//        if (socketChannel!!.finishConnect()) {
//            Log.e(TAG, "initNio---socketChannel结束连接，开始监听----")
//            if (socketChannel!!.isConnected) {
//                Log.e(TAG, "initNio---socketChannel结束连接，发送数据----")
//                socketChannel?.write(Charset.forName("utf-8").encode("您好啊啊啊啊啊啊啊啊啊啊啊啊啊啊"))
//            }
//        }
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
//                Thread(Runnable { initNio() }).start()
            } else if (p1?.action == "GET_SERVER_CONTENT") {
                val oldContent = accept_tv.text
                val getContentSB = StringBuilder(oldContent)
                val newContent = p1.getStringExtra("content")
                getContentSB.append("\n$newContent")
                accept_tv.text = getContentSB.toString()
            }
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id){
            R.id.con_bt -> {
                ip = ip_ev.text.toString()
                if (TextUtils.isEmpty(ip) || !isboolIp(ip)) {
                    Toast.makeText(this, "请输入正确的ip地址！！", Toast.LENGTH_LONG).show()
                    return
                }
                com = com_ev.text.toString()
                if (TextUtils.isEmpty(com) || !isNumeric(com)) {
                    Toast.makeText(this, "请输入正确的端口！！", Toast.LENGTH_LONG).show()
                    return
                }
                Thread(Runnable { initNio() }).start()
            }
            R.id.send_bt -> {
                sendContent = send_ev.text.toString()
                if (TextUtils.isEmpty(sendContent)){
                    Toast.makeText(this, "发送内容为空！！", Toast.LENGTH_LONG).show()
                    return
                }
                if (socketChannel == null) {
                    Toast.makeText(this, "连接失败，请检查ip及端口是否正确！！", Toast.LENGTH_LONG).show()
                    return
                }
                Thread(Runnable {
                    if (socketChannel!!.finishConnect()) {
                        if (socketChannel!!.isConnected) {
                            socketChannel?.write(Charset.forName("utf-8").encode(sendContent))
                        } else {
                            Toast.makeText(this, "连接失败，请点击连接重新连接！！", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "请等待连接成功！！", Toast.LENGTH_LONG).show()
                    }
                }).start()
            }
        }
    }

    fun isboolIp(ipAddress: String?): Boolean {
        val ip =
            "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}"
        val pattern: Pattern = Pattern.compile(ip)
        val matcher: Matcher = pattern.matcher(ipAddress)
        return matcher.matches()
    }

    fun isNumeric(str: String?): Boolean {
        val pattern = Pattern.compile("[0-9]*")
        return pattern.matcher(str).matches()
    }
}
