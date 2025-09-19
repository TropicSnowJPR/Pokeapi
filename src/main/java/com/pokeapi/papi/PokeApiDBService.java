package com.pokeapi.papi;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;



@Service
@Transactional
@Component
public class PokeApiDBService {

    public static void createUser(UsersRepository usersRepository, String name, String email, String password, String salt) {
        if(!usersRepository.findByName(name).isEmpty() || !usersRepository.findByEmail(email).isEmpty() || password.isEmpty() || salt.isEmpty()) {throw new RuntimeException("Ikd some error");}
        Users user = new Users();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setSalt(salt);
        usersRepository.save(user);
    }

    public static void deleteUser(UsersRepository usersRepository, String name) {
        if(!usersRepository.findByName(name).isEmpty()) {return;}
        usersRepository.deleteByName(name);
    }

    public static void changeUsername(UsersRepository usersRepository, String name, String newName) {
        if(!usersRepository.findByName(newName).isEmpty()) {return;}
        List<Users> users = usersRepository.findByName(name);
        assert users.size() == 1;
        Users user = users.getFirst();
        user.setName(newName);
        usersRepository.save(user);
    }

    public static void changeEmail(UsersRepository usersRepository, String name, String email, String newEmail) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findByEmail(email).isEmpty() || !usersRepository.findByEmail(newEmail).isEmpty()) {return;}
        if(email.equals(newEmail)) {return;}
        List<Users> users = usersRepository.findByName(name);
        assert users.size() == 1;
        Users user = users.getFirst();
        if(!Objects.equals(user.getEmail(), email)) {return;}
        user.setEmail(newEmail);
        usersRepository.save(user);
    }

    public static void changePassword(UsersRepository usersRepository, String name, String password, String salt, String newPassword, String newSalt) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findBySalt(salt).isEmpty() || usersRepository.findByPassword(password).isEmpty()) {return;}
        if(password != newPassword && salt != newSalt) {return;}
        List<Users> users = usersRepository.findByName(name);
        assert users.size() == 1;
        Users user = users.getFirst();
        if(!Objects.equals(user.getPassword(), password) || !Objects.equals(user.getSalt(), salt)) {return;}
        if(!Objects.equals(password,newPassword) || !Objects.equals(salt, newSalt)) {return;}
        user.setPassword(newPassword);
        user.setSalt(newSalt);
        usersRepository.save(user);
    }

}