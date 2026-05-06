package nlgrandtaskmanager.reportservise.dto;

import java.math.BigDecimal;

public record BalanceResponse (BigDecimal totalIncome,
                               BigDecimal totalExpense,
                               BigDecimal totalAmount){
}
