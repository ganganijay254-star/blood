package com.bloodcare.bloodcare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;

public interface DonorRepository extends JpaRepository<Donor, Long> {

    Donor findByUser(User user);

    List<Donor> findByBloodGroupIgnoreCaseAndCityIgnoreCaseAndAvailableTrue(String bloodGroup, String city);

    List<Donor> findByBloodGroupIgnoreCaseAndAvailableTrue(String bloodGroup);

    List<Donor> findByCityIgnoreCaseAndAvailableTrue(String city);

    List<Donor> findByAvailableTrue();
}
