package ma.enset.adria.account.web.rest;

import lombok.RequiredArgsConstructor;
import ma.enset.adria.account.domain.Account;
import ma.enset.adria.account.domain.AccountStatus;
import ma.enset.adria.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountRestController {

    private final AccountService accountService;

    public record CreateAccountRequest(
            String customerReference,
            String currency,
            double balance,
            AccountStatus status
    ) {}

    public record BalanceOperationRequest(double amount) {}

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable UUID id) {
        return accountService.getAccountById(id);
    }

    @GetMapping("/customer/{customerReference}")
    public List<Account> getByCustomerReference(@PathVariable String customerReference) {
        return accountService.getAccountsByCustomerReference(customerReference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody CreateAccountRequest request) {
        return accountService.createAccount(
                request.customerReference(),
                request.currency(),
                request.balance(),
                request.status()
        );
    }

    @PatchMapping("/{id}/status")
    public Account updateStatus(@PathVariable UUID id, @RequestParam AccountStatus status) {
        return accountService.updateAccountStatus(id, status);
    }

    @PutMapping("/{id}/debit")
    public Account debitAccount(@PathVariable UUID id, @RequestBody BalanceOperationRequest request) {
        return accountService.debitAccount(id, request.amount());
    }

    @PutMapping("/{id}/credit")
    public Account creditAccount(@PathVariable UUID id, @RequestBody BalanceOperationRequest request) {
        return accountService.creditAccount(id, request.amount());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBusinessException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
