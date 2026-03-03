package org.example;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

public class Client {
    private final com.google.genai.Client client;
    private final String model = "gemini-2.5-flash"; // Hay que decidir que modelo usar

    public Client(String API_KEY) {
        client = com.google.genai.Client.builder().apiKey(API_KEY).build();
    }

    public String queryConConfig(String location) {
        // Este texto igual hay que cambiarlo que lo evalua
        String query = "I am going to give you a location name and some context from a document. " +
                "Please answer ONLY with the URL of the English version of Wikipedia (starting with https://en.wikipedia.org/wiki/) "
                + "that most likely identifies that location. Do not include any other text, markdown, or explanation.";
        GenerateContentConfig config =
                GenerateContentConfig.builder().systemInstruction(
                        Content.fromParts(Part.fromText(query))).build();

        GenerateContentResponse response = client.models.generateContent(model, location, config);
        return response.text();
    }
}
