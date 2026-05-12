package com.farm.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "farm_cattle")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FarmCattle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "farm_id", nullable = false)
  private Farm farm;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cattle_id", nullable = false)
  private Cattle cattle;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private CattleState state;

  @Column(nullable = false)
  private LocalDateTime registeredAt;

  public FarmCattle(Farm farm, Cattle cattle, CattleState state) {
    this.farm = farm;
    this.cattle = cattle;
    this.state = state;
    this.registeredAt = LocalDateTime.now();
  }
}
