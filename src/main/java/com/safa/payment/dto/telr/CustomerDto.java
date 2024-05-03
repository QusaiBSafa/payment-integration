package com.safa.payment.dto.telr;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CustomerDto {

    private String ref;

    private String email;

    private NameDto name;

    private AddressDto address;

    private String phone;
}
