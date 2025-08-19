package com.qandding.global.common.paging;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(name = "PageResponse", description = "공통 페이징 응답")
public class PageResponse<T> {
	@ArraySchema(arraySchema = @Schema(description = "페이지 컨텐츠"))
	private final List<T> content;
	@Schema(description = "현재 페이지(0-based)", example = "0")
	private final int page;
	@Schema(description = "페이지 크기", example = "20")
	private final int size;
	@Schema(description = "전체 아이템 수", example = "123")
	private final long totalElements;
	@Schema(description = "전체 페이지 수", example = "7")
	private final int totalPages;

	public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
