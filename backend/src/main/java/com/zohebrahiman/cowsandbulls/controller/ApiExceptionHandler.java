package com.zohebrahiman.cowsandbulls.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zohebrahiman.cowsandbulls.model.DuplicateGuessException;
import com.zohebrahiman.cowsandbulls.model.GameOverException;
import com.zohebrahiman.cowsandbulls.service.GameNotFoundException;
import com.zohebrahiman.cowsandbulls.service.NotAWordException;

/**
 * Maps failures to RFC 9457 problem responses instead of 500s.
 *
 * <ul>
 * <li>400: the request is malformed (not four letters, missing, not JSON)</li>
 * <li>404: no such game</li>
 * <li>409: the game is over, or the word was already guessed</li>
 * <li>422: well-formed, but not a word</li>
 * </ul>
 *
 * None of these echo raw client input. That input is unvalidated and may
 * contain CR/LF, and the handled exceptions bypass Spring's default resolver,
 * which would otherwise log the rejected value.
 */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidGuess(MethodArgumentNotValidException e) {
		FieldError error = e.getBindingResult().getFieldError();
		String detail = error != null ? error.getDefaultMessage() : "Invalid guess";
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadableBody(HttpMessageNotReadableException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Send the guess as JSON, for example {\"name\":\"word\"}");
	}

	@ExceptionHandler(GameNotFoundException.class)
	ProblemDetail notFound(GameNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(GameOverException.class)
	ProblemDetail gameOver(GameOverException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(DuplicateGuessException.class)
	ProblemDetail duplicate(DuplicateGuessException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(NotAWordException.class)
	ProblemDetail notAWord(NotAWordException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail concurrentGuess(ObjectOptimisticLockingFailureException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Another guess for this game was submitted at the same time; reload the game and try again");
	}
}
