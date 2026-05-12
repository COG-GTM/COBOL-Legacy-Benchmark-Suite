package com.portfolio.service.common;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class Db2ExceptionTranslator {

    public DataAccessException translate(SQLException ex) {
        int errorCode = ex.getErrorCode();

        if (errorCode == -803) {
            return new DataIntegrityViolationException("Duplicate key", ex);
        }
        if (errorCode == -911) {
            return new DeadlockLoserDataAccessException("Deadlock detected", ex);
        }
        if (errorCode == -30081) {
            return new DataAccessResourceFailureException("Connection error", ex);
        }

        return new DataAccessResourceFailureException("Database error: " + errorCode, ex);
    }
}
