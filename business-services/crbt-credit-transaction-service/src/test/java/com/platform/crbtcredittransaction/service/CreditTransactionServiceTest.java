package com.platform.crbtcredittransaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.common.core.exception.BaseException;
import com.platform.common.rmq.event.CreditChangedEvent;
import com.platform.crbtcredittransaction.entity.CreditTransaction;
import com.platform.crbtcredittransaction.repository.CreditTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditTransactionServiceTest {

    @Mock
    private CreditTransactionRepository repository;

    @InjectMocks
    private CreditTransactionService service;

    @Test
    void save_shouldSaveTransaction() {
        CreditChangedEvent event = new CreditChangedEvent(1L, 100, "IN", "Recharge", "REF-1", System.currentTimeMillis());

        service.save(event);

        verify(repository).save(any(CreditTransaction.class));
    }

    @Test
    void query_shouldThrowWhenInvalidDateRange() {
        assertThrows(BaseException.class, () ->
            service.query(1L, null, null, 1000L, 500L, null)
        );
    }
}
