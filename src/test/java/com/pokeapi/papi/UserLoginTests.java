package com.pokeapi.papi;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class UserLoginTests {

    @Test
    public void userCanLoginByEmail() {
        open("https://localhost:8081/login");
        $(By.id("usernameoremail")).setValue("jonny@mail.com");
        $(By.id("password")).setValue("StrongPass!23");
        $(By.className("button")).click();
        $(By.id("username")).shouldHave(text("jonny")); // Waits until element gets text
        $(By.id("user-logout")).click();
    }

    @Test
    public void userCanLoginByUsername() {
        open("https://localhost:8081/login");
        $(By.id("usernameoremail")).setValue("jonny");
        $(By.id("password")).setValue("StrongPass!23");
        $(By.className("button")).click();// Waits until element disappears
        $(By.id("username")).shouldHave(text("jonny"));
        $(By.id("user-logout")).click();
    }

    @Test
    public void userCannotLoginWithInvalidCredentials() {
        open("https://localhost:8081/login");
        $(By.id("usernameoremail")).setValue("invaliduser");
        $(By.id("password")).setValue("wrongpassword");
        $(By.className("button")).click();
        $(By.className("errormessage")).shouldHave(text("⚠️ Invalid Usernamer/Email or Password"));
    }

    @Test
    public void userCannotLoginWithEmptyFields() {
        open("https://localhost:8081/login");
        $(By.id("usernameoremail")).setValue("");
        $(By.id("password")).setValue("");
        $(By.className("button")).click();
        $(By.className("errormessage")).shouldHave(text("⚠️ All fields are required."));
    }

}

