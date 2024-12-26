package com.example.demo.service;

import com.example.demo.dto.ReservationResponseDto;
import com.example.demo.entity.Item;
import com.example.demo.entity.RentalLog;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.User;
import com.example.demo.exception.ReservationConflictException;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class ReservationService {
  private final ReservationRepository reservationRepository;
  private final ItemRepository itemRepository;
  private final UserRepository userRepository;
  private final RentalLogService rentalLogService;

  public ReservationService(ReservationRepository reservationRepository,
                            ItemRepository itemRepository,
                            UserRepository userRepository,
                            RentalLogService rentalLogService) {
    this.reservationRepository = reservationRepository;
    this.itemRepository = itemRepository;
    this.userRepository = userRepository;
    this.rentalLogService = rentalLogService;
  }

  // TODO: 1. 트랜잭션 이해
//  - 개선
//    - `createReservation` 함수 내에서
//        - 하나라도 에러가 발생하면 모두 저장되지 않도록 수정합니다.
//        - 하나라도 에러가 발생하지 않으면 모두 저장되도록 수정합니다.
//        - All or Nothing
  @Transactional
  public void createReservation(Long itemId, Long userId, LocalDateTime startAt, LocalDateTime endAt) {
    // 쉽게 데이터를 생성하려면 아래 유효성검사 주석 처리
    List<Reservation> haveReservations = reservationRepository.findConflictingReservations(itemId, startAt, endAt);
    if (!haveReservations.isEmpty()) {
      throw new ReservationConflictException("해당 물건은 이미 그 시간에 예약이 있습니다.");
    }
    //repository 코드 분리
    Item item = itemRepository.findItemById(itemId);
    User user = userRepository.findUserById(userId);
    Reservation reservation = new Reservation(item, user, "PENDING", startAt, endAt);
    Reservation savedReservation = reservationRepository.save(reservation);

    RentalLog rentalLog = new RentalLog(savedReservation, "로그 메세지", "CREATE");
    rentalLogService.save(rentalLog);
  }

  // TODO: 3. N+1 문제
//  - 개선
//    - 동일한 데이터를 가져올 때 N+1 문제가 발생하지 않게 수정합니다.
  public List<ReservationResponseDto> getReservations() {
    List<Reservation> reservations = reservationRepository.findAllWithUserAndItem();

    return reservations.stream().map(reservation ->

        new ReservationResponseDto(
            reservation.getId(),
            reservation.getUser().getNickname(),
            reservation.getItem().getName(),
            reservation.getStartAt(),
            reservation.getEndAt()
        )).toList();
  }

  /*
  implementation "com.querydsl:querydsl-jpa:5.0.0:jakarta"
  annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
  annotationProcessor "jakarta.annotation:jakarta.annotation-api"
  annotationProcessor "jakarta.persistence:jakarta.persistence-api"
  build.gradle dependencies 추가
  */
  // TODO: 5. QueryDSL 검색 개선
//  - 개선
//    - `QueryDSL`을 활용하여 동적 쿼리를 적용합니다.
//    - N+1 문제가 발생하지 않도록 합니다.
  public List<ReservationResponseDto> searchAndConvertReservations(Long userId, Long itemId) {

    List<Reservation> reservations = reservationRepository.searchReservations(userId, itemId);

    return convertToDto(reservations);
  }


  private List<ReservationResponseDto> convertToDto(List<Reservation> reservations) {
    return reservations.stream()
        .map(reservation -> new ReservationResponseDto(
            reservation.getId(),
            reservation.getUser().getNickname(),
            reservation.getItem().getName(),
            reservation.getStartAt(),
            reservation.getEndAt()
        ))
        .toList();
  }

  // TODO: 7. 리팩토링
//  - 개선
//    1. 필요하지 않은 `else` 구문을 걷어냅니다.
//      2. 컨트롤러 응답 데이터 타입을 적절하게 변경합니다.
//      3. 재사용 비중이 높은 `findById` 함수들을 `default` 메소드로 선언합니다.
//      4. 상태 값을 명확하게 `enum`으로 관리합니다.
//      5. 첫번째 Transactional 문제를 해결했다면 `RentalLogService` save 함수 내 19~21번째 코드를 삭제하거나 주석처리하여 기능이 동작하도록 수정합니다.
  @Transactional
  public void updateReservationStatus(Long reservationId, String status) {
    Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("해당 ID에 맞는 데이터가 존재하지 않습니다."));

    if ("APPROVED".equals(status) && !"PENDING".equals(reservation.getStatus())) {
      throw new IllegalArgumentException("PENDING 상태만 APPROVED로 변경 가능합니다.");

    }
    if ("CANCELED".equals(status) && "EXPIRED".equals(reservation.getStatus())) {
      throw new IllegalArgumentException("EXPIRED 상태인 예약은 취소할 수 없습니다.");

    }
    if ("EXPIRED".equals(status) && !"PENDING".equals(reservation.getStatus())) {
      throw new IllegalArgumentException("PENDING 상태만 EXPIRED로 변경 가능합니다.");

    }
    reservation.updateStatus(status);
  }
}
