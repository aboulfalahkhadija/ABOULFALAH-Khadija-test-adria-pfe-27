package ma.enset.adria.account;

import ma.enset.adria.account.domain.Account;
import ma.enset.adria.account.domain.AccountStatus;
import ma.enset.adria.account.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.UUID;

@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(AccountRepository accountRepository) {
        return args -> {
            if (accountRepository.count() == 0) {
                Account acc1 = Account.builder()
                        .customerReference("CUST-ADRIA-001")
                        .status(AccountStatus.ACTIVE)
                        .currency("EUR")
                        .balance(5000.00)
                        .build();

                Account acc2 = Account.builder()
                        .customerReference("CUST-ADRIA-002")
                        .status(AccountStatus.ACTIVE)
                        .currency("EUR")
                        .balance(2500.00)
                        .build();

                Account acc3 = Account.builder()
                        .customerReference("CUST-ADRIA-003")
                        .status(AccountStatus.BLOCKED)
                        .currency("MAD")
                        .balance(1200.00)
                        .build();

                accountRepository.saveAll(List.of(acc1, acc2, acc3));
            }
        };
    }
}
