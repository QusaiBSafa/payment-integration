package com.safa.payment.dto.telr;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AddressDto {

    private String line1;

    private String city;

    private String country;

}
