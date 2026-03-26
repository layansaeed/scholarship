package com.example.beans.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity that maps to dbo.SERVICES_ANALYTICS_TABLE_INT.
 * Used only to read Beneficiary NIN(s) from the database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SERVICES_ANALYTICS_TABLE_INT", schema = "dbo")
public class BeneficiaryEntity {

    @Id
    @Column(name = "BEN_ID")
    private Long nin;

    @Column(name = "DOB")
    private String birthDate;
}