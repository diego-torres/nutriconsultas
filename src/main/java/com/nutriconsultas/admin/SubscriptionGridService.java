package com.nutriconsultas.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nutriconsultas.subscription.Subscription;

public interface SubscriptionGridService {

	Page<Subscription> findPage(Pageable pageable);

	long countAll();

}
