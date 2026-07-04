package com.fund.research.module.researchpool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class SavedFilterCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotBlank(message = "name must not be blank")
    private String name;

    private String summary;

    @NotBlank(message = "condition must not be blank")
    private String condition;

    private Integer hitCount;
}
