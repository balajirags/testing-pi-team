package com.company.app.campaign.repository;

import com.company.app.campaign.domain.AdAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdAccountRepository extends JpaRepository<AdAccountEntity, UUID> {
}
