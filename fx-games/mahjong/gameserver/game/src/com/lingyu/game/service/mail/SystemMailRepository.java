package com.lingyu.game.service.mail;

import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.SystemMail;
import com.lingyu.noark.data.repository.OrmRepository;

@Repository
public class SystemMailRepository extends OrmRepository<SystemMail, Long> {

}
