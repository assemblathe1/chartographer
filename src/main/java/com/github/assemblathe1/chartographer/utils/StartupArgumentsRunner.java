package com.github.assemblathe1.chartographer.utils;

import lombok.Data;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Data
@Component
public class StartupArgumentsRunner implements CommandLineRunner {
    private String folder;
    @Override
    public void run(String... args) {
        this.folder = Arrays.deepToString(args);
    }
}
