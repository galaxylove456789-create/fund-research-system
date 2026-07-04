package com.fund.research.module.compare.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CompareCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotEmpty(message = "fundIds must not be empty")
    @Size(min = 2, max = 10, message = "fundIds size must be between 2 and 10")
    private List<Long> fundIds;

    private String compareDimension = "SCORE,RETURN,RISK";
}
