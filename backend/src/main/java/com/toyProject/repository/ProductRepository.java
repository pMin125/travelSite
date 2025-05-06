package com.toyProject.repository;

import com.toyProject.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.tags")
    List<Product> findAllWithTags();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    Optional<Product> findByProductName(String testProduct);

    @Query("SELECT p FROM Product p WHERE FUNCTION('DATE', p.endDate) = :targetDate AND p.capacity > 0")
    List<Product> findExpiringProducts(@Param("targetDate") LocalDate targetDate);
}
