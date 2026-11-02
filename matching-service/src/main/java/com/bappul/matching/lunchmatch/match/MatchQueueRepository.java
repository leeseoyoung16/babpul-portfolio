package com.bappul.matching.lunchmatch.match;

import org.springframework.data.repository.CrudRepository;

public interface MatchQueueRepository extends CrudRepository<MatchQueue, String> {
}