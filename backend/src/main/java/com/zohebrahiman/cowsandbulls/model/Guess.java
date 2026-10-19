package com.zohebrahiman.cowsandbulls.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Guess {

	@Id
	@GeneratedValue
	private Long id;

	@NonNull
	private String name;

	private Integer cows;

	private Integer bulls;
}
