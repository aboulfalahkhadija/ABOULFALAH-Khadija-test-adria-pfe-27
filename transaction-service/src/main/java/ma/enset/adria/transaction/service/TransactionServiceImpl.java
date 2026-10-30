package ma.enset.adria.transaction.service;

import lombok.RequiredArgsConstructor;
import ma.enset.adria.transaction.domain.AccountDTO;
import ma.enset.adria.transaction.domain.Transaction;
import ma.enset.adria.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountFeignClient accountFeignClient;

    @Value("${bank.transaction.max-amount:1000}")
    private BigDecimal maxAmount;

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable : " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccountId(UUID accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    @Override
    public Transaction createTransaction(UUID accountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        // Règle 1 : Le montant doit être strictement positif et respecter le plafond configuré
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de la transaction doit être strictement positif");
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Le montant dépasse le plafond autorisé (" + maxAmount + ")");
        }

        // Règle 2 : Consultation du compte (doit exister et être actif)
        AccountDTO account = accountFeignClient.getAccountById(accountId);
        if (account == null || "FALLBACK-UNAVAILABLE".equals(account.getCustomerReference())) {
            throw new IllegalStateException("Service compte indisponible (Resilience4J Fallback activé) ou compte inexistant");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Le compte " + accountId + " n'est pas actif (statut : " + account.getStatus() + ")");
        }

        // Règle 3 : La devise de la transaction doit correspondre à celle du compte
        if (currency == null || !currency.equalsIgnoreCase(account.getCurrency())) {
            throw new IllegalArgumentException("La devise de la transaction (" + currency + ") ne correspond pas à celle du compte (" + account.getCurrency() + ")");
        }

        // Règle 4 : Consultation du solde du compte avant enregistrement
        if (BigDecimal.valueOf(account.getBalance()).compareTo(amount) < 0) {
            throw new IllegalStateException("Solde insuffisant sur le compte (Solde : " + account.getBalance() + ")");
        }

        // Enregistrement de la transaction
        Transaction tx = Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .currency(account.getCurrency())
                .merchantCategory(merchantCategory != null ? merchantCategory : "PAYMENT")
                .country(country != null ? country : "MA")
                .createdAt(Instant.now())
                .build();
        Transaction savedTx = transactionRepository.save(tx);

        // Mise à jour du solde du compte après enregistrement
        accountFeignClient.debitAccount(accountId, Map.of("amount", amount.doubleValue()));

        return savedTx;
    }

    @Override
    public List<Transaction> transfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, String currency) {
        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("Les comptes source et destination doivent être différents");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du virement doit être strictement positif");
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Le montant du virement dépasse le plafond autorisé (" + maxAmount + ")");
        }

        AccountDTO source = accountFeignClient.getAccountById(sourceAccountId);
        AccountDTO target = accountFeignClient.getAccountById(targetAccountId);

        if (source == null || "FALLBACK-UNAVAILABLE".equals(source.getCustomerReference()) ||
            target == null || "FALLBACK-UNAVAILABLE".equals(target.getCustomerReference())) {
            throw new IllegalStateException("Service compte indisponible (Resilience4J Fallback activé) ou compte introuvable");
        }
        if (!"ACTIVE".equalsIgnoreCase(source.getStatus()) || !"ACTIVE".equalsIgnoreCase(target.getStatus())) {
            throw new IllegalStateException("Les deux comptes doivent être au statut ACTIVE pour effectuer un virement");
        }
        if (!currency.equalsIgnoreCase(source.getCurrency()) || !currency.equalsIgnoreCase(target.getCurrency())) {
            throw new IllegalArgumentException("La devise du virement doit correspondre aux devises des deux comptes");
        }
        if (BigDecimal.valueOf(source.getBalance()).compareTo(amount) < 0) {
            throw new IllegalStateException("Solde insuffisant sur le compte source");
        }

        Instant now = Instant.now();
        Transaction debitTx = transactionRepository.save(Transaction.builder()
                .accountId(sourceAccountId)
                .amount(amount)
                .currency(currency.toUpperCase())
                .merchantCategory("TRANSFER_OUT")
                .country("MA")
                .createdAt(now)
                .build());

        Transaction creditTx = transactionRepository.save(Transaction.builder()
                .accountId(targetAccountId)
                .amount(amount)
                .currency(currency.toUpperCase())
                .merchantCategory("TRANSFER_IN")
                .country("MA")
                .createdAt(now)
                .build());

        accountFeignClient.debitAccount(sourceAccountId, Map.of("amount", amount.doubleValue()));
        accountFeignClient.creditAccount(targetAccountId, Map.of("amount", amount.doubleValue()));

        return List.of(debitTx, creditTx);
    }
}
