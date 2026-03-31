package com.propertize.commons.exception;

/** 409 — a state-machine transition is not allowed. */
public class InvalidStateTransitionException extends BaseException {

    public InvalidStateTransitionException(String entityType, Object currentState, Object targetState) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                entityType + " cannot transition from " + currentState + " to " + targetState,
                entityType, currentState, targetState);
    }
}
