package com.example.myapplication;

import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;

public class Reader implements Runnable {
    private final BluetoothSocket socket;
    public Boolean stopThread;
    private final LinkedBlockingQueue<String> queue;
    InputStream inputStream;

    public Reader(LinkedBlockingQueue<String> queue1, Boolean stopThread1, InputStream inputStream, BluetoothSocket socket){
        this.socket = socket;
        this.queue = queue1;
        this.stopThread = stopThread1;
        this.inputStream = inputStream;

    }

    @Override
    public void run(){
        // create a BufferedReader that gives us more convenient methods to use onto the imputstream
        BufferedReader btInputStream = new BufferedReader(new InputStreamReader(inputStream));
        while(!Thread.currentThread().isInterrupted() && !stopThread){
            try {
                //check if the inputstream has available data incoming
                Boolean byteCount = btInputStream.ready();
                if(byteCount){
                    String string = btInputStream.readLine();
                    boolean isFound = string.contains("$MEA");
                    if (string.length() > 25 && isFound){
                        // put the reading (a string ending in \n in this case) onto a blocking queue
                        //a blockingqueue is an array that is shared between threads
                        //if the blockingqueue is at full capacity, the put operation is blocked and a slot is freed
                        queue.put(string);
                        System.out.println("thread1 working" + byteCount);
                    }
                }else{
                    System.out.println("thread1 not working"+ byteCount);
                }


            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                stopThread =true;
                try {
                    //close inputstream and btsocket
                    inputStream.close();
                    socket.close();
                } catch (IOException i) {
                    i.printStackTrace();
                }
            }
        }
    }

}
