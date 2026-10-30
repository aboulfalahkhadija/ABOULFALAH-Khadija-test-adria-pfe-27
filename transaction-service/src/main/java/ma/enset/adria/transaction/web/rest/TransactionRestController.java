package ma.enset.adria.transaction.web.rest;

import lombok.RequiredArgsConstructor;
import ma.enset.adria.transaction.domain.Transaction;
import ma.enset.adria.transaction.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionRestController {

    private final TransactionService transactionService;

    public record CreateTransactionRequest(
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantCategory,
            String country
    ) {}

    public record TransferRequest(
            UUID sourceAccountId,
            UUID targetAccountId,
            BigDecimal amount,
            String currency
    ) {}

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable UUID id) {
        return transactionService.getTransactionById(id);
    }

    @GetMapping("/account/{accountId}")
    public List<Transaction> getTransactionsByAccount(@PathVariable UUID accountId) {
        return transactionService.getTransactionsByAccountId(accountId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransaction(@RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(
                request.accountId(),
                request.amount(),
                request.currency(),
                request.merchantCategory(),
                request.country()
        );
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transaction> transfer(@RequestBody TransferRequest request) {
        return transactionService.transfer(
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount(),
                request.currency()
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBusinessException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
