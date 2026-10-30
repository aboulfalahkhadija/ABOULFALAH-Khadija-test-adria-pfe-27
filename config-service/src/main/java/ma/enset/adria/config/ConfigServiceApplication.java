package ma.enset.adria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class ConfigServiceApplication {

    @Value("${bank.transaction.max-amount:1000}")
    private int maxAmount;

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

    @GetMapping("/{application}/{profile}")
    public Map<String, Object> getConfiguration(
            @PathVariable String application,
            @PathVariable String profile
    ) {
        return Map.of(
                "name", application,
                "profiles", List.of(profile),
                "propertySources", List.of(
                        Map.of(
                                "name", "classpath:/config-repo/" + application + ".properties",
                                "source", Map.of(
                                        "bank.transaction.max-amount", maxAmount
                                )
                        )
                )
        );
    }

    @GetMapping("/config/params")
    public Map<String, Object> getSharedParams() {
        return Map.of("bank.transaction.max-amount", maxAmount);
    }
}
