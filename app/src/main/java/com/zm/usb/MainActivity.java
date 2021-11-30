package com.zm.usb;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity {

    private SocketServerThread mServerThread06;
    private SocketServerThread mServerThread05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mServerThread06 = new SocketServerThread(28806);
        mServerThread06.setIsLoop(true);
        mServerThread06.start();

    }


    //调试数据线程 tcp
    class SocketServerThread extends Thread {
        private final int port;
        private BufferedOutputStream out;
        private Socket socket;
        public boolean isLoop = true;//服务线程的启动标识
        private ServerSocket serverSocket05;
        private BufferedInputStream in;

        //        private ServerSocket serverSocket06;
        public SocketServerThread(int port) {
            this.port = port;
        }
        @Override
        public void run() {
            try {
//                LogUtil.d("wsy", "等待连接");
                serverSocket05 = new ServerSocket(port);
                while (isLoop) {
                    socket = serverSocket05.accept();
                    out = new BufferedOutputStream(socket.getOutputStream());
                    // 开启子线程去读去数据
                    new Thread(new SocketReadThread(new BufferedInputStream(socket.getInputStream()))).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (serverSocket05 != null) {///
                    try {
                        serverSocket05.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //暴露给外部调用写入流的方法
        public void sendMsg(String msg) {
            try {
                if (out != null) {
                    out.write(msg.getBytes());
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void setIsLoop(boolean b) {
            this.isLoop = b;
        }

        class SocketReadThread implements Runnable {
            public SocketReadThread(BufferedInputStream inStream) throws IOException {
                in = inStream;
            }

            public void sendData(String rsg) {
                try {
                    out.write((rsg.getBytes()));
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public void run() {
                try {
                    String readMsg = "";
                    while (isLoop) {
                        if (!socket.isConnected()) {
                            break;
                        }
                        //   读到后台发送的消息  然后去处理
                        readMsg = readMsgFromSocket(in);
                        String finalReadMsg = readMsg;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"：接受的数据:" +finalReadMsg ,Toast.LENGTH_SHORT).show();//
                            }
                        });
                        if (port == 28805) {//发送二进制通道
                            sendData("通道：28805");
                        }
                        if (port == 28806) {//发送py通道
                            sendData("通道：28806");
                        }
                    }
                    in.close();
                    connectStop();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            //读取PC端发送过来的数据
            private String readMsgFromSocket(BufferedInputStream in) {
                String msg = "";
                BufferedReader bufferedReader = null;
                byte[] temp = new byte[1024 * 64];
                try {
                    int readedBytes = in.read(temp, 0, temp.length);
                    msg = new String(temp, 0, readedBytes);
                } catch (StringIndexOutOfBoundsException s) {
                    isLoop = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return msg;
            }
        }
        private void connectStop() {
            isLoop = false;
            try {
                if (socket != null) {
                    socket.close();
                }
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}