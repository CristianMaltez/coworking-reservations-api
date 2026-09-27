package com.coworking.reservation.space.controller;

import com.coworking.reservation.space.dto.CreateSpaceRequest;
import com.coworking.reservation.space.dto.SpaceResponse;
import com.coworking.reservation.space.dto.UpdateSpaceRequest;
import com.coworking.reservation.space.service.SpaceService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public List<SpaceResponse> findAll() {
        return spaceService.findAll();
    }

    @GetMapping("/{id}")
    public SpaceResponse findById(@PathVariable UUID id) {
        return spaceService.findById(id);
    }

    @PostMapping
    public ResponseEntity<SpaceResponse> create(@Valid @RequestBody CreateSpaceRequest request) {
        SpaceResponse response = spaceService.create(request);
        return ResponseEntity.created(URI.create("/api/spaces/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public SpaceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSpaceRequest request) {
        return spaceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        spaceService.delete(id);
    }
}
