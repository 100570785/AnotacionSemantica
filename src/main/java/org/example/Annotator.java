package org.example;

import es.uc3m.miaa.utils.Entity;
import es.uc3m.miaa.utils.MyGATE;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase principal que lee URLs desde un fichero, extrae entidades con GATE
 * y genera un fichero RDF en formato Turtle (.ttl) enriquecido con
 * desambiguación de localizaciones mediante la API de Gemini.
 */
public class Annotator {

    public static void main(String[] args) {
        // Configurar el User-Agent para que las peticiones HTTP no sean bloqueadas por
        // los servidores
        System.setProperty("http.agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // Validar que se ha proporcionado al menos el fichero de entrada como argumento
        if (args.length == 0) {
            System.out.println("Debes ejecutar el programa con: java Annotator <fichero> [-C <clase>]");
            return;
        }

        // Leer el nombre del fichero de entrada y la clase RDF (por defecto: webPage)
        String nombreArchivo = args[0];
        String clase = "urn:uc3m.es:miaa#webPage";
        // Si se especifica la opción -C, se usa la clase proporcionada por el usuario
        if (args.length == 3 && args[1].equals("-C")) {
            clase = args[2];
        }

        // Generar el nombre del fichero de salida sustituyendo la extensión por .ttl
        String nombreArchivoSalida = nombreArchivo.contains(".")
                ? nombreArchivo.substring(0, nombreArchivo.lastIndexOf('.')) + ".ttl"
                : nombreArchivo + ".ttl";

        System.out.println("Leyendo " + nombreArchivo + " usando la clase: " + clase);

        // Inicializar el lector de fichero y el motor GATE para la extracción de
        // entidades
        MyFileReader fileReader = new MyFileReader(nombreArchivo);
        MyGATE gate = MyGATE.getInstance();

        // Inicializar el cliente de Gemini
        Client client = new Client("Introduce aqui tu clave"); // <--- Introduce tu clave aqui

        try (PrintWriter writer = new PrintWriter(new FileWriter(nombreArchivoSalida))) {
            // Iniciar el archivo turtle salida
            writer.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
            writer.println("@prefix miaa: <urn:uc3m.es:miaa#> .");
            writer.println("@prefix dcterms: <http://purl.org/dc/terms/> .");
            writer.println();

            // Contador para generar identificadores únicos de nodos blancos (_:ent1,
            // _:ent2, ...)
            int contadorEntidades = 1;

            // Recorrer cada URL del fichero de entrada
            while (fileReader.hasNextLine()) {
                try {
                    String urlString = fileReader.getLine();
                    URL url = new URL(urlString);
                    System.out.println("\nProcesando fichero: " + url);
                    writer.println("<" + urlString + "> a <" + clase + "> .");

                    // Eliminar duplicados usando Set
                    Set<Entity> resultados = new HashSet<>(gate.findEntities(url));
                    if (!resultados.isEmpty()) {
                        // Extraemos el contexto: unimos todas las entidades detectadas en un solo texto
                        String contexto = resultados.stream().map(Entity::getText).collect(Collectors.joining(", "));

                        for (Entity e : resultados) {
                            // Escapar comillas y saltos de línea para que el texto sea válido en Turtle
                            String textoLimpio = e.getText().replace("\"", "\\\"").replace("\n", " ");
                            // Crear un identificador único para el nodo blanco de esta entidad
                            String nodoEntidad = "_:ent" + contadorEntidades++;

                            // Escribir las tripletas RDF que relacionan la URL con la entidad encontrada
                            writer.println("<" + urlString + "> miaa:mentionsEntity " + nodoEntidad + " .");
                            writer.println(nodoEntidad + " a miaa:" + e.getType() + " ;");
                            writer.println("         miaa:name \"" + textoLimpio + "\" .");

                            System.out.println(" - " + e.getText() + " -> " + e.getType());

                            // --- CONEXIÓN CON GEMINI PARA DESAMBIGUAR LOCALIZACIONES ---
                            if (e.getType().equals("Location")) {
                                String query = "Location: " + e.getText() + "\nContext: " + contexto;
                                try {
                                    System.out.print(
                                            "   [Consultando a Gemini para desambiguar '" + e.getText() + "'...] ");
                                    String wikipediaUrl = client.queryConConfig(query).trim();

                                    // Si la respuesta es una URL válida de Wikipedia, añadir las tripletas de
                                    // instancia
                                    if (wikipediaUrl.startsWith("https://en.wikipedia.org/")) {
                                        writer.println(
                                                "<" + urlString + "> miaa:mentionsInstance <" + wikipediaUrl + "> .");
                                        writer.println("<" + wikipediaUrl + "> a dcterms:Location .");
                                        System.out.println("ÉXITO: " + wikipediaUrl);
                                    } else {
                                        System.out.println("FALLO (Respuesta no válida: " + wikipediaUrl + ")");
                                    }
                                    // Pausa obligatoria de 12 segundos para no superar las 5 consultas/minuto de la
                                    // API gratuita
                                    Thread.sleep(12500);
                                } catch (Exception ex) {
                                    System.out.println("ERROR DE API: " + ex.getMessage());
                                }
                            }
                        }
                    } else {
                        System.out.println(" - [Aviso] No se pudieron extraer entidades.");
                    }
                } catch (MalformedURLException e) {
                    System.out.println("Error en la URL: " + e.getMessage());
                }
            }
            System.out.println("\n¡Éxito! Fichero " + nombreArchivoSalida + " generado y enriquecido correctamente.");
        } catch (IOException e) {
            System.out.println("Error al crear el archivo de salida: " + e.getMessage());
        }
    }
}