package nlgrandtaskmanager.financesevice.dto;

import java.math.BigDecimal;

public record TransactionSummary(BigDecimal income,
                                 BigDecimal expense
                                 ) {
}
