package com.qandding.global.common.paging;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageResponse<T> {
	private final List<T> content;
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;

	public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
