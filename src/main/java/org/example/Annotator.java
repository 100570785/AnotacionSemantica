package org.example;

import es.uc3m.miaa.utils.Entity;
import es.uc3m.miaa.utils.MyGATE;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class Annotator {
    public static void main(String[] args) {
        // Leer los argumentos del programa
        if (args.length == 0) {
            System.out.println("Debes ejecutar el programa con: java Annotator <fichero> [-C <clase>]");
        } else {
            String nombreArchivo = args[0];
            if (args.length == 3) { // file and class included
                String clase = args[2];
            } else {
                MyFileReader fileReader = new MyFileReader(nombreArchivo);
                MyGATE gate =  MyGATE.getInstance();
                List<Entity> resultados;
                while(fileReader.hasNextLine()){
                    try {
                        URL url = new URL(fileReader.getLine());
                        System.out.println(url);
                        resultados = gate.findEntities(url);
                        for (Entity e: resultados){
                            System.out.println(e.getText() + "->"  + e.getType());
                        }
                    } catch (MalformedURLException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }


//        Client client = new Client(Config.API_ANTONIO);
//        String response = client.querySinConfig("Tell me something about Paris");
//        System.out.println(response);
    }
}