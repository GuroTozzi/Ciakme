package com.ciakme.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest registerRequest) {
        User user = userService.CreateUser(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getRole()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/id")
    public ResponseEntity<UserResponse> geById(@PathVariable UUID id){
        Optional<User> userFound = userService.findByID(id);

        return userFound.map(u ->
                        ResponseEntity.ok(UserResponse.from(u))).
                        orElse(ResponseEntity.notFound().build());

    }
}
