package com.nexuslearn.api;

import org.springframework.boot.SpringApplication;

public class TestNexuslearnApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(NexuslearnApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
