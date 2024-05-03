package com.safa.payment.dto.common;


import lombok.Data;

import java.util.Date;

@Data
public class BaseEntityDto {

    private Long id;

    private Date createdAt;

    private Date updatedAt;
}
