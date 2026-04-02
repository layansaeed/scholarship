package com.example.beans.model;

import javax.persistence.*;
import lombok.*;

/**
 * JPA entity that maps to dbo.SERVICES_ANALYTICS_TABLE_INT.
 * Used only to read Beneficiary NIN(s) from the database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "BENEF_NINS", schema = "ETL")
public class BeneficiaryEntity {

    @Id
    @Column(name = "nin")
    private Long nin;

//    @Column(name = "DOB")
//    private String birthDate;
}