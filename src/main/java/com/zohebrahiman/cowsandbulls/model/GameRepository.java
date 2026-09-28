package com.zohebrahiman.cowsandbulls.model;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

	/**
	 * The game currently being played. Ids ascend, so the newest game is the
	 * one the most recent POST /api/game created.
	 */
	Optional<Game> findFirstByOrderByIdDesc();
}
