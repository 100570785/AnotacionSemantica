package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class MyFileReader {
    private Scanner myReader;

    public MyFileReader(String fileName) {
        File file = new File(fileName);
        System.out.println("Leyendo " + fileName);
        try {
            myReader = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public String getLine() {
        return myReader.nextLine();
    }

    public boolean hasNextLine() {
        return myReader.hasNextLine();
    }
}