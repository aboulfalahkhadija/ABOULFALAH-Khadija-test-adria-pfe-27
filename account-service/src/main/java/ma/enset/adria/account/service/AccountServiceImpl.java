package ma.enset.adria.account.service;

import lombok.RequiredArgsConstructor;
import ma.enset.adria.account.domain.Account;
import ma.enset.adria.account.domain.AccountStatus;
import ma.enset.adria.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable avec l'ID : " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccountsByCustomerReference(String customerReference) {
        return accountRepository.findByCustomerReference(customerReference);
    }

    @Override
    public Account createAccount(String customerReference, String currency, double initialBalance, AccountStatus status) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Le solde initial ne peut pas être négatif");
        }
        Account account = Account.builder()
                .customerReference(customerReference)
                .currency(currency != null ? currency.toUpperCase() : "MAD")
                .balance(initialBalance)
                .status(status != null ? status : AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    @Override
    public Account updateAccountStatus(UUID id, AccountStatus status) {
        Account account = getAccountById(id);
        account.setStatus(status);
        return accountRepository.save(account);
    }

    @Override
    public Account debitAccount(UUID id, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant à débiter doit être strictement positif");
        }
        Account account = getAccountById(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Opération refusée : le compte " + id + " est " + account.getStatus());
        }
        if (account.getBalance() < amount) {
            throw new IllegalStateException("Solde insuffisant sur le compte " + id + " (Solde actuel : " + account.getBalance() + ")");
        }
        account.setBalance(account.getBalance() - amount);
        return accountRepository.save(account);
    }

    @Override
    public Account creditAccount(UUID id, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant à créditer doit être strictement positif");
        }
        Account account = getAccountById(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Opération refusée : le compte " + id + " est " + account.getStatus());
        }
        account.setBalance(account.getBalance() + amount);
        return accountRepository.save(account);
    }
}
