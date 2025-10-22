package com.pokeapi;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;


@Named("papi")
@RequestScoped
public class PokeAPI {

    private String username;
    private String password;
    private String email;

    // Constructor
    public PokeAPI(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String Error;

    public String SearchValue;
    public String JsonValue;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Sign Up
    public String signupNewUser() {

        //   Name validity check
        if (username == null || username.isEmpty()) {
            return "Missing username";

        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "Invalid username";

        }

        //   email validity check
        if (email == null || email.isEmpty()) { // email kind check later (like @yahoo.com or @google.com)
            return "Missing email";

        } else if (!email.matches("^[a-zA-Z0-9_@.+-]")) {
            return "Invalid email";

        }

        //   password validity check
        if (password == null || password.isEmpty()) {
            return "Missing password";

        } else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$")) {
            StringBuilder reasons = new StringBuilder();
            if (password.length() < 8) {
                reasons.append("min. 8 characters");
            }
            if (!password.matches(".*[a-z].*")) {
                reasons.append("at least 1 lowercase letter, ");
            }
            if (!password.matches(".*[A-Z].*")) {
                reasons.append("at least 1 uppercase letter, ");
            }
            if (!password.matches(".*\\d.*")) {
                reasons.append("at least 1 digit, ");
            }
            if (!password.matches(".*[^a-zA-Z0-9].*")) {
                reasons.append("at least 1 special character, ");
            }

            if (!reasons.isEmpty()) {
                reasons.setLength(reasons.length() - 2); // Remove the empty space and comma.
                return "password not secure enough (" + reasons + ")";
            }

        }

        String Info = DB.createUser(username, email, password);

        if (Info == null || Info.isEmpty())  {
            return null;
        }

        if (Info.contains("Unknown Error")) {
            return "IDK man my code sucks and something did not work";
        }

        return Info;
    }

    // Login
    public  String loginUser(String nameoremail, String password) { // Return auth cookie
        return nameoremail + password;
    }

    // Auth Cookie (need to inform how works)





}




