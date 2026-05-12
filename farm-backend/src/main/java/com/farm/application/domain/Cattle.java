package com.farm.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 이력 시스템에서 내려받은 개체 정보 캐시.
 * 다음 동기화 때 이 테이블을 먼저 뒤져 동일 개체를 다시 받아오지 않게 한다.
 */
@Entity
@Getter
@Table(name = "cattle")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cattle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String cattleNo;

  @Column(nullable = false, length = 10)
  @Enumerated(EnumType.STRING)
  private Gender gender;

  @Column(nullable = false)
  private LocalDate birthDate;

  public Cattle(String cattleNo, Gender gender, LocalDate birthDate) {
    this.cattleNo = cattleNo;
    this.gender = gender;
    this.birthDate = birthDate;
  }

  public enum Gender {
    MALE, FEMALE, UNKNOWN
  }
}
