package com.school.backend.fee.service;

import com.school.backend.fee.enums.FundingCoverageMode;
import com.school.backend.fee.enums.FundingCoverageType;
import com.school.backend.fee.entity.StudentFundingArrangement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeCalculationService {

    public BigDecimal calculateNetPrincipal(BigDecimal amount, BigDecimal discount, BigDecimal funding) {
        BigDecimal net = amount.subtract(discount).subtract(funding);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : net.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFundingSnapshot(BigDecimal baseAmount, BigDecimal discountAmount,
            StudentFundingArrangement funding) {
        if (funding == null || !funding.isActive()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (funding.getCoverageType() == FundingCoverageType.FULL) {
            return baseAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal netAfterDiscount = baseAmount.subtract(discountAmount);
        BigDecimal fundingAmount = BigDecimal.ZERO;

        if (funding.getCoverageMode() == FundingCoverageMode.FIXED_AMOUNT) {
            fundingAmount = funding.getCoverageValue();
        } else if (funding.getCoverageMode() == FundingCoverageMode.PERCENTAGE) {
            fundingAmount = netAfterDiscount.multiply(funding.getCoverageValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Cap funding to netAfterDiscount
        if (fundingAmount.compareTo(netAfterDiscount) > 0) {
            fundingAmount = netAfterDiscount;
        }

        return fundingAmount.setScale(2, RoundingMode.HALF_UP);
    }
}
