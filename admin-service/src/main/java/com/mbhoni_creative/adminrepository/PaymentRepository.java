package com.mbhoni_creative.adminrepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminentity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceOrderByPaidAtDesc(Invoice invoice);
}