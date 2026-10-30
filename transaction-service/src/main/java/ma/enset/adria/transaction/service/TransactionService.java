package ma.enset.adria.transaction.service;

import ma.enset.adria.transaction.domain.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    List<Transaction> getAllTransactions();
    Transaction getTransactionById(UUID id);
    List<Transaction> getTransactionsByAccountId(UUID accountId);
    Transaction createTransaction(UUID accountId, BigDecimal amount, String currency, String merchantCategory, String country);
    List<Transaction> transfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, String currency);
}
