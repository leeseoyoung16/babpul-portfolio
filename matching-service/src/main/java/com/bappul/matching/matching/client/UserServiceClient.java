package com.bappul.matching.matching.client;

import com.bappul.matching.matching.dto.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * user-service 연동 Feign Client.
 * TODO (1주차 공통): user-service 분리 완료(Person C) 후 실제 엔드포인트 확정 필요
 */
@FeignClient(name = "user-service", contextId = "matchingInternalUserClient")
public interface UserServiceClient {

    @GetMapping("/api/users/internal-id")
    Long findInternalIdByExternalId(@RequestParam("externalId") String externalId);

    @GetMapping("/api/users/{id}/info")
    UserInfoDto getUserInfo(@PathVariable("id") Long id);
}
