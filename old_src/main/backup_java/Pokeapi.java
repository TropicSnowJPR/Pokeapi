import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import io.github.cdimascio.dotenv.Dotenv;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;


public class Pokeapi {
    private static final List<String> team = new ArrayList<>();

    private static String env(String args) {
        Dotenv dotenv = Dotenv.load();
        try {
            return dotenv.get(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        InetSocketAddress addr = new InetSocketAddress(8081);

        // === Load keystore ===
        char[] passphrase = env("KeyStorePassword").toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream is = Pokeapi.class.getResourceAsStream("/src/main/backup_java/keystore/keystore.jks")) {
            ks.load(is, env("KeyStorePassword").toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        // === Create HTTPS server ===
        HttpsServer server = HttpsServer.create(addr, 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));

        Path webRoot = Path.of("H:/Pokeapi/src/main/resources/web");


        // === Websites ===
        server.createContext("/", new StaticFileHandler(webRoot));
        server.createContext("/login", new StaticFileHandler(webRoot));
        server.createContext("/signup", new StaticFileHandler(webRoot));

        // === APIs ===
        server.createContext("/team", new TeamListHandler());
        server.createContext("/team/add", new TeamAddHandler());
        server.createContext("/team/remove", new TeamRemoveHandler());

        server.createContext("/pokemon", new PokemonHandler());
        server.createContext("/pokemon/import", new PokemonImportHandler());
        server.createContext("/pokemon/export", new PokemonExportHandler());

        server.createContext("/moves", new MovesHandler());

        server.start();
        System.out.println("Serving web server on https://0.0.0.0:8081");
    }

    // generic file handler
    static class StaticFileHandler implements HttpHandler {
        private final Path root;

        StaticFileHandler(Path root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            Path filePath = switch (requestPath) {
                case "/" -> root.resolve("main.html");
                case "/login" -> root.resolve("login.xhtml");
                case "/signup" -> root.resolve("signup.xhtml");
                default ->
                    // fallback: try to serve static file directly
                        root.resolve("." + requestPath).normalize();
            };

            if (!Files.exists(filePath) || !filePath.startsWith(root)) {
                String msg = "404 Not Found";
                exchange.sendResponseHeaders(404, msg.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg.getBytes());
                }
                return;
            }

            String mime = Files.probeContentType(filePath);
            if (mime == null) mime = "application/octet-stream";

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class PokemonExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String json;
                synchronized (team) {
                    json = "{\"team\":[" +
                            String.join(",", team.stream().map(p -> "\"" + p + "\"").toList()) +
                            "]}";
                }
                byte[] respBytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respBytes.length);
                exchange.getResponseBody().write(respBytes);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class PokemonImportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery(); // e.g. "name=pikachu"
                String name = null;

                if (query != null && query.startsWith("name=")) {
                    name = query.substring(5).toLowerCase();
                }

                if (name == null || name.isEmpty()) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                synchronized (team) {
                    if (team.size() >= 6) {
                        String error = "{\"error\":\"Team already has 6 Pokémon\"}";
                        byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(400, bytes.length);
                        exchange.getResponseBody().write(bytes);
                        exchange.close();
                        return;
                    }
                    team.add(name);
                }

                String json = "{\"success\":true,\"added\":\"" + name + "\"}";
                byte[] respBytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respBytes.length);
                exchange.getResponseBody().write(respBytes);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class PokemonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery(); // e.g. "name=ditto" or "name=25"
                String name = null;


                if (query != null && query.startsWith("name=")) {
                    name = query.substring(5);
                }

                if (name == null || name.isEmpty()) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://pokeapi.co/api/v2/pokemon/" + name.toLowerCase()))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    byte[] respBytes = response.body().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(response.statusCode(), respBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(respBytes);
                    }
                } catch (Exception e) {
                    String error = "{\"error\":\"Could not fetch data\"}";
                    byte[] respBytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, respBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(respBytes);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class MovesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery(); // e.g. "name=ditto" or "name=25"
                String name = null;


                if (query != null && query.startsWith("name=")) {
                    name = query.substring(5);
                }

                if (name == null || name.isEmpty()) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://pokeapi.co/api/v2/move/" + name.toLowerCase()))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    byte[] respBytes = response.body().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(response.statusCode(), respBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(respBytes);
                    }
                } catch (Exception e) {
                    String error = "{\"error\":\"Could not fetch data\"}";
                    byte[] respBytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, respBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(respBytes);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class TeamAddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery(); // e.g. "name=pikachu"
                String name = null;

                if (query != null && query.startsWith("name=")) {
                    name = query.substring(5).toLowerCase();
                }

                if (name == null || name.isEmpty()) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                synchronized (team) {
                    if (team.size() >= 6) {
                        String error = "{\"error\":\"Team already has 6 Pokémon\"}";
                        byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(400, bytes.length);
                        exchange.getResponseBody().write(bytes);
                        exchange.close();
                        return;
                    }
                    team.add(name);
                }

                String json = "{\"success\":true,\"added\":\"" + name + "\"}";
                byte[] respBytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respBytes.length);
                exchange.getResponseBody().write(respBytes);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class TeamListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String json;
                synchronized (team) {
                    json = "{\"team\":[" +
                            String.join(",", team.stream().map(p -> "\"" + p + "\"").toList()) +
                            "]}";
                }
                byte[] respBytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respBytes.length);
                exchange.getResponseBody().write(respBytes);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class TeamRemoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery(); // e.g. "name=pikachu"
                String name = null;

                if (query != null && query.startsWith("name=")) {
                    name = query.substring(5).toLowerCase();
                }

                if (name == null || name.isEmpty()) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                boolean removed;
                synchronized (team) {
                    removed = team.remove(name);
                }

                String json;
                if (removed) {
                    json = "{\"success\":true,\"removed\":\"" + name + "\"}";
                } else {
                    json = "{\"error\":\"Pokémon not found in team\"}";
                }

                byte[] respBytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respBytes.length);
                exchange.getResponseBody().write(respBytes);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
