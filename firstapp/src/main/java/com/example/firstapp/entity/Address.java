package com.example.firstapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Column(
        name = "street",
        length = 200
    )
    private String street;

    @Column(
        name = "city",
        length = 100
    )
    private String city;

    @Column(
        name = "state",
        length = 100
    )
    private String state;

    @Column(
        name = "postal_code",
        length = 20
    )
    private String postalCode;

    @Column(
        name = "country",
        length = 100
    )
    private String country;
}