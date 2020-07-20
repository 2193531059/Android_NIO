package com.example.niodemo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class MyReadThread extends Thread {
    private static final String TAG = "MyReadThread";
    Selector selector;
    private Context context;

    public MyReadThread(Selector selector, Context context) {

        this.selector = selector;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            //处理事件
            while(true) {
                int readyChannels = selector.select(); //等待所注册的事件发生
                if(0 == readyChannels) {
                    continue;
                }
                //处理事件
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    if(selectionKey.isReadable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(128);
                        StringBuffer stringBuffer = new StringBuffer();
                        while(channel.read(buffer) > 0) {
                            buffer.flip();
                            stringBuffer .append(Charset.forName("UTF-8").decode(buffer));
                            buffer.clear();
                        }
                        Intent intent = new Intent("GET_SERVER_CONTENT");
                        intent.putExtra("content", stringBuffer.toString());
                        context.sendBroadcast(intent);
//                        Log.e(TAG, "run: ----" + stringBuffer.toString());
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
