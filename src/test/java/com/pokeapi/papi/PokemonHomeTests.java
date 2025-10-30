package com.pokeapi.papi;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;

import java.awt.*;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PokemonHomeTests {

    public void setup() {
        open("https://localhost:8081/login");
        $(By.id("usernameoremail")).setValue("jonny");
        $(By.id("password")).setValue("StrongPass!23");
        $(By.className("button")).click();// Waits until element disappears
        $(By.id("username")).shouldHave(text("jonny"));
        $(By.id("user-logout")).click();
    }

    @Test
    public void randomPokemonHomePageTest() {
        setup();
        String firstValue = $(By.id("pokemon-name")).getText();
        $(By.id("random-button")).click();
        if ($(By.id("pokemon-name")).getText().isEmpty() && $(By.id("pokemon-name")).getText() == "Name: rayquaza / ID:" && $(By.id("pokemon-name")).getText() != firstValue) {
            throw new AssertionError("Random Pokemon name should not be empty");

        }
        $(By.id("user-logout")).click();
    }

    @Test
    public void searchPokemonHomePageTest() {
        setup();
        $(By.id("search")).setValue("pikachu");
        $(By.id("search-button")).click();
        $(By.id("pokemon-name")).shouldHave(text("Name: pikachu / ID: 25"));
        $(By.id("user-logout")).click();
    }

    @Test
    public void addPokemonToTeamHomePageTest() {
        setup();
        $(By.id("search")).setValue("pikachu");
        $(By.id("search-button")).click();
        $(By.id("pokemon-name")).shouldHave(text("Name: pikachu / ID: 25"));
        $(By.id("add-to-team")).click();
        assertTrue($$(By.className("team-member")).texts().contains("pikachu"));
        $(By.id("user-logout")).click();
    }

    @Test
    public void copyPokemonHomePageTest() {
        setup();
        $(By.id("search")).setValue("pikachu");
        $(By.id("search-button")).click();
        $(By.id("pokemon-name")).shouldHave(text("Name: pikachu / ID: 25"));
        $(By.id("copy-pokemon")).click();
        Alert alert = switchTo().alert();
        String alertText = alert.getText();
        alert.accept();
        assertTrue(alertText.contains("copied") || alertText.contains("copy")); // adjust expected text as needed
        String clipboardContent;
        try {
            clipboardContent = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read clipboard", e);
        }
        $(By.id("user-logout")).click();
    }

    @Test
    public void pastePokemonHomePageTest() {
        setup();
        String pokemonData = "{\"pokemon\": { \"success\": true, \"pokemon\": {\"id\": 1, \"name\": \"bulbasaur\"}}}";// Example data to paste
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(pokemonData), null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to write to clipboard", e);
        }
        $(By.id("paste-pokemon")).click();
        $(By.id("pokemon-name")).shouldHave(text("Name: bulbasaur / ID: 1"));
        $(By.id("user-logout")).click();
    }

    @Test
    public void downloadExcelHomePageTest() {
        setup();
        $(By.id("download-excel")).click();
        $(By.id("user-logout")).click();
    }

    @Test void downloadCSVHomePageTest() {
        setup();
        $(By.id("download-csv")).click();
        $(By.id("user-logout")).click();
    }

}
