package com.zohebrahiman.cowsandbulls.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
