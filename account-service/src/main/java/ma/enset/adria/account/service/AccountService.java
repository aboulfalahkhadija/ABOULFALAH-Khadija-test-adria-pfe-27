package ma.enset.adria.account.service;

import ma.enset.adria.account.domain.Account;
import ma.enset.adria.account.domain.AccountStatus;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    List<Account> getAllAccounts();
    Account getAccountById(UUID id);
    List<Account> getAccountsByCustomerReference(String customerReference);
    Account createAccount(String customerReference, String currency, double initialBalance, AccountStatus status);
    Account updateAccountStatus(UUID id, AccountStatus status);
    Account debitAccount(UUID id, double amount);
    Account creditAccount(UUID id, double amount);
}
