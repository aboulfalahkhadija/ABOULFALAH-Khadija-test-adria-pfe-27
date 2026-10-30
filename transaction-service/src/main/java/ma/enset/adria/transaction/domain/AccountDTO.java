package ma.enset.adria.transaction.domain;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {
    private UUID id;
    private String customerReference;
    private String status;
    private String currency;
    private double balance;
}
