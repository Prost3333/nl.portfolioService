package nlgrandtaskmanager.financesevice.service;

import nlgrandtaskmanager.financesevice.model.Transaction;
import nlgrandtaskmanager.financesevice.model.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public class TransactionSpecification {

    public static Specification<Transaction> byUser(UUID userId) {
        return (root, query, cb) ->
                cb.equal(root.get("userId"), userId);
    }

    public static Specification<Transaction> fromDate(Instant from) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }
    public static  Specification<Transaction> toDate(Instant to){
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("createdAt"),to);
    }

    public static  Specification<Transaction> byType(TransactionType type){
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("type"),type);
    }

    public static Specification<Transaction> byCategory(String category){
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("category"),category);
    }

}
