package com.pokeapi.papi;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class PokemonHomeTests {

    public void setup() {
        open("https://localhost:8081/login");
        $(By.id("usernameoremail")).setValue("jonny");
        $(By.id("password")).setValue("IDontKnow123!");
        $(By.className("button")).click();// Waits until element disappears
        $(By.id("username")).shouldHave(text("jonny"));
    }

    @Test
    public void randomPokemonHomePageTest() {
        setup();
        String firstValue = $(By.id("pokemon-name")).getText();
        $(By.id("random-button")).click();
        if ($(By.id("pokemon-name")).getText().isEmpty() && $(By.id("pokemon-name")).getText() == "Name: rayquaza / ID:" && $(By.id("pokemon-name")).getText() != firstValue) {
            throw new AssertionError("Random Pokemon name should not be empty");
        }
    }

    @Test
    public void searchPokemonHomePageTest() {
        setup();
        $(By.id("search")).setValue("pikachu");
        $(By.id("search-button")).click();
        $(By.id("pokemon-name")).shouldHave(text("Name: pikachu / ID: 25"));
    }

    @Test
    public void addPokemonToTeamHomePageTest() {
        setup();
        $(By.id("search")).setValue("pikachu");
        $(By.id("search-button")).click();
        $(By.id("pokemon-name")).shouldHave(text("Name: pikachu / ID: 25"));
        $(By.id("add-to-team-button")).click();
        $(By.id("team-member")).shouldHave(text("pikachu"));
    }

    @Test
    public void removePokemonFromTeamHomePageTest() {
        // This is a final placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void teamDetailsHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void terastallizeChangeHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void copyPokemonHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void pastePokemonHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void downloadExcelHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test void downloadCSVHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void checkMoveInfoHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void audioPlaybackHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void openAccountSettingsHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }

    @Test
    public void logoutUserHomePageTest() {
        // This is a placeholder for future tests related to the Pokemon Home page.
    }
}
