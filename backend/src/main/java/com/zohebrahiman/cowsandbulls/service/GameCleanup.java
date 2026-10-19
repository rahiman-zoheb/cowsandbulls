package com.zohebrahiman.cowsandbulls.service;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zohebrahiman.cowsandbulls.config.GameLimits;
import com.zohebrahiman.cowsandbulls.model.GameRepository;

/**
 * Deletes games nobody has touched for {@code cowsandbulls.games.ttl}. Without
 * this, every POST /api/games added rows that were never removed (#11).
 */
@Component
public class GameCleanup {

	private static final Logger log = LoggerFactory.getLogger(GameCleanup.class);

	private final GameRepository games;
	private final GameLimits limits;
	private final Clock clock;

	public GameCleanup(GameRepository games, GameLimits limits, Clock clock) {
		this.games = games;
		this.limits = limits;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${cowsandbulls.games.cleanup-interval}",
			initialDelayString = "${cowsandbulls.games.cleanup-interval}")
	@Transactional
	public int deleteExpiredGames() {
		int deleted = games.deleteIdleSince(clock.instant().minus(limits.ttl()));
		if (deleted > 0) {
			log.info("Deleted {} games idle for more than {}", deleted, limits.ttl());
		}
		return deleted;
	}
}
