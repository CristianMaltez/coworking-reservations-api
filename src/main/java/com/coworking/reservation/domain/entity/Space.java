package com.coworking.reservation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "spaces")
public class Space extends UuidEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_type_id", nullable = false)
    private SpaceType spaceType;

    protected Space() {
    }

    public Space(String name, Integer capacity, String location, BigDecimal hourlyRate, SpaceType spaceType) {
        this.name = name;
        this.capacity = capacity;
        this.location = location;
        this.hourlyRate = hourlyRate;
        this.spaceType = spaceType;
    }

    public void update(String name, Integer capacity, String location, BigDecimal hourlyRate, SpaceType spaceType) {
        this.name = name;
        this.capacity = capacity;
        this.location = location;
        this.hourlyRate = hourlyRate;
        this.spaceType = spaceType;
    }

    public String getName() {
        return name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getLocation() {
        return location;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public SpaceType getSpaceType() {
        return spaceType;
    }
}
