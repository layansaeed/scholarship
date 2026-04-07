package com.example.beans.repository;

import com.example.beans.model.BeneficiaryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneficiaryJpaRepository extends JpaRepository<BeneficiaryEntity, Long> {

    @Query(
            value = "SELECT b FROM BeneficiaryEntity b " +
                    "WHERE b.rowNum BETWEEN :start AND :end " +
                    "ORDER BY b.nin ASC")
    Page<BeneficiaryEntity> findBeneficiaries(
            Pageable pageable,
            @Param("start") Long start,
            @Param("end") Long end
    );

    //Because now we fetch beneficiaries page by page from repository, not all NINs at once.
    Page<BeneficiaryEntity> findAllByOrderByNinAsc(Pageable pageable);

}