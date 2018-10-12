package com.lingyu.game.service.mail;

import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.RoleRedeemInfo;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

@Repository
public class RoleRedeemInfoRepository extends UniqueCacheRepository<RoleRedeemInfo, Long>{

}
