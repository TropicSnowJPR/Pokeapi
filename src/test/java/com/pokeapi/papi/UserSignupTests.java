package com.pokeapi.papi;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class UserSignupTests {

    @Test
    public void userCanSignUpSuccessfully() {
        open("https://localhost:8081/signup");
        $(By.id("username")).setValue("newuser123");
        $(By.id("email")).setValue("newuser123@mail.com");
        $(By.id("password")).setValue("StrongPass!23");
        $(By.className("button")).click();
        $(By.id("username")).shouldHave(text("newuser123"));
        $(By.id("user-logout")).click();
    }

    @Test
    public void userCannotSignUpWithExistingEmail() {
        open("https://localhost:8081/signup");
        $(By.id("username")).setValue("anotheruser");
        $(By.id("email")).setValue("admin@mail.com");
        $(By.id("password")).setValue("SomePass!1");
        $(By.className("button")).click();
        $(By.className("errormessage")).shouldHave(text("Email already in use"));
    }

    @Test
    public void userCannotSignUpWithEmptyFields() {
        open("https://localhost:8081/signup");
        $(By.id("username")).setValue("");
        $(By.id("email")).setValue("");
        $(By.id("password")).setValue("");
        $(By.className("button")).click();
        $(By.className("errormessage")).shouldHave(text("All fields are required"));
    }

}
