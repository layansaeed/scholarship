package com.example.beans.repository;

import com.example.beans.model.BeneficiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryJpaRepository extends JpaRepository<BeneficiaryEntity, Long> {

    @Query("select b.nin from BeneficiaryEntity b order by b.nin asc")
    List<Long> findAllNinsSorted();

}