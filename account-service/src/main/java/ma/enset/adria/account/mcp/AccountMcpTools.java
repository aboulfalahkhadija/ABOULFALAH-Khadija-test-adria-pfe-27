package ma.enset.adria.account.mcp;

import lombok.RequiredArgsConstructor;
import ma.enset.adria.account.domain.Account;
import ma.enset.adria.account.domain.AccountStatus;
import ma.enset.adria.account.service.AccountService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RestController
@RequestMapping("/mcp/tools")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountMcpTools {

    private final AccountService accountService;

    @McpTool(description = "Get all bank accounts")
    @GetMapping("/accounts")
    public List<Account> getAllBankAccounts() {
        return accountService.getAllAccounts();
    }

    @McpTool(description = "Get a bank account by UUID id")
    @GetMapping("/accounts/{id}")
    public Account getBankAccountById(
            @McpToolParam(description = "The bank account UUID") @PathVariable UUID id
    ) {
        return accountService.getAccountById(id);
    }

    @McpTool(description = "Create a new bank account for a customer")
    @PostMapping("/accounts")
    public Account createBankAccount(
            @McpToolParam(description = "Customer reference (e.g. CUST-001)") @RequestParam String customerReference,
            @McpToolParam(description = "Currency (e.g. EUR, MAD, USD)") @RequestParam String currency,
            @McpToolParam(description = "Initial balance") @RequestParam double initialBalance
    ) {
        return accountService.createAccount(customerReference, currency, initialBalance, AccountStatus.ACTIVE);
    }

    @McpTool(description = "Debit an amount from an active bank account")
    @PostMapping("/accounts/{id}/debit")
    public Account debitBankAccount(
            @McpToolParam(description = "The bank account UUID") @PathVariable UUID id,
            @McpToolParam(description = "Amount to debit") @RequestParam double amount
    ) {
        return accountService.debitAccount(id, amount);
    }

    @McpTool(description = "Credit an amount to an active bank account")
    @PostMapping("/accounts/{id}/credit")
    public Account creditBankAccount(
            @McpToolParam(description = "The bank account UUID") @PathVariable UUID id,
            @McpToolParam(description = "Amount to credit") @RequestParam double amount
    ) {
        return accountService.creditAccount(id, amount);
    }

    @GetMapping("/manifest")
    public ResponseEntity<Map<String, Object>> getMcpManifest() {
        return ResponseEntity.ok(Map.of(
                "server", "account-service-mcp",
                "protocol", "streamable",
                "tools", List.of(
                        Map.of("name", "getAllBankAccounts", "description", "Get all bank accounts"),
                        Map.of("name", "getBankAccountById", "description", "Get a bank account by UUID id"),
                        Map.of("name", "createBankAccount", "description", "Create a new bank account for a customer"),
                        Map.of("name", "debitBankAccount", "description", "Debit an amount from an active bank account"),
                        Map.of("name", "creditBankAccount", "description", "Credit an amount to an active bank account")
                )
        ));
    }
}
