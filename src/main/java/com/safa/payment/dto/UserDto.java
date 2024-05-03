package com.safa.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class UserDto {

    private Long id;

    private String fullName;

    private MoneyDto amount;

    private String country;

    private String city;

    private String formattedAddress;

    private String phoneNumber;

    private String email;
}
