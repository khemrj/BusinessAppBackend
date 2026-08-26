package com.example.firstapp.dto;

import com.example.firstapp.entity.Address;

public record AddressRequest(
        String street,
        String city,
        String state,
        String postalCode,
        String country
) {

    public Address toEntity() {
        return Address.builder()
                .street(street)
                .city(city)
                .state(state)
                .postalCode(postalCode)
                .country(country)
                .build();
    }
}