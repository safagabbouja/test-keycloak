package com.keycloakk.example.keycloak_exemple;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;

@RestController
public class MyRestController {

    @RequestMapping("/cars")
    @PreAuthorize("hasAuthority('ROLE_view-cars')")
    public List<CarEntity> getCars(){
        return Arrays.asList(
                new CarEntity("Audi", "white", 22000000L),
                new CarEntity("BMW", "black", 24500000L),
                new CarEntity("Mercedes", "silver", 45000000L));
    }

    @RequestMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_admin-role')")
    public String adminEndpoint(){
        return "Welcome Admin!";
    }
}
