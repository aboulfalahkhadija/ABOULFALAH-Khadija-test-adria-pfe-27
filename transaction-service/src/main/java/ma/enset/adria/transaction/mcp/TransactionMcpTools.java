package ma.enset.adria.transaction.mcp;

import lombok.RequiredArgsConstructor;
import ma.enset.adria.transaction.domain.Transaction;
import ma.enset.adria.transaction.service.TransactionService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RestController
@RequestMapping("/mcp/tools")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionMcpTools {

    private final TransactionService transactionService;

    @McpTool(description = "Get all bank transactions")
    @GetMapping("/transactions")
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @McpTool(description = "Get transactions for a specific bank account UUID")
    @GetMapping("/transactions/account/{accountId}")
    public List<Transaction> getTransactionsByAccount(
            @McpToolParam(description = "The bank account UUID") @PathVariable UUID accountId
    ) {
        return transactionService.getTransactionsByAccountId(accountId);
    }

    @McpTool(description = "Record a new bank transaction and update account balance")
    @PostMapping("/transactions")
    public Transaction createBankTransaction(
            @McpToolParam(description = "Account UUID") @RequestParam UUID accountId,
            @McpToolParam(description = "Transaction amount (strictly positive)") @RequestParam BigDecimal amount,
            @McpToolParam(description = "Currency matching the account (e.g. EUR, MAD)") @RequestParam String currency,
            @McpToolParam(description = "Merchant category (e.g. GROCERY, ECOMMERCE)") @RequestParam String merchantCategory,
            @McpToolParam(description = "Country code (e.g. MA, FR)") @RequestParam String country
    ) {
        return transactionService.createTransaction(accountId, amount, currency, merchantCategory, country);
    }

    @McpTool(description = "Transfer money from source account to target account")
    @PostMapping("/transactions/transfer")
    public List<Transaction> transferBetweenAccounts(
            @McpToolParam(description = "Source account UUID") @RequestParam UUID sourceAccountId,
            @McpToolParam(description = "Target account UUID") @RequestParam UUID targetAccountId,
            @McpToolParam(description = "Amount to transfer") @RequestParam BigDecimal amount,
            @McpToolParam(description = "Currency (e.g. EUR, MAD)") @RequestParam String currency
    ) {
        return transactionService.transfer(sourceAccountId, targetAccountId, amount, currency);
    }

    @GetMapping("/manifest")
    public ResponseEntity<Map<String, Object>> getMcpManifest() {
        return ResponseEntity.ok(Map.of(
                "server", "transaction-service-mcp",
                "protocol", "streamable",
                "tools", List.of(
                        Map.of("name", "getAllTransactions", "description", "Get all bank transactions"),
                        Map.of("name", "getTransactionsByAccount", "description", "Get transactions for a specific bank account UUID"),
                        Map.of("name", "createBankTransaction", "description", "Record a new bank transaction and update account balance"),
                        Map.of("name", "transferBetweenAccounts", "description", "Transfer money from source account to target account")
                )
        ));
    }
}
