# PokeAPI Application
### Overview
This is a Java-based web application that provides an API for interacting with a Pokémon database. The application includes features for user authentication, data management, and cookie handling.

Project Structure
>PokeApiApplication.java
: Main application class with configuration settings and controller logic.

> PokeApiService.java
: Contains methods for fetching Pokémon and move data from the external API (pokeapi.co).

> PokeApiFileService.java
: Handles file upload and storage for user profile pictures.

> PokeApiDB.java
: Contains database operations, including user creation, deletion, login functionality, and cookie management.

> ConfigManager.java
: Manages configuration files using YAML format.

> WebConfig.java
: Configures resource handlers for serving static files.

> SecurityConfig.java
: Configures security settings for the application.

### Configuration
The application uses a config.yml file for configuration settings. This file is loaded using a ConfigManager class.

### Security
Passwords are hashed using PBKDF2 with a salt and a high number of iterations for security.\
Salts are generated using a secure random number generator.\
User authentication is handled through a combination of username/email and hashed password.\
Security configurations are managed using Spring Security to ensure secure communication and access control.

### Features
- User registration and login
- Secure password storage using PBKDF2
- Cookie-based session management
- File upload and storage for user profile pictures
- Integration with the Pokémon API (pokeapi.co) for fetching Pokémon and move data
- Configuration management via YAML file

### Dependencies
- Java 8 or higher
- Spring Boot for application development
- PostgreSQL for database management
- SnakeYAML for YAML file parsing
- HTTP client for interacting with the Pokémon API
- Spring Security for handling security configurations

### Usage

1. Clone the repository.
```bash
git clone https://github.com/TropicSnowJPR/Pokeapi.git
```

2. Configure the config.yml file with your database settings.
```yaml
password: dbPassword
url: jdbc:postgresql://127.0.0.1:5432/database
username: dbUser
```

3. Set up the PostgreSQL database and ensure it is running. 
4. Build and run the application using Spring Boot. 
5. Access the application via a web browser or API client.


### License
This project is licensed under the MIT License. See the LICENSE file for more information.