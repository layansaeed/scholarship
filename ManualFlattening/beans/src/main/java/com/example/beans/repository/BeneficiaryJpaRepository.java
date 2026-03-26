package com.example.beans.repository;

import com.example.beans.model.BeneficiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryJpaRepository extends JpaRepository<BeneficiaryEntity, Long> {

    BeneficiaryEntity findFirstByOrderByNinAsc();

    List<BeneficiaryEntity> findAllByOrderByNinAsc();

}