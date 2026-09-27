package com.coworking.reservation.repository;

import com.coworking.reservation.domain.entity.Space;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceRepository extends JpaRepository<Space, UUID> {

    @EntityGraph(attributePaths = "spaceType")
    @Query("select s from Space s")
    List<Space> findAllWithSpaceType();

    @EntityGraph(attributePaths = "spaceType")
    @Query("select s from Space s where s.id = :id")
    Optional<Space> findWithSpaceTypeById(UUID id);
}
