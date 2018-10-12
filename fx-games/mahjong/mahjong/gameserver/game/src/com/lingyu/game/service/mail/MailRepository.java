package com.lingyu.game.service.mail;

import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.Mail;
import com.lingyu.noark.data.repository.MultiCacheRepository;

@Repository
public class MailRepository extends MultiCacheRepository<Mail, Long> {

}
