package com.example.beans.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Entity mapped to the beneficiary view/object used for ranged processing.
 * rowNum is used with audit range start/end.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "BENEF_NINS", schema = "EXT")
public class BeneficiaryEntity {

    @Id
    @Column(name = "ROW_NUM")
    private Long rowNum;

    @Column(name = "NIN")
    private Long nin;
}