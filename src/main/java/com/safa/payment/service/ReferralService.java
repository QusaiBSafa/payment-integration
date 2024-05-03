package com.safa.payment.service;

import com.safa.payment.dto.*;
import com.safa.payment.dto.common.ConsultationIncomingDto;
import com.safa.payment.dto.common.ReferralIncomingDto;
import com.safa.payment.dto.common.ReferralOutgoingDto;
import com.safa.payment.dto.common.RewardsBalanceOutgoingDto;
import com.safa.payment.entity.Referral;
import com.safa.payment.entity.ReferralProgram;
import com.safa.payment.entity.RewardType;
import com.safa.payment.entity.RewardsBalance;
import com.safa.payment.repository.ReferralProgramRepository;
import com.safa.payment.repository.ReferralRepository;
import com.safa.payment.repository.RewardsBalanceRepository;
import com.safa.payment.util.PaymentUtil;
import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handle the communication between referral controller and referral repositories
 * Referral code is saved in referral program in 2 ways
 * 1- Save it as characters only like this AAA
 * 2- Save it with numbers such as AAA
 * when the referral code is applied we check if we have program with the referral code entered, or if we have a program with the first character parts
 * This will help us to support both a referral code with a real user id attached to it, and a referral code connected with a referral id in referral program table
 *
 * @author Qusai Safa
 */
@Service
@Transactional
public class ReferralService {

    private final Logger logger = LoggerFactory.getLogger(ReferralService.class);

    private final ReferralRepository referralRepository;
    private final RewardsBalanceRepository rewardsBalanceRepository;
    private final ReferralProgramRepository referralProgramRepository;

    private static final String REFERRER_BALANCE_REWARD_DESCRIPTION = "Claimed when referrer code %s is used by user id %s";

    private final ModelMapper modelMapper;
    private static final String REFEREE_BALANCE_REWARD_DESCRIPTION = "Claimed when referrer code %s is used";
    private final ApplicationEventPublisher eventPublisher;

    private final PaymentUtil paymentUtil;

    @Autowired
    public ReferralService(ReferralRepository referralRepository, RewardsBalanceRepository rewardsBalanceRepository, ReferralProgramRepository referralProgramRepository, ApplicationEventPublisher eventPublisher, ModelMapper modelMapper, PaymentUtil paymentUtil) {
        this.referralRepository = referralRepository;
        this.rewardsBalanceRepository = rewardsBalanceRepository;
        this.referralProgramRepository = referralProgramRepository;
        this.eventPublisher = eventPublisher;
        this.modelMapper = modelMapper;
        this.paymentUtil = paymentUtil;
    }


    private static String[] extractReferralCode(String referralCode) {
        // Extract referral program and referrer id, EX: AAA123 it will be extracted to ["AAA",1234]
        String[] parts = referralCode.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        if (parts.length != 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, String.format("Invalid referral code (%s)", referralCode));
        }
        return parts;
    }

    /**
     * Apply referral code
     * 1- Extract referrer id, and program lookup code from referral code
     * 2- validate referral program
     * 3- create and save new referral
     * 5- send new referral event
     * 4- map to outgoing dto
     */
    public ReferralOutgoingDto applyReferralCode(ReferralIncomingDto referralIncomingDto, long userId) {

        if (StringUtils.isEmpty(referralIncomingDto.getReferralCode()) || referralIncomingDto.getReferralCode().length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid referral code");
        }

        if (referralRepository.findByRefereeId(userId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, String.format("This user(%s) is already used a referral code", userId));
        }

        String referralCode = referralIncomingDto.getReferralCode();

        Optional<ReferralProgram> referralProgramOptional = referralProgramRepository.findByReferralCode(referralCode);
        Long referrerId = null;
        ReferralProgram referralProgram = null;
        // In case the referral code is same as a referral program lookup code then get the referrer id from the referral program saved record
        if (referralProgramOptional.isPresent()) {
            referralProgram = referralProgramOptional.get();
            referrerId = referralProgram.getReferrerUserId();
        } else {// In case the referral code is 2 parts, referral lookup code + user Id
            String[] parts = extractReferralCode(referralCode);
            // Referral program lookup code (a reference to a referral program)
            String programLookupCode = parts[0];
            // User id for the user who owns the referral code.
            String referrerIdPart = parts[1];

            if (StringUtils.isEmpty(programLookupCode)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, String.format("Empty referral program, referral code(%s)", referralCode));
            }

            referralProgram = referralProgramRepository.findByReferralCode(programLookupCode).orElseThrow(() -> {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, String.format("Invalid referral program, referral lookup code(%s)", programLookupCode));
            });

            try {
                referrerId = Long.parseLong(referrerIdPart);
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, String.format("Failed while extracting referrer id, referral code (%s)", referralCode));
            }
        }

        // Create new referral record.
        Referral referral = createReferral(referralCode, referralProgram.getId(), referrerId, userId);
        // Map referral to outgoing dto
        var referralOutgoingDto = mapToReferralOutgoingDto(referral);
        // Send referral applied event(it is used by warehouse and other services such as PH BE)
        eventPublisher.publishEvent(referralOutgoingDto);
        return referralOutgoingDto;
    }

    /**
     * Create and save referral record
     */
    private Referral createReferral(String referralCode, long referralProgramId, long referrerId, long refereeId) {
        Referral referral = new Referral();
        referral.setReferralCode(referralCode);
        referral.setReferralProgramId(referralProgramId);
        referral.setReferrerId(referrerId);
        referral.setRefereeId(refereeId);
        return referralRepository.save(referral);
    }

    /**
     * Listen to spring internal event which is published by the consumer service after consuming the consultation kafak event
     */
    @Async
    @EventListener(ConsultationIncomingDto.class)
    public void consumeConsultationReferralEvent(ConsultationIncomingDto consultationIncomingDto) {
        Optional<Referral> referralOptional = referralRepository.findByRefereeId(consultationIncomingDto.getUserId());
        if (referralOptional.isEmpty()) {
            return;
        }
        Referral referral = referralOptional.get();
        ReferralProgram referralProgram = referralProgramRepository.findById(referral.getReferralProgramId()).get();

        // Add reward to referrer, if he/she claimed less than referrerNumberOfRewards
        long referrerNumberOfRewards = rewardsBalanceRepository.countByUserId(referral.getReferrerId());
        if (referrerNumberOfRewards < referralProgram.getReferrerNumberOfRewards()) {
            RewardsBalance rewardsBalance = createRewardBalance(referral.getReferrerId(), referralProgram.getReferrerRewardType(), referralProgram.getReferrerRewardValue(), String.format(REFERRER_BALANCE_REWARD_DESCRIPTION, referral.getReferralCode(), referral.getRefereeId()));
            RewardsBalanceOutgoingDto rewardsBalanceReportingDtp = paymentUtil.createRewardingBalanceReportingDto(rewardsBalance, referral.getReferralCode(), true);
            // Send warehouse event
            eventPublisher.publishEvent(rewardsBalanceReportingDtp);
        }

        // Add reward to referee if he/she claimed less than refereeNumberOfRewards
        long refereeNumberOfRewards = rewardsBalanceRepository.countByUserId(referral.getRefereeId());
        if (refereeNumberOfRewards < referralProgram.getRefereeNumberOfRewards()) {
            RewardsBalance rewardsBalance = createRewardBalance(referral.getRefereeId(), referralProgram.getRefereeRewardType(), referralProgram.getRefereeRewardValue(), String.format(REFEREE_BALANCE_REWARD_DESCRIPTION, referral.getReferralCode()));
            RewardsBalanceOutgoingDto rewardsBalanceReportingDtp = paymentUtil.createRewardingBalanceReportingDto(rewardsBalance, referral.getReferralCode(), false);
            NotificationTransactionDto notificationTransactionDto = paymentUtil.createReferralNotificationDto(rewardsBalance);
            // Send notification
            eventPublisher.publishEvent(notificationTransactionDto);
            // Send warehouse event
            eventPublisher.publishEvent(rewardsBalanceReportingDtp);

        }
    }

    private RewardsBalance createRewardBalance(long userId, RewardType rewardType, String rewardValue, String description) {
        RewardsBalance rewardsBalance = new RewardsBalance();
        rewardsBalance.setUserId(userId);
        rewardsBalance.setRewardType(rewardType);
        rewardsBalance.setValue(rewardValue);
        rewardsBalance.setDescription(description);
        return rewardsBalanceRepository.save(rewardsBalance);
    }

    public List<RewardsBalanceOutgoingDto> getRewardsBalanceByUserId(long userId, Pageable pageable) {
        List<RewardsBalance> rewardsBalances = rewardsBalanceRepository.findByUserId(userId, pageable);
        if (CollectionUtils.isEmpty(rewardsBalances)) {
            return Collections.emptyList();
        }

        return rewardsBalances.stream().map(this::mapToRewardsBalanceOutgoingDto).collect(Collectors.toList());
    }

    private ReferralOutgoingDto mapToReferralOutgoingDto(Referral referral) {
        return modelMapper.map(referral, ReferralOutgoingDto.class);
    }

    private RewardsBalanceOutgoingDto mapToRewardsBalanceOutgoingDto(RewardsBalance rewardsBalance) {
        return modelMapper.map(rewardsBalance, RewardsBalanceOutgoingDto.class);
    }
}
