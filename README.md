# 🌟 PokeAPI Application

## 📌 Overview

This is a **Java-based web application** that provides an API for interacting with a **Pokémon database**. The application includes the following key features:

- **User authentication**
- **Data management**
- **Cookie handling**
- **Integration with the Pokémon API (pokeapi.co)**

---

## 📁 Project Structure

| File Name              | Description |
|-----------------------|-------------|
| `PokeApiApplication.java` | Main application class with configuration settings and controller logic |
| `PokeApiService.java`     | Contains methods for fetching Pokémon and move data from the external API |
| `PokeApiFileService.java` | Handles file upload and storage for user profile pictures |
| `PokeApiDB.java`          | Contains database operations, including user creation, deletion, login functionality, and cookie management |
| `ConfigManager.java`      | Manages configuration files using YAML format |
| `WebConfig.java`          | Configures resource handlers for serving static files |
| `SecurityConfig.java`     | Configures security settings for the application |

---

## 🔐 Security

- **Password hashing**: Uses **PBKDF2 with a salt** and a **high number of iterations** for security.
- **Salt generation**: Done using a **secure random number generator**.
- **Authentication**: Done via **username/email** and **hashed password**.
- **Security framework**: Uses **Spring Security** for secure communication and access control.

---

## 📦 Features

- ✅ **User registration and login**
- 🔒 **Secure password storage using PBKDF2**
- 🍪 **Cookie-based session management**
- 📸 **File upload and storage for user profile pictures**
- 🐉 **Integration with the Pokémon API (pokeapi.co)** for fetching Pokémon and move data
- 📜 **Configuration management via YAML file**

---

## 🧰 Dependencies

- **Java 8 or higher**
- **Spring Boot** for application development
- **PostgreSQL** for database management
- **SnakeYAML** for YAML file parsing
- **HTTP client** for interacting with the Pokémon API
- **Spring Security** for handling security configurations

---

## 🚀 Usage

1. **Clone the repository**:
   ```bash
   git clone https://github.com/TropicSnowJPR/Pokeapi.git
   ```

2. **Configure the `config.yml` file** with your database settings:
   ```yaml
   password: dbPassword
   url: jdbc:postgresql://127.0.0.1:5432/database
   username: dbUser
   ```

3. **Set up the PostgreSQL database** and ensure it is running.

4. **Build and run the application** using Spring Boot.

5. **Access the application** via a web browser or API client.

---

## 📜 License

This project is licensed under the **MIT License**. See the `LICENSE` file for more information.