package com.pokeapi.papi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokeapi.papi.config.ConfigManager;
import com.pokeapi.papi.db.CookiesRepository;
import com.pokeapi.papi.db.PokemonsRepository;
import com.pokeapi.papi.db.TeamsRepository;
import com.pokeapi.papi.db.UsersRepository;
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

    @Autowired
    private CookiesRepository cookiesRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private PokemonsRepository pokemonsRepository;

    private static final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws Exception {

        ConfigManager<MyConfig> cm = new ConfigManager<>(
                Paths.get("config.yml"),
                MyConfig.class
        );
        cm.load();
        MyConfig cfg = cm.get();

        //PokeApiDB.resetAllCookies(); TODO: ENABLE BEFORE DEPLOYMENT

        SpringApplication.run(PokeApiApplication.class, args);

        logger.info("POKEAPI STARTED SUCCESSFULLY");
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
                if (loginCookie.isPresent() && PokeApiDBService.validateCookie(cookiesRepository, loginCookie.get().getValue())) {
                    ModelAndView modelAndView = new ModelAndView("homeempty");
                    modelAndView.addObject("name", (PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, String.valueOf(loginCookie.get().getValue()))));
                    modelAndView.addObject("pfpurl", "/papipfps/" + (PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, String.valueOf(loginCookie.get().getValue()))) + "/pfp.png");
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
            if (loginCookie.isEmpty() || !PokeApiDBService.validateCookie(cookiesRepository, loginCookie.get().getValue())) {
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
            modelAndView.addObject("name", (PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, String.valueOf(loginCookie.get().getValue()))));
            modelAndView.addObject("pfpurl", "/papipfps/" + (PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, String.valueOf(loginCookie.get().getValue()))) + "/pfp.png");
            modelAndView.addObject("pokemon", pokemon);

            final List<Pair<Integer, String>> GENERATIONS = new ArrayList<>() {{
                add(new Pair<>(151, "Gen I"));
                add(new Pair<>(251, "Gen II"));
                add(new Pair<>(386, "Gen III"));
                add(new Pair<>(493, "Gen IV"));
                add(new Pair<>(649, "Gen V"));
                add(new Pair<>(721, "Gen VI"));
                add(new Pair<>(809, "Gen VII"));
                add(new Pair<>(905, "Gen VIII"));
                add(new Pair<>(1025, "Gen IX"));
            }};

            String gen = "Gen X";
            for (Pair<Integer, String> g : GENERATIONS) {
                if (pokemon.id() <= g.a) {
                    gen = g.b;
                    break;
                }
            }

            modelAndView.addObject("generation", gen);
            return modelAndView;

        }

        @GetMapping("/move-api")
        @ResponseBody
        public Map<String, Object> getMove(@RequestParam("id") String id) {
            Optional<String> s = PokeApiService.getMove(id).describeConstable();
            if(s.isEmpty())
                return Map.of("success", false);

            return Map.of(
                    "success", true,
                    "move", gson.fromJson(s.get(), Moves.class)
            );
        }

        @GetMapping("/pokemon-api")
        @ResponseBody
        public Map<String, Object> getPokemonApi(@RequestParam("id") String id) {
            Optional<String> s = PokeApiService.getPokemon(id);
            if(s.isEmpty())
                return Map.of("success", false);
            return Map.of(
                    "success", true,
                    "pokemon", gson.fromJson(s.get(), Pokemon.class)
            );
        }



        // -------- LOGIN PAGE --------
        @GetMapping("/login")
        public String getLoginPage(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDBService.validateCookie(cookiesRepository, loginCookie.get().getValue())) {
                    return "redirect:/";
                }
            }
            return "login";
        }

        @PostMapping("/login/submit")
        @ResponseBody
        public Map<String, Object> postLoginPage(
                @RequestBody LoginRequest req,
                HttpServletResponse response
        ) {
            Map<String, Object> respMap = new HashMap<>();

            boolean ok = PokeApiDBService.validatePassword(usersRepository, req.usernameoremail(), req.password()); // PASSWORD NEED TO BE HASHED CLIENT SIDE <<< CLIENT SIDE NOT IMPLEMENTED YET

            if (!ok) {
                respMap.put("success", false);
                respMap.put("error", "Invalid username/email or password");
            } else {
                String cookieValue;
                try {
                    Long uid = PokeApiDBService.getIdByUsernameOrEmail(usersRepository, req.usernameoremail());
                    cookieValue = PokeApiDBService.createCookie(usersRepository, cookiesRepository, uid);
                    if(cookieValue == null || cookieValue.isEmpty()) {
                        respMap.put("success", false);
                        respMap.put("error", "Could not create login cookie");
                        return respMap;
                    }
                } catch (Exception e) {
                    respMap.put("success", false);
                    respMap.put("error", e.getMessage());
                    return respMap;
                }

                Cookie cookie = new Cookie("loginCookie", cookieValue);
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24 * 7);

                response.addCookie(cookie);

                respMap.put("loginCookie", cookieValue);
                respMap.put("success", true);
            }
            return respMap;
        }

        @GetMapping("/login/salt/{usernameoremail}")
        @ResponseBody
        public Map<String, Object> getSalt(@PathVariable String usernameoremail) {
            Map<String, Object> respMap = new HashMap<>();
            try {
                String salt = PokeApiDBService.getSaltByUsernameOrEmail(usersRepository, usernameoremail);
                respMap.put("success", true);
                respMap.put("salt", salt);
                return respMap;
            } catch (Exception e) {
                respMap.put("success", false);
                respMap.put("error", e.getMessage());
                return respMap;
            }
        }



        // -------- SIGNUP PAGE --------
        @GetMapping("/signup")
        public String getSignupPage(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDBService.validateCookie(cookiesRepository, loginCookie.get().getValue())) {
                    return "redirect:/";
                }
            }
            return "signup";
        }

        @PostMapping("/signup/submit")
        @ResponseBody
        public Map<String, Object> postSignupPage(@RequestBody SignupRequest req) {
            Map<String, Object> response = new HashMap<>();
            try {
                PokeApiDBService.createUser(usersRepository, req.username(), req.email(), req.password(), req.salt());
                Long id = PokeApiDBService.getIdByUsernameOrEmail(usersRepository, req.username());
                PokeApiDBService.createTeam(teamsRepository, usersRepository, id);
                response.put("success", true);
            } catch (Exception err) {
                response.put("success", false);
                response.put("error", err.getMessage());
                return response;
            }
            return response;
        }



        // -------- USER PAGE --------
        @GetMapping("/user")
        public Object getUserPage(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                if (loginCookie.isPresent() && PokeApiDBService.validateCookie(cookiesRepository, loginCookie.get().getValue())) {
                    ModelAndView modelAndView = new ModelAndView("user");
                    modelAndView.addObject("name", (PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, String.valueOf(loginCookie.get().getValue()))));
                    modelAndView.addObject("pfpurl", "/papipfps/" + (PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, String.valueOf(loginCookie.get().getValue()))) + "/pfp.png");
                    return modelAndView;
                }
            }
            return "redirect:/login";
        }



        // -------- ERROR PAGE --------
        @GetMapping("/error")
        public String errorPage() {
            return "error";
        }



        // -------- LOGOUT PAGE --------
        @GetMapping("/logout")
        public ResponseEntity<String> logout(@CookieValue("loginCookie") String loginCookie) {
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, ResponseCookie.from("loginCookie", loginCookie).secure(true).maxAge(0).httpOnly(true).path("/").build().toString()).body("cookieweg");
        }



        // -------- TEAM API PAGES --------
        @GetMapping("/team")
        @ResponseBody
        public Map<String, Object> getTeamPokemons(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                try {
                    if(loginCookie.isPresent()) { String value = String.valueOf(loginCookie.get().getValue());
                    return PokeApiDBService.getTeamFromCookie(pokemonsRepository, teamsRepository, cookiesRepository, value); }
                } catch (Exception e) {
                    logger.error("Error getting team: {}", String.valueOf(e));
                }
            }
            return Map.of("success", false);
        }

        @GetMapping("/team/add/{nameid}")
        @ResponseBody
        public Map<String, Object> addPokemonToTeam(@PathVariable String nameid, HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) { return Map.of("success", false); }
            Optional<String> loginCookie = Arrays.stream(cookies)
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findAny();
            if (loginCookie.isEmpty()) { return Map.of("success", false); }
            if (nameid.matches(".*[a-zA-Z].*")) {
                Optional<String> s = PokeApiService.getPokemon(nameid);
                if (s.isEmpty()) { return Map.of("success", false); }
                Pokemon pokemon = gson.fromJson(s.get(), Pokemon.class);
                nameid = String.valueOf(pokemon.id());
            }
            try {
                PokeApiDBService.addPokemonToTeam(cookiesRepository, teamsRepository, usersRepository, loginCookie.get(), nameid);
            } catch (Exception e) {
                return Map.of("success", true);
            }
            return Map.of("success", false);
        }

        @GetMapping("/team/remove/{nameid}")
        @ResponseBody
        public Map<String, Object> removePokemonFromTeam(@PathVariable String nameid, HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) { return Map.of("success", false); }
            Optional<String> loginCookie = Arrays.stream(cookies)
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findAny();
            if (loginCookie.isEmpty()) { return Map.of("success", false); }
            if (nameid.matches(".*[a-zA-Z].*")) {
                Optional<String> s = PokeApiService.getPokemon(nameid);
                if (s.isEmpty()) { return Map.of("success", false); }
                Pokemon pokemon = gson.fromJson(s.get(), Pokemon.class);
                nameid = String.valueOf(pokemon.id());
            }
            try {
                PokeApiDBService.removePokemonFromTeam(cookiesRepository, teamsRepository, usersRepository, loginCookie.get(), nameid);
            } catch (Exception e) {
                return Map.of("success", true);
            }
            return Map.of("success", false);
        }



        // -------- USER API PAGE --------
        @PostMapping("user/uploadpfp")
        @ResponseBody
        public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes, HttpServletRequest request) {

            Cookie[] cookies = request.getCookies();
            if (cookies == null) { return "error"; }
            Optional<String> loginCookie = Arrays.stream(cookies)
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findAny();
            if (loginCookie.isEmpty()) { return "error"; }
            String cookieUsername = PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, loginCookie.get());
            storage.save(file, cookieUsername);
            return "redirect:/user/";
        }
    }
}