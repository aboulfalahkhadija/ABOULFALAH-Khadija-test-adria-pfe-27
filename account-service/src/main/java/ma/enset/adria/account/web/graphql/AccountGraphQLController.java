package ma.enset.adria.account.web.graphql;

import lombok.RequiredArgsConstructor;
import ma.enset.adria.account.domain.Account;
import ma.enset.adria.account.domain.AccountStatus;
import ma.enset.adria.account.service.AccountService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountGraphQLController {

    private final AccountService accountService;

    @QueryMapping
    public List<Account> accounts() {
        return accountService.getAllAccounts();
    }

    @QueryMapping
    public Account accountById(@Argument UUID id) {
        return accountService.getAccountById(id);
    }

    @MutationMapping
    public Account createAccount(
            @Argument String customerReference,
            @Argument String currency,
            @Argument double balance,
            @Argument AccountStatus status
    ) {
        return accountService.createAccount(customerReference, currency, balance, status);
    }

    @MutationMapping
    public Account updateAccountStatus(@Argument UUID id, @Argument AccountStatus status) {
        return accountService.updateAccountStatus(id, status);
    }

    @MutationMapping
    public Account debitAccount(@Argument UUID id, @Argument double amount) {
        return accountService.debitAccount(id, amount);
    }

    @MutationMapping
    public Account creditAccount(@Argument UUID id, @Argument double amount) {
        return accountService.creditAccount(id, amount);
    }
}
