package com.zohebrahiman.cowsandbulls.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {
}
