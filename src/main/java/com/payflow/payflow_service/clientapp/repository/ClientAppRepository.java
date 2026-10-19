package com.payflow.payflow_service.clientapp.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payflow.payflow_service.clientapp.entity.ClientApp;


@Repository 
public interface ClientAppRepository extends JpaRepository<ClientApp, UUID> {

    Optional<ClientApp> findByKeyPrefix(String keyPrefix);
}