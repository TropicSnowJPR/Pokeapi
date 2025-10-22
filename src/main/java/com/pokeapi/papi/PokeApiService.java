package com.pokeapi.papi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class PokeApiService {

    private static final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);

    public static Optional<String> getPokemon(String nameid) {

        if (nameid == null || nameid.isEmpty()) {
            return Optional.empty();
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://pokeapi.co/api/v2/pokemon/" + nameid.toLowerCase()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200)
                return Optional.empty();

            //System.out.println(response.body());
            return Optional.of(response.body());

        } catch (Exception e) {

            logger.error("Error while fetching the Pokemon data:\n{}", String.valueOf(e));
            return Optional.empty();

        }

    }



    public static String getMove(String moveid) {

        if (moveid == null || moveid.isEmpty()) {
            return "Invalid move or id";
        }

        try {

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://pokeapi.co/api/v2/move/" + moveid.toLowerCase()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            //System.out.println(response.body());
            return response.body();

        } catch (Exception e) {

            System.out.println("Error while fetching the Moves data:\n" + e);
            return "{\"error\":\"Could not fetch data\"}";

        }

    }

}
