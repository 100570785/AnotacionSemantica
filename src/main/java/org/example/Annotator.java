package org.example;

import es.uc3m.miaa.utils.Entity;
import es.uc3m.miaa.utils.MyGATE;

// Importaciones necesarias para la API de Gemini
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class Annotator {

    // --- IMPORTANTE: Pega tu API Key de Google aquí ---
    private static final String API_KEY = "PEGAR API KEY DE GEMINI AQUÍ";

    public static void main(String[] args) {

        System.setProperty("http.agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        if (args.length == 0) {
            System.out.println("Debes ejecutar el programa con: java Annotator <fichero> [-C <clase>]");
            return;
        }

        String nombreArchivo = args[0];
        String clase = "urn:uc3m.es:miaa#webPage";
        if (args.length == 3 && args[1].equals("-C")) {
            clase = args[2];
        }

        String nombreArchivoSalida = nombreArchivo.contains(".")
                ? nombreArchivo.substring(0, nombreArchivo.lastIndexOf('.')) + ".ttl"
                : nombreArchivo + ".ttl";

        System.out.println("Leyendo " + nombreArchivo + " usando la clase: " + clase);

        MyFileReader fileReader = new MyFileReader(nombreArchivo);
        MyGATE gate = MyGATE.getInstance();

        // 1. Inicializar el cliente de Gemini
        Client client = null;
        try {
            client = Client.builder().apiKey(API_KEY).build();
        } catch (Exception e) {
            System.out.println("No se pudo inicializar Gemini. Revisa tu API KEY.");
        }

        // 2. Configurar el comportamiento de Gemini (System Instruction)
        String systemInstruction = "I am going to give you a location name and some context from a document. " +
                "Please answer ONLY with the URL of the English version of Wikipedia (starting with https://en.wikipedia.org/wiki/) "
                +
                "that most likely identifies that location. Do not include any other text, markdown, or explanation.";

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                .build();

        try (PrintWriter writer = new PrintWriter(new FileWriter(nombreArchivoSalida))) {

            writer.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
            writer.println("@prefix miaa: <urn:uc3m.es:miaa#> .");
            writer.println("@prefix dcterms: <http://purl.org/dc/terms/> .");
            writer.println();

            int contadorEntidades = 1;

            while (fileReader.hasNextLine()) {
                try {
                    String urlString = fileReader.getLine();
                    URL url = new URL(urlString);
                    System.out.println("\nProcesando: " + url);

                    writer.println("<" + urlString + "> a <" + clase + "> .");

                    List<Entity> resultados = gate.findEntities(url);

                    if (resultados != null) {
                        // Extraemos el contexto: unimos todas las entidades detectadas en un solo texto
                        String contexto = resultados.stream().map(Entity::getText).collect(Collectors.joining(", "));

                        for (Entity e : resultados) {
                            String textoLimpio = e.getText().replace("\"", "\\\"").replace("\n", " ");
                            String nodoEntidad = "_:ent" + contadorEntidades++;

                            writer.println("<" + urlString + "> miaa:mentionsEntity " + nodoEntidad + " .");
                            writer.println(nodoEntidad + " a miaa:" + e.getType() + " ;");
                            writer.println("         miaa:name \"" + textoLimpio + "\" .");

                            System.out.println(" - " + e.getText() + " -> " + e.getType());

                            // --- CONEXIÓN CON GEMINI PARA DESAMBIGUAR LOCALIZACIONES ---
                            if (e.getType().equals("Location") && client != null) {
                                String query = "Location: " + e.getText() + "\nContext: " + contexto;

                                try {
                                    System.out.print(
                                            "   [Consultando a Gemini para desambiguar '" + e.getText() + "'...] ");

                                    // Usamos el modelo rápido (flash)
                                    GenerateContentResponse response = client.models.generateContent(
                                            "gemini-2.5-flash", query, config);

                                    String wikipediaUrl = response.text().trim();

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