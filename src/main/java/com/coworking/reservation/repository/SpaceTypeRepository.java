package com.coworking.reservation.repository;

import com.coworking.reservation.domain.entity.SpaceType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceTypeRepository extends JpaRepository<SpaceType, Short> {
}
