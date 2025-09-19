package com.pokeapi.papi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokeapi.papi.config.ConfigManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.antlr.v4.runtime.misc.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@SpringBootApplication(scanBasePackages = "com.pokeapi.papi")
public class PokeApiApplication {

    public static class MyConfig {
        public String password = "defaultpassword";
        public String username = "defaultuser";
        public String url = "localhost";
    }

    @Autowired
    private UsersRepository usersRepository;

    private static final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws Exception {

        ConfigManager<MyConfig> cm = new ConfigManager<>(
                Paths.get("config.yml"),
                MyConfig.class
        );
        cm.load();
        MyConfig cfg = cm.get();

        //PokeApiDB.resetAllCookies();

        SpringApplication.run(PokeApiApplication.class, args);

        AsciiArt();
    }

    @Controller
    public class SimpleController {

        private final PokeApiFileService storage;

        public SimpleController(PokeApiFileService storage) {
            this.storage = storage;
        }

        record SignupRequest(String username, String email, String password, String salt) {
        }

        record LoginRequest(String usernameoremail, String password) {
        }




        // -------- HOME PAGE --------
        @GetMapping("/")
        public Object getHomePage(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDB.checkIfCookieValid(loginCookie.get().getValue())) {
                    ModelAndView modelAndView = new ModelAndView("homeempty");
                    modelAndView.addObject("name", PokeApiDB.getUsernameFromCookie(loginCookie.get().getValue()));
                    modelAndView.addObject("pfpurl", "/papipfps/" + PokeApiDB.getUsernameFromCookie(loginCookie.get().getValue()) + "/pfp.png");
                    return modelAndView;
                }
            }
            return "redirect:/login";
        }

        @GetMapping("/pokemon")
        public Object getPokemon(@RequestParam("id") String nameid, HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            Optional<Cookie> loginCookie = (cookies == null ? Stream.<Cookie>empty() : Arrays.stream(cookies))
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .findAny();
            if (loginCookie.isEmpty() || !PokeApiDB.checkIfCookieValid(loginCookie.get().getValue())) {
                return "redirect:/login";
            }

            Optional<String> s = PokeApiService.getPokemon(nameid);
            if(s.isEmpty())
                return "redirect:/";

            Pokemon pokemon = gson.fromJson(s.get(), Pokemon.class);

            for(Stats stats : pokemon.stats()) {
                String name = stats.inner().getName();
                stats.inner().setName(name.replace("-", " "));
            }

            ModelAndView modelAndView = new ModelAndView("home");
            modelAndView.addObject("name", PokeApiDB.getUsernameFromCookie(loginCookie.get().getValue()));
            modelAndView.addObject("pfpurl", "/papipfps/" + PokeApiDB.getUsernameFromCookie(loginCookie.get().getValue()) + "/pfp.png");
            modelAndView.addObject("pokemon", pokemon);
            modelAndView.addObject("generation", calGeneration(pokemon.id()));
            return modelAndView;

        }

        public String calGeneration(int id) {

            final List<Pair<Integer, String>> GENERATIONS = new ArrayList<>() {{
                add(new Pair<>(151, "gen I"));
                add(new Pair<>(251, "gen II"));
                add(new Pair<>(386, "gen III"));
                add(new Pair<>(493, "gen IV"));
                add(new Pair<>(649, "gen V"));
                add(new Pair<>(721, "gen VI"));
                add(new Pair<>(809, "gen VII"));
                add(new Pair<>(905, "gen VIII"));
                add(new Pair<>(1025, "gen IX"));
            }};

            String gen = "gen X";
            for (Pair<Integer, String> g : GENERATIONS) {
                if (id <= g.a) {
                    gen = g.b;
                    break;
                }
            }
            return gen;
        }



        // -------- LOGIN PAGE --------
        @GetMapping("/login")
        public String getLoginPage(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDB.checkIfCookieValid(loginCookie.get().getValue())) {
                    return "redirect:/";
                }
            }
            return "login";
        }

        // -------- SIGNUP PAGE --------
        @GetMapping("/signup")
        public String getSignupPage(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDB.checkIfCookieValid(loginCookie.get().getValue())) {
                    return "redirect:/";
                }
            }
            return "signup";
        }

        // -------- USER PAGE --------
        @GetMapping("/user/{username}")
        public String getUserPage(HttpServletRequest request, @PathVariable String username) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDB.checkIfCookieValid(loginCookie.get().getValue())) {
                    return "user";
                }
            }
            return "redirect:/login";
        }

        @GetMapping("user")
        public String getPersonalUserPage(HttpServletRequest request, @PathVariable String username) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDB.checkIfCookieValid(loginCookie.get().getValue())) {
                    if (Objects.equals(username, PokeApiDB.getUsernameFromCookie(loginCookie.get().getValue()))) {
                        return "personaluser";
                    } else {
                        return "redirect:/user/" + PokeApiDB.getUsernameFromCookie(loginCookie.get().getValue());
                    }
                }
            }
            return "redirect:/login";
        }


        // -------- ERROR PAGE --------
        @GetMapping("/error")
        public String errorPage() {
            return "error";
        }

        // -------- APIs --------
        @GetMapping("/getuserdata")
        public ResponseEntity<Map<String, Object>> getUsernameFromCookie(HttpServletRequest request) {
            Map<String, Object> response = new HashMap<>();

            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                response.put("loggedIn", false);
                return ResponseEntity.ok(response);
            }

            Optional<String> loginCookie = Arrays.stream(cookies)
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findAny();

            if (loginCookie.isEmpty()) {
                response.put("loggedIn", false);
                return ResponseEntity.ok(response);
            }

            String username = PokeApiDB.getUsernameFromCookie(loginCookie.get());

            if (username.matches("invalid cookie")) {
                response.put("loggedIn", false);
            } else {
                response.put("loggedIn", true);
                response.put("username", username);
                response.put("pfplink", "https://192.168.161.22:8081/papipfps/" + username + "/pfp.png");
            }

            return ResponseEntity.ok(response);
        }

        @GetMapping("/logout")
        public ResponseEntity<String> logout(@CookieValue("loginCookie") String loginCookie) {
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, ResponseCookie.from("loginCookie", loginCookie).secure(true).maxAge(0).httpOnly(true).path("/").build().toString()).body("cookieweg");
        }

        @PostMapping("/signup/submit")
        @ResponseBody
        public Map<String, Object> postSignupPage(@RequestBody SignupRequest req) throws IOException {
            Map<String, Object> response = new HashMap<>();
            if (checkForValidUsername(req.username()) && checkForValidEmail(req.email())) {
                try {
                    PokeApiDBService.createUser(usersRepository, req.username(), req.email(), req.password(), req.salt());
                } catch (Exception err) {
                    response.put("success", false);
                    response.put("error", err);
                    return response;
                }
            } else if (!checkForValidUsername(req.username)) {
                response.put("success", false);
                response.put("error", "Invalid Username. Username must be 3–16 characters long and may only contain letters, numbers, and underscores.");
            } else if (!checkForValidEmail(req.email)) {
                response.put("success", false);
                response.put("error", "Invalid Email. Email must look like user@example.com and only use letters, numbers, ., _, +, - before @.\n");
            }
            return response;
        }

        @PostMapping("/login/submit")
        @ResponseBody
        public Map<String, Object> postLoginPage(
                @RequestBody LoginRequest req,
                HttpServletResponse response
        ) {
            Map<String, Object> respMap = new HashMap<>();

//            boolean ok = PokeApiDB.checkLogin(req.usernameoremail(), req.password());
            boolean ok;
            ok = true;
            if (!ok) {
                respMap.put("success", false);
            } else {
                respMap.put("success", true);

                String cookieValue = PokeApiDB.generateCookie(req.usernameoremail());

                Cookie cookie = new Cookie("loginCookie", cookieValue);
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24 * 7);

                response.addCookie(cookie);

                respMap.put("loginCookie", cookieValue);
            }

            return respMap;
        }

        @GetMapping("/home/pokemon/{nameid}") //794
        public ResponseEntity<String> postPokemonHomePage(@PathVariable String nameid) {
            return ResponseEntity.of(PokeApiService.getPokemon(nameid));
        }

        @GetMapping("/home/move/{nameid}")
        public ResponseEntity<String> postMoveHomePage(@PathVariable String nameid) {
            return ResponseEntity.of(Optional.of(PokeApiService.getMove(nameid)));
        }

        @PostMapping("/user/{username}/uploadpfp")
        public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes, @PathVariable String username, HttpServletRequest request) {
            Map<String, Object> response = new HashMap<>();

            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return "error";
            }

            Optional<String> loginCookie = Arrays.stream(cookies)
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findAny();

            if (loginCookie.isEmpty()) {
                return "error";
            }

            String cookieUsername = PokeApiDB.getUsernameFromCookie(loginCookie.get());
            if (cookieUsername.equals(username)){
                storage.save(file, username);
                return "redirect:/user/" + username;
            }

            return "error";
        }

        @GetMapping("home/team")
        @ResponseBody
        public Map<String, Object> getTeamPokemons(@CookieValue("loginCookie") String loginCookie) {
            return PokeApiDB.getPokemonsFromUser(loginCookie);
        }

        @GetMapping("home/team/add/{nameid}")
        public Map<String, Object> addPokemonToTeam(@PathVariable String nameid) {
            return PokeApiDB.addPokemonToTeam(nameid);
        }
    }

    private boolean checkForValidUsername(String username) {
        if (username == null) return false;
        if (username.length() < 3 || username.length() > 16) return false;
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean checkForValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public static void AsciiArt() {
        logger.info("========================================");
        logger.info(" mmmmmm       mm     mmmmmm     mmmmmm  ");
        logger.info(" ##\"\"\"\"#m    ####    ##\"\"\"\"#m   \"\"##\"\"  ");
        logger.info(" ##    ##    ####    ##    ##     ##    ");
        logger.info(" ######\"    ##  ##   ######\"      ##    ");
        logger.info(" ##         ######   ##           ##    ");
        logger.info(" ##        m##  ##m  ##         mm##mm  ");
        logger.info(" \"\"        \"\"    \"\"  \"\"         \"\"\"\"\"\"  ");
        logger.info("========================================");
    }
}