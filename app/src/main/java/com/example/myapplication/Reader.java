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
    private Object InterruptedException;

    public Reader(LinkedBlockingQueue<String> queue1, InputStream inputStream, BluetoothSocket socket){
        this.queue = queue1;
        this.inputStream = inputStream;
        this.socket = socket;

    }

    @Override
    public void run(){
        // create a BufferedReader that gives us more convenient methods to use onto the inputstream
        BufferedReader btInputStream = new BufferedReader(new InputStreamReader(inputStream));
        stopThread = false;
        while(!Thread.currentThread().isInterrupted() && !stopThread){
            try {
                //check if the inputstream has available data incoming
                int byteCount = inputStream.available();
                //check if buffer of inputStream is not too full; then proceed to read a line (till \n)
                //and put it on the blockingqueue shared with main
                if(byteCount>0 && byteCount<2000){
                    String string = btInputStream.readLine();
                    boolean isFound = string.contains("$MEA") != string.contains("ERR"); //&& !string.contains("ERR");
                    if (string.length() > 30 && isFound){
                        // put the reading (a string ending in \n in this case) onto a blocking queue
                        //a blockingqueue is an array that is shared between threads
                        //if the blockingqueue is at full capacity, the put operation is blocked and a slot is freed
                        queue.put(string);
                        //System.out.println("thread1 working" + byteCount);
                    }else {
                        //System.out.println("thread1 not working"+ byteCount);
                    }
            }// If buffer is too full, "eat" further incoming data in line - chunks to avoid EOF due to size of buffer too large
                else if (byteCount > 2000) {
                    btInputStream.readLine();
            }
        } catch (java.lang.InterruptedException | IOException e) {
                e.printStackTrace();
                stopThread =true;
                try {
                    socket.close();
                    inputStream.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

}
}
