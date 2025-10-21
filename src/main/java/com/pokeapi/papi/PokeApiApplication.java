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
                if (pokemon.id() <= g.a) {
                    gen = g.b;
                    break;
                }
            }

            modelAndView.addObject("generation", gen);
            return modelAndView;

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
            } else {
                respMap.put("success", true);
                Long uid = PokeApiDBService.getIdByUsernameOrEmail(usersRepository, req.usernameoremail());
                String cookieValue = PokeApiDBService.createCookie(usersRepository, cookiesRepository, uid);
                if(cookieValue == null || cookieValue.isEmpty()) {
                    respMap.put("success", false);
                    return respMap;
                }

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

        @GetMapping("/login/salt/{usernameoremail}")
        @ResponseBody
        public Map<String, Object> getSalt(@PathVariable String usernameoremail) {
            Map<String, Object> respMap = new HashMap<>();
            String salt = PokeApiDBService.getSaltByUsernameOrEmail(usersRepository, usernameoremail);
            if(salt == null || salt.isEmpty()) {
                respMap.put("success", false);
            } else {
                respMap.put("success", true);
                respMap.put("salt", salt);
            }
            return respMap;
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
        public Map<String, Object> postSignupPage(@RequestBody SignupRequest req) throws IOException {
            Map<String, Object> response = new HashMap<>();
            try {
                PokeApiDBService.createUser(usersRepository, req.username(), req.email(), req.password(), req.salt());
                Long id = PokeApiDBService.getIdByUsernameOrEmail(usersRepository, req.username());
                PokeApiDBService.createTeam(teamsRepository, usersRepository, id);
                response.put("success", true);
            } catch (Exception err) {
                response.put("success", false);
                response.put("error", err);
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
                    return PokeApiDBService.getTeamFromCookie(pokemonsRepository, teamsRepository, cookiesRepository, String.valueOf(loginCookie.get().getValue()));
                } catch (Exception e) {
                    logger.error("Error getting team: " + e);
                }
            }
            return Map.of("success", false);
        }

        @GetMapping("/team/add/{id}")
        @ResponseBody
        public Map<String, Object> addPokemonToTeam(@PathVariable String id, HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) { return Map.of("success", false); }
            Optional<String> loginCookie = Arrays.stream(cookies)
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findAny();
            if (loginCookie.isEmpty()) { return Map.of("success", false); }
            PokeApiDBService.addPokemonToTeam(cookiesRepository, teamsRepository, usersRepository, loginCookie.get(), id);
            return Map.of("success", true);
        }

        @GetMapping("/team/remove/{name}")
        @ResponseBody
        public Map<String, Object> removePokemonFromTeam(@PathVariable String name, HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) { return Map.of("success", false); }
            Optional<String> loginCookie = Arrays.stream(cookies)
                    .filter(cookie -> "loginCookie".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findAny();
            if (loginCookie.isEmpty()) { return Map.of("success", false); }
            Optional<String> s = PokeApiService.getPokemon(name);
            if (s.isEmpty()) { return Map.of("success", false); }
            Pokemon pokemon = gson.fromJson(s.get(), Pokemon.class);
            String id = String.valueOf(pokemon.id());
            PokeApiDBService.removePokemonFromTeam(cookiesRepository, teamsRepository, usersRepository, loginCookie.get(), id);
            logger.info("Removed pokemon " + id + " from team");
            return Map.of("success", true);
        }



        // -------- USER API PAGE --------
        @PostMapping("user/uploadpfp")
        @ResponseBody
        public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes, HttpServletRequest request) {
            Map<String, Object> response = new HashMap<>();
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