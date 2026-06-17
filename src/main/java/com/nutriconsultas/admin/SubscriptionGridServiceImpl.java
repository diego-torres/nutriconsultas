package com.nutriconsultas.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionRepository;

@Service
public class SubscriptionGridServiceImpl implements SubscriptionGridService {

	private final SubscriptionRepository subscriptionRepository;

	public SubscriptionGridServiceImpl(final SubscriptionRepository subscriptionRepository) {
		this.subscriptionRepository = subscriptionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Subscription> findPage(final Pageable pageable) {
		return subscriptionRepository.findAll(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public long countAll() {
		return subscriptionRepository.count();
	}

}
