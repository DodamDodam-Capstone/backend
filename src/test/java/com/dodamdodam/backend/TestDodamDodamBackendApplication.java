package com.dodamdodam.backend;

import org.springframework.boot.SpringApplication;

public class TestDodamDodamBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(DodamDodamBackendApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
