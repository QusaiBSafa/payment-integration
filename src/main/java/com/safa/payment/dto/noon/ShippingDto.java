package com.safa.payment.dto.noon;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ShippingDto {

    private AddressDto address;

    private ContactDto contact;
}
