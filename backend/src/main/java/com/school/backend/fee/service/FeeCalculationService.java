package com.school.backend.fee.service;

import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.fee.enums.FundingCoverageMode;
import com.school.backend.fee.enums.FundingCoverageType;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static java.math.BigDecimal.ZERO;

@Service
public class FeeCalculationService {

    private static final int SCALE_2 = 2;
    private static final RoundingMode ROUNDING_HALF_UP = RoundingMode.HALF_UP;
    private static final BigDecimal ZERO_HALF_UP = ZERO.setScale(SCALE_2, ROUNDING_HALF_UP);

    private static @NonNull BigDecimal getFundingAmount(BigDecimal baseAmount, BigDecimal discountAmount, StudentFundingArrangement funding) {
        BigDecimal netAfterDiscount = baseAmount.subtract(discountAmount);
        BigDecimal fundingAmount = ZERO;

        if (funding.getCoverageMode() == FundingCoverageMode.FIXED_AMOUNT) {
            fundingAmount = funding.getCoverageValue();
        } else if (funding.getCoverageMode() == FundingCoverageMode.PERCENTAGE) {
            fundingAmount = netAfterDiscount.multiply(funding.getCoverageValue())
                    .divide(BigDecimal.valueOf(100), SCALE_2, ROUNDING_HALF_UP);
        }

        // Cap funding to netAfterDiscount
        if (fundingAmount.compareTo(netAfterDiscount) > 0) {
            fundingAmount = netAfterDiscount;
        }
        return fundingAmount;
    }

    public BigDecimal calculateNetPrincipal(BigDecimal amount, BigDecimal discount, BigDecimal funding) {
        BigDecimal net = amount.subtract(discount).subtract(funding);
        return net.compareTo(ZERO) < 0 ? ZERO_HALF_UP
                : net.setScale(SCALE_2, ROUNDING_HALF_UP);
    }

    public BigDecimal calculateFundingSnapshot(BigDecimal baseAmount, BigDecimal discountAmount,
                                               StudentFundingArrangement funding) {
        if (funding == null || !funding.isActive()) {
            return ZERO_HALF_UP;
        }

        // ✅ Add validity check
        LocalDate today = LocalDate.now();
        if (funding.getValidFrom() != null && today.isBefore(funding.getValidFrom())) {
            return ZERO_HALF_UP;  // Not yet valid
        }
        if (funding.getValidTo() != null && today.isAfter(funding.getValidTo())) {
            return ZERO_HALF_UP; // Expired
        }

        if (funding.getCoverageType() == FundingCoverageType.FULL) {
            return baseAmount.subtract(discountAmount).setScale(SCALE_2, ROUNDING_HALF_UP);
        }

        BigDecimal fundingAmount = getFundingAmount(baseAmount, discountAmount, funding);

        return fundingAmount.setScale(SCALE_2, ROUNDING_HALF_UP);
    }
}
