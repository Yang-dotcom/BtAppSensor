package com.example.myapplication;


import java.util.Arrays;

public class ProcessedInput {
    Integer n;
    Integer[] sensor_ID;
    String str;
    String[] vet_sensors; //size is n+1
    String[] single_sensor_data; //size of each row is n, each cell having 3 elements [sensor, T, P]
    Float[] temp, pressure;


    public ProcessedInput(Integer n, String str){
        this.n = n;
        this.str = str;
    }

    // str to process: "$MEA	m5833	s37	P996.5796	T24.5816	s38	P996.4803	T24.6520	s39	P996.5444	T24.8601	s40	P996.4953	T24.4348	s41	P996.5275	T24.7779	s42	P996.2718	T24.6095"
    public void run(){
        // example vet_sensors = ["$MEA	m5833	", "37	P996.5796	T24.5816	", "38	P996.4803	T24.6520	", "39	P996.5444	T24.8601	", "40	P996.4953	T24.4348	","41	P996.5275	T24.7779	", "42	P996.2718	T24.6095"]
        vet_sensors = str.split("s");
        temp = new Float[n];
        pressure = new Float[n];
        sensor_ID = new Integer[n];
        for (int i=0; i<n; i++){
            //single_sensor_data = ["37", "P996.5796", "T24.5816"] example
            single_sensor_data = vet_sensors[i+1].split("\t");
            System.out.println(i+"  Thread 3: "+ Arrays.toString(single_sensor_data)+ Arrays.toString(vet_sensors));
            sensor_ID[i] = Integer.parseInt(single_sensor_data[0]);
            pressure[i] = Float.parseFloat(single_sensor_data[1].substring(1));
            temp[i] = Float.parseFloat(single_sensor_data[2].substring(1));
        }

        /*System.out.println("sensor"+Arrays.toString(sensor_ID));
        System.out.println("press"+Arrays.toString(pressure));
        System.out.println("temp"+Arrays.toString(temp));*/
    }


}
