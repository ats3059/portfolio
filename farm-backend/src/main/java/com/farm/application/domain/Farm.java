package com.farm.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "farm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Farm {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40)
  private String farmNo;

  @Column(nullable = false, length = 30)
  private String farmerName;

  @Column(nullable = false, length = 20)
  private String phoneNumber;

  public Farm(String farmNo, String farmerName, String phoneNumber) {
    this.farmNo = farmNo;
    this.farmerName = farmerName;
    this.phoneNumber = phoneNumber;
  }
}
