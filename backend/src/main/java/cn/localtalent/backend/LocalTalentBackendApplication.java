package cn.localtalent.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LocalTalentBackendApplication {

    public static void main(String[] args) {
        // Guard local runs from ambient DEBUG env vars used by other tools.
        System.setProperty("debug", System.getProperty("debug", "false"));
        SpringApplication.run(LocalTalentBackendApplication.class, args);
    }
}
