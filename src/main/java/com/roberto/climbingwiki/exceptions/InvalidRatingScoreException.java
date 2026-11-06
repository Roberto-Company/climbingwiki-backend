package com.roberto.climbingwiki.exceptions;

import java.math.BigDecimal;

public class InvalidRatingScoreException extends RuntimeException {

    public InvalidRatingScoreException(BigDecimal score) {
        super("Rating score must be in 0.5 increments between 0 and 5: " + score);
    }
}
