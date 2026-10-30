package ma.enset.adria.transaction.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ma.enset.adria.transaction.domain.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "account-service", url = "${account.service.url:http://localhost:8081}")
public interface AccountFeignClient {

    @GetMapping("/api/accounts/{id}")
    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "getDefaultAccount")
    AccountDTO getAccountById(@PathVariable("id") UUID id);

    @PutMapping("/api/accounts/{id}/debit")
    AccountDTO debitAccount(@PathVariable("id") UUID id, @RequestBody Map<String, Double> request);

    @PutMapping("/api/accounts/{id}/credit")
    AccountDTO creditAccount(@PathVariable("id") UUID id, @RequestBody Map<String, Double> request);

    default AccountDTO getDefaultAccount(UUID id, Exception exception) {
        return AccountDTO.builder()
                .id(id)
                .customerReference("FALLBACK-UNAVAILABLE")
                .status("BLOCKED")
                .currency("N/A")
                .balance(0.0)
                .build();
    }
}
