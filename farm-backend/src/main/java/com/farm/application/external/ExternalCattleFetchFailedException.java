package com.farm.application.external;

import lombok.Getter;

/**
 * 외부 이력 시스템에서 단건 cattle 정보를 끝내 가져오지 못한 경우 던지는 sentinel 예외.
 *
 * <p>이 예외가 ComponentParallelCattleFetch 의 .exceptionally() 분기로 흘러가면
 * 해당 cattleNo 는 failedAfterRetry 로 분류되어 chunk 가 PARTIAL/FAILED 로 마감된다.
 * "외부 시스템에 정보가 정말 없는 경우"(notFound, 정상)와 구분하기 위해 별도 타입으로 둔다.
 */
@Getter
public class ExternalCattleFetchFailedException extends RuntimeException {

  private final String cattleNo;

  public ExternalCattleFetchFailedException(String cattleNo, Throwable cause) {
    super("외부 이력 시스템 호출 실패 cattleNo=%s".formatted(cattleNo), cause);
    this.cattleNo = cattleNo;
  }
}
