package com.safa.payment.dto.noon;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AddressDto {

    private String street;

    private String city;

    private String country;
     

}
