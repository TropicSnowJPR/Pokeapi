package com.pokeapi.papi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class PokeApiService {

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

            return Optional.of(response.body());

        } catch (Exception e) {

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

            return response.body();

        } catch (Exception e) {

            return "{\"error\":\"Could not fetch data\"}";

        }

    }

}
