package nlgrandtaskmanager.financesevice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.financesevice.config.BusinessException;
import nlgrandtaskmanager.financesevice.dto.*;
import nlgrandtaskmanager.financesevice.model.Transaction;
import nlgrandtaskmanager.financesevice.model.TransactionEvent;
import nlgrandtaskmanager.financesevice.model.TransactionEventType;
import nlgrandtaskmanager.financesevice.model.TransactionType;
import nlgrandtaskmanager.financesevice.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private  final TransactionEventPublisher publisher;

    public TransactionResponse create(
            UUID userId,
            CreateTransactionRequest request
    ) {
        validateCreate(userId, request);
        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUserId(userId);

        transactionRepository.save(transaction);
        publisher.publish(TransactionEvent.builder()
                .eventId(UUID.randomUUID()) // Генерируем ID самого события
                .transactionId(transaction.getId())
                .userId(userId)
                .oldAmount(transaction.getAmount())
                .oldType(transaction.getType())
                .eventType(TransactionEventType.CREATED)
                .occurredAt(Instant.now())
                .build());;
        return transactionMapper.toResponse(transaction);
    }


    public TransactionSummary getSummary(UUID userId) {
        return transactionRepository.getSummary(userId);
    }


    public BalanceResponse getBalance(UUID userId) {
        TransactionSummary ts = transactionRepository.getSummary(userId);
        return new BalanceResponse(ts.income(), ts.expense(), ts.income().subtract(ts.expense()));
    }

    public Page<TransactionResponse> search(
            UUID userId,
            Instant from,
            Instant to,
            TransactionType type,
            String category,
            Pageable pageable
    ) {
        Specification<Transaction> spec = TransactionSpecification.byUser(userId);

        if (from != null) {
            spec = spec.and(TransactionSpecification.fromDate(from));
        }

        if (to != null) {
            spec = spec.and(TransactionSpecification.toDate(to));
        }

        if (type != null) {
            spec = spec.and(TransactionSpecification.byType(type));
        }

        if (category != null) {
            spec = spec.and(TransactionSpecification.byCategory(category));
        }

        return transactionRepository
                .findAll(spec, pageable)
                .map(transactionMapper::toResponse);
    }

    private BigDecimal calculateBalance(UUID userId) {
        TransactionSummary summary = transactionRepository.getSummary(userId);
        return summary.income().subtract(summary.expense());
    }

    public void validateCreate(UUID userId, CreateTransactionRequest request) {
        if (userId == null) {
            throw new BusinessException("UserId is required");
        }

        if (request.getAmount() == null ||
                request.getAmount().signum() <= 0) {
            throw new BusinessException("Amount must be greater than 0");
        }

        if (request.getType() == TransactionType.EXPENSE) {

            if (request.getCategory() == null || request.getCategory().isBlank()) {
                throw new BusinessException("Category is required for expense");
            }

            BigDecimal balance = calculateBalance(userId);

            if (request.getAmount().compareTo(balance) > 0) {
                throw new BusinessException("Not enough balance");
            }
        }
    }
    @Transactional
    public void delete(UUID userId, UUID transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));

        validateDelete(userId, transaction);
        publisher.publish(TransactionEvent.builder()
                .eventId(UUID.randomUUID())
                .transactionId(transaction.getId())
                .userId(userId)
                .oldAmount(transaction.getAmount())
                .oldType(transaction.getType())
                .eventType(TransactionEventType.DELETED)
                .occurredAt(Instant.now())
                .build());

        transactionRepository.delete(transaction);
    }


    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, CreateTransactionRequest request) {

        Transaction existing = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));


        TransactionType oldType = existing.getType();
        BigDecimal oldAmount = existing.getAmount();

        existing.setType(request.getType());
        existing.setAmount(request.getAmount());

        Transaction saved = transactionRepository.save(existing);

        publisher.publishUpdatedEvent(saved, oldType, oldAmount);

        return transactionMapper.toResponse(saved);
    }


    private void validateUpdate(
            UUID userId,
            Transaction existing,
            CreateTransactionRequest request
    ) {

        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("Access denied");
        }

        if (request.getAmount() != null &&
                request.getAmount().signum() <= 0) {
            throw new BusinessException("Amount must be greater than 0");
        }

        if (existing.getType() == TransactionType.EXPENSE) {

            if (request.getCategory() != null &&
                    request.getCategory().isBlank()) {
                throw new BusinessException("Category cannot be empty");
            }

            BigDecimal balance = calculateBalance(userId)
                    .add(existing.getAmount());

            BigDecimal newAmount = request.getAmount() != null
                    ? request.getAmount()
                    : existing.getAmount();

            if (newAmount.compareTo(balance) > 0) {
                throw new BusinessException("Not enough balance");
            }
        }
    }

   private  void validateDelete(UUID userId, Transaction transaction){
        if (!transaction.getUserId().equals(userId)){
            throw  new BusinessException("Access denied");
        }
   }
}
