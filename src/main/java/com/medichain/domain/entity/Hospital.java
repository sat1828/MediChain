package com.medichain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hospital", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class Hospital extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "registration_number", unique = true, length = 50)
    private String registrationNumber;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pharmacy_license_number", length = 50)
    private String pharmacyLicenseNumber;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "bed_count")
    private Integer bedCount;

    @OneToMany(mappedBy = "hospital")
    private List<Ward> wards = new ArrayList<>();

    @OneToMany(mappedBy = "hospital")
    private List<User> users = new ArrayList<>();
}
