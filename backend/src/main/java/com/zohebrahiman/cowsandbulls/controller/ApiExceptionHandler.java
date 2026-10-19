package com.zohebrahiman.cowsandbulls.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zohebrahiman.cowsandbulls.model.GameOverException;
import com.zohebrahiman.cowsandbulls.service.GameNotFoundException;

/**
 * Maps domain failures to RFC 9457 problem responses instead of 500s.
 */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(GameNotFoundException.class)
	ProblemDetail notFound(GameNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(GameOverException.class)
	ProblemDetail gameOver(GameOverException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail concurrentGuess(ObjectOptimisticLockingFailureException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Another guess for this game was submitted at the same time; reload the game and try again");
	}
}
