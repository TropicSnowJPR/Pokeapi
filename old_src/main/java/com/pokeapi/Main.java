package com.pokeapi;

public class Main {
    public static void main(String[] args) {
        // Werte für den neuen User
        String username = "jonas";
        String email = "jonas@example.com";
        String password = "MeinSicheresPasswort123!";

        // Aufruf der com.pokeapi.DB-Methode
        System.out.println(DB.createUser(username, email, password));;
    }
}