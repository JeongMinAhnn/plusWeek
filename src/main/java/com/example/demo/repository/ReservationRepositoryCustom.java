package com.example.demo.repository;

import com.example.demo.entity.Reservation;

import java.util.List;

public interface ReservationRepositoryCustom {
  List<Reservation> searchReservations(Long userId, Long itemId);

}
