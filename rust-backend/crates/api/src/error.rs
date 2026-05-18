//! Domain error → HTTP status mapping.
//!
//! Translates internal errors from the domain, database, and validation
//! layers into JSON error responses with appropriate HTTP status codes.
//! Mirrors the error classification in COBOL's ERRPROC/ERRHND copybooks
//! while presenting a REST-friendly interface.

use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::Serialize;

use db::DbError;
use domain::ValidationErrors;

// ---------------------------------------------------------------------------
// API error envelope
// ---------------------------------------------------------------------------

/// Unified API error — returned to clients as JSON.
#[derive(Debug, thiserror::Error)]
pub enum ApiError {
    #[error("validation failed: {0}")]
    Validation(#[from] ValidationErrors),

    #[error("not found: {0}")]
    NotFound(String),

    #[error("unauthorized: {0}")]
    Unauthorized(String),

    #[error("forbidden: {0}")]
    Forbidden(String),

    #[error("conflict: {0}")]
    Conflict(String),

    #[error("database error: {0}")]
    Database(#[from] DbError),

    #[error("internal error: {0}")]
    Internal(String),
}

/// JSON body sent for error responses.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorBody {
    pub status: u16,
    pub error: String,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub details: Option<Vec<FieldError>>,
}

/// Per-field validation error.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FieldError {
    pub field: String,
    pub message: String,
}

// ---------------------------------------------------------------------------
// IntoResponse — maps domain errors to HTTP status codes
// ---------------------------------------------------------------------------

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let (status, details) = match &self {
            ApiError::Validation(v) => {
                let field_errors: Vec<FieldError> = v
                    .errors()
                    .iter()
                    .map(|e| FieldError {
                        field: e.field.clone(),
                        message: e.message.clone(),
                    })
                    .collect();
                (StatusCode::BAD_REQUEST, Some(field_errors))
            }
            ApiError::NotFound(_) => (StatusCode::NOT_FOUND, None),
            ApiError::Unauthorized(_) => (StatusCode::UNAUTHORIZED, None),
            ApiError::Forbidden(_) => (StatusCode::FORBIDDEN, None),
            ApiError::Conflict(_) => (StatusCode::CONFLICT, None),
            ApiError::Database(db_err) => {
                let code = match db_err {
                    DbError::NotFound => StatusCode::NOT_FOUND,
                    DbError::DuplicateKey => StatusCode::CONFLICT,
                    DbError::Deadlock | DbError::Timeout => StatusCode::SERVICE_UNAVAILABLE,
                    _ => StatusCode::INTERNAL_SERVER_ERROR,
                };
                (code, None)
            }
            ApiError::Internal(_) => (StatusCode::INTERNAL_SERVER_ERROR, None),
        };

        let body = ErrorBody {
            status: status.as_u16(),
            error: status.canonical_reason().unwrap_or("Error").to_string(),
            message: self.to_string(),
            details,
        };

        (status, Json(body)).into_response()
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use axum::body::to_bytes;

    fn json_body(resp: Response) -> serde_json::Value {
        let rt = tokio::runtime::Runtime::new().unwrap();
        rt.block_on(async {
            let bytes = to_bytes(resp.into_body(), 1_048_576).await.unwrap();
            serde_json::from_slice(&bytes).unwrap()
        })
    }

    #[test]
    fn validation_error_returns_400() {
        let mut v = ValidationErrors::new();
        v.add("id", "must not be empty");
        v.add("account_no", "exceeds max length 10");
        let resp = ApiError::Validation(v).into_response();
        assert_eq!(resp.status(), StatusCode::BAD_REQUEST);
        let body = json_body(resp);
        assert_eq!(body["status"], 400);
        assert!(body["details"].is_array());
        assert_eq!(body["details"].as_array().unwrap().len(), 2);
    }

    #[test]
    fn not_found_returns_404() {
        let resp = ApiError::NotFound("portfolio ABC".into()).into_response();
        assert_eq!(resp.status(), StatusCode::NOT_FOUND);
        let body = json_body(resp);
        assert_eq!(body["status"], 404);
    }

    #[test]
    fn unauthorized_returns_401() {
        let resp = ApiError::Unauthorized("invalid token".into()).into_response();
        assert_eq!(resp.status(), StatusCode::UNAUTHORIZED);
    }

    #[test]
    fn forbidden_returns_403() {
        let resp = ApiError::Forbidden("access denied".into()).into_response();
        assert_eq!(resp.status(), StatusCode::FORBIDDEN);
    }

    #[test]
    fn conflict_returns_409() {
        let resp = ApiError::Conflict("duplicate key".into()).into_response();
        assert_eq!(resp.status(), StatusCode::CONFLICT);
    }

    #[test]
    fn db_not_found_returns_404() {
        let resp = ApiError::Database(DbError::NotFound).into_response();
        assert_eq!(resp.status(), StatusCode::NOT_FOUND);
    }

    #[test]
    fn db_duplicate_returns_409() {
        let resp = ApiError::Database(DbError::DuplicateKey).into_response();
        assert_eq!(resp.status(), StatusCode::CONFLICT);
    }

    #[test]
    fn db_deadlock_returns_503() {
        let resp = ApiError::Database(DbError::Deadlock).into_response();
        assert_eq!(resp.status(), StatusCode::SERVICE_UNAVAILABLE);
    }

    #[test]
    fn internal_error_returns_500() {
        let resp = ApiError::Internal("oops".into()).into_response();
        assert_eq!(resp.status(), StatusCode::INTERNAL_SERVER_ERROR);
    }
}
