package com.pokeapi.papi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Stream;

@SpringBootApplication(scanBasePackages = "com.pokeapi.papi")
public class PokeApiApplication {

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

        org.springframework.context.ConfigurableApplicationContext ctx = SpringApplication.run(PokeApiApplication.class, args);

        String ip;

        try (final DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 12345);
            ip = datagramSocket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            ip = "";
        };

        String port = ctx.getEnvironment().getProperty("local.server.port",
                ctx.getEnvironment().getProperty("server.port", "8080"));

        logger.info("Started Application at: https://{}:{}", ip, port);
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
        public Object getHomePage() {
            SecureRandom sr = new SecureRandom();
            return "redirect:/pokemon?id=" + sr.nextInt(1025) + 1;
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
        public Map<String, Object> getMoveApi(@RequestParam("id") String id) {
            Optional<String> s = PokeApiService.getMove(id).describeConstable();
            return s.map(string -> Map.of(
                    "success", true,
                    "move", gson.fromJson(string, Moves.class)
            )).orElseGet(() -> Map.of("success", false));

        }

        @GetMapping("/pokemon-api")
        @ResponseBody
        public Map<String, Object> getPokemonApi(@RequestParam("id") String id) {
            Optional<String> s = PokeApiService.getPokemon(id);
            return s.map(string -> Map.of(
                    "success", true,
                    "pokemon", gson.fromJson(string, Pokemon.class)
            )).orElseGet(() -> Map.of("success", false));
        }

        @GetMapping("/team-api")
        @ResponseBody
        public Map<String, Object> getTeamApi(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
                try {
                    String value = null;
                    if (loginCookie.isPresent()) {
                        value = loginCookie.get().getValue();
                    }

                    Map<String, Object> team = PokeApiDBService.getTeamFromCookie(pokemonsRepository, teamsRepository, cookiesRepository, value);

                    Object memObj = team.get("team_members");
                    String[] memArr;

                    try {
                        memArr = gson.fromJson(gson.toJson(memObj), String[].class);
                        if (memArr == null) memArr = new String[0];
                        memArr = Arrays.stream(memArr).map(String::trim).toArray(String[]::new);
                    } catch (Exception e) {
                        memArr = new String[0];
                    }

                    String mem1 = memArr.length > 0 ? memArr[0] : null;
                    String mem2 = memArr.length > 1 ? memArr[1] : null;
                    String mem3 = memArr.length > 2 ? memArr[2] : null;
                    String mem4 = memArr.length > 3 ? memArr[3] : null;
                    String mem5 = memArr.length > 4 ? memArr[4] : null;
                    String mem6 = memArr.length > 5 ? memArr[5] : null;



                    List<Map<String, Object>> pmaps = new ArrayList<>();
                    for (String mem : Arrays.asList(mem1, mem2, mem3, mem4, mem5, mem6)) {
                        if (mem == null || mem.isBlank()) continue;
                        Map<String, Object> pmap = getPokemonApi(mem);
                        pmaps.add(pmap);
                    }

                    List<String> TeamTypes = new ArrayList<>();
                    List<String> TeamTypesUnique;
                    Map<String, Integer> TeamTypeCounts = new HashMap<>();

                    int TeamSize = 0;
                    Map<String, Integer> TeamStatsSum = new HashMap<>();

                    for (Map<String, Object> pmap : pmaps) {
                        if (pmap == null) continue;
                        Object pobj = pmap.get("pokemon");
                        if (pobj == null) continue;

                        String pjson = gson.toJson(pobj);
                        Map<?, ?> p = gson.fromJson(pjson, Map.class);

                        Object typesObj = p.get("types");
                        if (typesObj instanceof List) {
                            for (Object tEntry : (List<?>) typesObj) {
                                if (!(tEntry instanceof Map<?, ?> tmap)) continue;
                                Object typeInner = tmap.get("type");
                                String typeName = null;
                                if (typeInner instanceof Map) {
                                    typeName = String.valueOf(((Map<?, ?>) typeInner).get("name"));
                                } else if (tmap.get("name") != null) {
                                    typeName = String.valueOf(tmap.get("name"));
                                }
                                if (typeName != null && !typeName.isBlank()) {
                                    TeamTypes.add(typeName);
                                    TeamTypeCounts.merge(typeName, 1, Integer::sum);
                                }
                            }
                        }

                        Object statsObj = p.get("stats");
                        if (statsObj instanceof List) {
                            for (Object sEntry : (List<?>) statsObj) {
                                if (!(sEntry instanceof Map<?, ?> smap)) continue;
                                String statName = null;
                                Object statInner = smap.get("stat");
                                if (statInner instanceof Map) {
                                    statName = String.valueOf(((Map<?, ?>) statInner).get("name"));
                                } else if (smap.get("name") != null) {
                                    statName = String.valueOf(smap.get("name"));
                                }
                                int base = 0;
                                Object baseObj = smap.get("base_stat");
                                if (baseObj instanceof Number) {
                                    base = ((Number) baseObj).intValue();
                                } else if (baseObj != null) {
                                    try { base = Integer.parseInt(String.valueOf(baseObj)); } catch (Exception ignored) {}
                                }
                                if (statName != null) {
                                    TeamStatsSum.merge(statName, base, Integer::sum);
                                }
                            }
                        }

                        TeamSize++;
                    }

                    TeamTypesUnique = new ArrayList<>(new LinkedHashSet<>(TeamTypes));

                    Map<String, Integer> TeamStats = new HashMap<>();
                    if (TeamSize > 0) {
                        for (Map.Entry<String, Integer> e : TeamStatsSum.entrySet()) {
                            TeamStats.put(e.getKey(), e.getValue() / TeamSize);
                        }
                    }

                    record Weakness(String type, String weakness_1, Optional<String> weakness_2, Optional<String> weakness_3, Optional<String> weakness_4, Optional<String> weakness_5, Optional<String> weakness_6, Optional<String> weakness_7) {
                    }

                    final List<Weakness> WEAKNESSES = new ArrayList<>() {{
                        add(new Weakness("Normal", "Rock", Optional.of("Rock"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Fire", "Fire", Optional.of("Water"), Optional.of("Rock"), Optional.of("Dragon"), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Water", "Water", Optional.of("Grass"), Optional.of("Dragon"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Electric", "Electric", Optional.of("Grass"), Optional.of("Dragon"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Grass", "Fire", Optional.of("Grass"), Optional.of("Poison"), Optional.of("Flying"), Optional.of("Bug"), Optional.of("Dragon"), Optional.of("Steel")));
                        add(new Weakness("Ice", "Fire", Optional.of("Water"), Optional.of("Ice"), Optional.of("Steel"), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Fighting", "Poison", Optional.of("Flying"), Optional.of("Psychic"), Optional.of("Bug"), Optional.of("Fairy"), Optional.empty(), Optional.empty()));
                        add(new Weakness("Poison", "Poison", Optional.of("Ground"), Optional.of("Rock"), Optional.of("Ghost"), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Ground", "Grass", Optional.of("Bug"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Flying", "Electric", Optional.of("Rock"), Optional.of("Steel"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Psychic", "Psychic", Optional.of("Steel"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Bug", "Fire", Optional.of("Fighting"), Optional.of("Flying"), Optional.of("Ghost"), Optional.of("Steel"), Optional.of("Fairy"), Optional.empty()));
                        add(new Weakness("Rock", "Fighting", Optional.of("Ground"), Optional.of("Steel"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Ghost", "Dark", Optional.of("Ghost"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Dragon", "Steel", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Dark", "Fighting", Optional.of("Dark"), Optional.of("Fairy"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Steel", "Fire", Optional.of("Water"), Optional.of("Electric"), Optional.of("Steel"), Optional.empty(), Optional.empty(), Optional.empty()));
                        add(new Weakness("Fairy", "Fire", Optional.of("Poison"), Optional.of("Steel"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                    }};

                    List WeaknessTypes = new ArrayList();
                    for (String t : TeamTypesUnique) {
                        for (Weakness w : WEAKNESSES) {
                            if (w.type().equalsIgnoreCase(t)) {
                                WeaknessTypes.add(w.weakness_1());
                                w.weakness_2().ifPresent(WeaknessTypes::add);
                                w.weakness_3().ifPresent(WeaknessTypes::add);
                                w.weakness_4().ifPresent(WeaknessTypes::add);
                                w.weakness_5().ifPresent(WeaknessTypes::add);
                                w.weakness_6().ifPresent(WeaknessTypes::add);
                                w.weakness_7().ifPresent(WeaknessTypes::add);
                            }
                        }
                    }

                    WeaknessTypes = new ArrayList<>(new LinkedHashSet<>(WeaknessTypes));

                    return Map.of(
                            "success", true,
                            //"teamTypes", TeamTypes, Uncomment if you want repeated types
                            "teamTypesUnique", TeamTypesUnique,
                            "teamWeaknesses", WeaknessTypes,
                            "teamTypeCounts", TeamTypeCounts,
                            "teamSize", TeamSize,
                            "teamStats", TeamStats
                    );

                } catch (Exception _) {}
            }
            return Map.of("success", false);
        }

        @GetMapping("/move-types-api")
        @ResponseBody
        public Map<String, Object> getMoveTypesApi(HttpServletRequest request, @RequestParam("id") String id) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return Map.of("success", false);
            }
                Optional<Cookie> loginCookie = Arrays.stream(cookies)
                        .filter(cookie -> "loginCookie".equals(cookie.getName()))
                        .findAny();
            if (loginCookie.isEmpty()) {
                return Map.of("success", false);
                    }

                    try {

                Optional<String> pjsonOpt = PokeApiService.getPokemon(id);
                if (pjsonOpt.isEmpty()) {
                    return Map.of("success", false);
                    }

                Map<?, ?> pmap = gson.fromJson(pjsonOpt.get(), Map.class);

                Set<String> pokemonTypes = new LinkedHashSet<>();
                Object typesObj = pmap.get("types");
                        if (typesObj instanceof List) {
                            for (Object tEntry : (List<?>) typesObj) {
                                if (!(tEntry instanceof Map<?, ?> tmap)) continue;
                                Object typeInner = tmap.get("type");
                                String typeName = null;
                                if (typeInner instanceof Map) {
                                    typeName = String.valueOf(((Map<?, ?>) typeInner).get("name"));
                                } else if (tmap.get("name") != null) {
                                    typeName = String.valueOf(tmap.get("name"));
                                }
                                if (typeName != null && !typeName.isBlank()) {
                                    pokemonTypes.add(typeName);
                                }
                            }
                        }


                Object movesObj = pmap.get("moves");
                List<?> movesList = (movesObj instanceof List) ? (List<?>) movesObj : Collections.emptyList();

                Map<String, Integer> moveTypeCounts = new HashMap<>();
                int totalMovesConsidered = 0;
                int matchCount = 0;

                for (Object mEntry : movesList) {
                    if (!(mEntry instanceof Map<?, ?> mmap)) continue;
                    Object moveInner = mmap.get("move");
                    String moveName = null;
                    if (moveInner instanceof Map) {
                        moveName = String.valueOf(((Map<?, ?>) moveInner).get("name"));
                    } else if (mmap.get("name") != null) {
                        moveName = String.valueOf(mmap.get("name"));
                    }
                    if (moveName == null || moveName.isBlank()) continue;

                    Optional<String> moveJsonOpt = PokeApiService.getMove(moveName).describeConstable();
                    if (moveJsonOpt.isEmpty()) continue;

                    Map<?, ?> movemap = gson.fromJson(moveJsonOpt.get(), Map.class);
                    Object typeObj = movemap.get("type");
                    String moveType = null;
                    if (typeObj instanceof Map) {
                        moveType = String.valueOf(((Map<?, ?>) typeObj).get("name"));
                    } else if (movemap.get("name") != null) {
                        moveType = String.valueOf(movemap.get("name"));
                    }
                    if (moveType == null || moveType.isBlank()) continue;

                    moveTypeCounts.merge(moveType, 1, Integer::sum);
                    totalMovesConsidered++;
                    if (pokemonTypes.contains(moveType)) matchCount++;
                    }

                List<String> uniqueMoveTypes = new ArrayList<>(new LinkedHashSet<>(moveTypeCounts.keySet()));
                double matchPercentage = totalMovesConsidered > 0 ? (matchCount * 100.0 / totalMovesConsidered) : 0.0;

                    return Map.of(
                            "success", true,
                        "pokemonTypes", new ArrayList<>(pokemonTypes),
                        "uniqueMoveTypes", uniqueMoveTypes,
                        "moveTypeCounts", moveTypeCounts,
                        "totalMoves", totalMovesConsidered,
                        "matchCount", matchCount,
                        "matchPercentage", matchPercentage
                    );
                } catch(Exception _){
                }
            return Map.of("success", false);
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

            boolean ok = PokeApiDBService.validatePassword(usersRepository, req.usernameoremail(), req.password());

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
                } catch (Exception _) {
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