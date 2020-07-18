package com.example.niodemo.server

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.charset.Charset


class NIOServer : Service() {
    private val TAG = "NIOServer"
    private var serverSocketChannel: ServerSocketChannel? = null
    private var selector: Selector? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate-----------")
        Thread(Runnable {
            initNio()
        }).start()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy-----------")
        serverSocketChannel?.close()
        selector?.close()
    }

    private fun initNio(){
        //创建selector
        selector  = Selector.open()
        //创建serverSocketChannel
        serverSocketChannel  = ServerSocketChannel.open()
        //绑定端口
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serverSocketChannel?.bind(InetSocketAddress(8000))
        }
        //设置非阻塞式
        serverSocketChannel?.configureBlocking(false)
        //将channel注册到selector中
        serverSocketChannel?.register(selector, SelectionKey.OP_ACCEPT)
        Log.e(TAG, "initNio---启动serverSocket成功，开始监听----")

        Log.e(TAG, "initNio---启动广播，通知client connect----")
        val intent = Intent("SERVER_START_SUCCESS")
        sendBroadcast(intent)

        watching()

   }

    private fun watching(){
        Log.e(TAG, "watching---start----")
        while (true) {
            Log.e(TAG, "watching---while loop")
            val readyChannels = selector!!.select() //等待所注册的事件发生
            if (0 == readyChannels) {
                Log.e(TAG, "watching---1----")
                continue
            }
            //通过selectedKeys() 拿到 selectedKeys 集合
            Log.e(TAG, "watching---2----")
            val selectionKeys : Set<SelectionKey> = selector!!.selectedKeys()
            val iterator: Iterator<SelectionKey> = selectionKeys.iterator()
            Log.e(TAG, "watching---3----${selectionKeys.size}")
            while (iterator.hasNext()) {
                //拿到selectKey实例
                val selectKey = iterator.next()
                Log.e(TAG, "watching---4----")
                //如果是接入事件
                if (selectKey.isAcceptable) {
                    Log.e(TAG, "initNio---handleAccept----")
                    handleAccept(serverSocketChannel!!, selector!!)
                }
                //如果是可读事件
                if (selectKey.isReadable) {
                    Log.e(TAG, "initNio---handleRead----")
                    handleRead(selectKey, selector!!)
                }
            }
            Thread.sleep(1000)
        }
    }

    private fun handleAccept(serverSocketChannel: ServerSocketChannel?, selector: Selector){
        Log.e(TAG, "handleAccept---start----")
        //拿到socketChannel客户端
        val socketChannel: SocketChannel? = serverSocketChannel?.accept()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e(TAG, "handleAccept----新客户端连接： ${socketChannel?.remoteAddress}")
        }
        //设置socketChannel为非阻塞模式
        socketChannel?.configureBlocking(false)
        Log.e(TAG, "handleAccept---1----")
        //客户端注册读事件，这样我们才能接收到客户端消息
        socketChannel?.register(selector, SelectionKey.OP_READ)
        //发送connected 提示服务的已经接收到
        val buf: ByteBuffer = Charset.forName("utf-8").encode("服务连接成功")
        socketChannel?.write(buf)
        Log.e(TAG, "handleAccept---end----")
    }

    private fun handleRead(selectionKey: SelectionKey, selector: Selector){
        Log.e(TAG, "handleRead---start----")
        //拿到已经就绪的channel
        val channel: SocketChannel? = selectionKey.channel() as SocketChannel
        if (channel != null) {
            //读取channel的数据
            val buf: ByteBuffer = ByteBuffer.allocate(512)
            val sb: StringBuilder = StringBuilder()
            var readByte: Int = channel.read(buf)
            while (readByte > 0) {
                //切换为读模式
                buf.flip()
                val msg: String = Charset.forName("utf-8").decode(buf).toString()
                sb.append(msg)
                readByte = channel.read(buf)
            }
            buf.clear()
            //将channel继续注册为可读事件
            channel.register(selector, SelectionKey.OP_READ)
            if (sb.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.e(TAG, "handleRead---${channel.remoteAddress}---$sb")
                }
                broadcastMsg(selector, channel, sb.toString())
            }
        }
        Log.e(TAG, "handleRead---end----")
    }

    private fun broadcastMsg(selector: Selector, targetChannel: SocketChannel?, msg: String){
        //拿到已连接的客户端数
        val keys: Set<SelectionKey> = selector.keys()
        keys.forEach {
            val channel: Channel = it.channel()
            if (channel is SocketChannel) {
                channel.write(Charset.forName("utf-8").encode(msg))
            }
        }
        Log.e(TAG, "broadcastMsg---end----")
    }

}