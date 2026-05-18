//! JWT authentication middleware.
//!
//! Translated from COBOL programs:
//! - `SECMGR.cbl` — P100-VALIDATE-USER, P200-CHECK-AUTH
//! - `INQONLN.cbl` — P050-SECURITY-CHECK
//!
//! The COBOL security model validated CICS user IDs and checked a DB2
//! AUTHFILE for resource/access-type authorization. In the Rust port this
//! becomes JWT bearer-token validation: the token carries the user identity
//! and a role claim that gates access to resources.

use axum::extract::FromRequestParts;
use axum::http::request::Parts;
use axum::http::HeaderMap;
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};
use serde::{Deserialize, Serialize};

use crate::error::ApiError;

// ---------------------------------------------------------------------------
// Claims — JWT payload (mirrors SECMGR SECURITY-REQUEST-AREA)
// ---------------------------------------------------------------------------

/// JWT claims.
///
/// `sub` → SEC-USER-ID, `role` → SEC-ACCESS-TYPE.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Claims {
    /// Subject — the user identifier (maps to SEC-USER-ID).
    pub sub: String,
    /// Role — access level (maps to SEC-ACCESS-TYPE).
    #[serde(default)]
    pub role: Role,
    /// Expiration (UNIX timestamp).
    pub exp: usize,
}

/// User role (derived from SECMGR access types).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "lowercase")]
pub enum Role {
    Admin,
    #[default]
    User,
    Readonly,
}

// ---------------------------------------------------------------------------
// JWT configuration
// ---------------------------------------------------------------------------

/// Configuration for JWT validation.
#[derive(Debug, Clone)]
pub struct JwtConfig {
    pub secret: String,
    pub issuer: Option<String>,
}

impl JwtConfig {
    pub fn new(secret: impl Into<String>) -> Self {
        Self {
            secret: secret.into(),
            issuer: None,
        }
    }

    pub fn with_issuer(mut self, issuer: impl Into<String>) -> Self {
        self.issuer = Some(issuer.into());
        self
    }

    /// Build a `jsonwebtoken::Validation` from this config.
    fn validation(&self) -> Validation {
        let mut v = Validation::new(Algorithm::HS256);
        v.validate_exp = true;
        if let Some(ref iss) = self.issuer {
            v.set_issuer(&[iss]);
        }
        v
    }

    /// Build the decoding key.
    fn decoding_key(&self) -> DecodingKey {
        DecodingKey::from_secret(self.secret.as_bytes())
    }
}

// ---------------------------------------------------------------------------
// Token extraction & validation (P050-SECURITY-CHECK + P100-VALIDATE-USER)
// ---------------------------------------------------------------------------

/// Extract and validate a JWT from the `Authorization: Bearer <token>` header.
///
/// Returns decoded [`Claims`] or an [`ApiError::Unauthorized`].
pub fn validate_token(headers: &HeaderMap, config: &JwtConfig) -> Result<Claims, ApiError> {
    let header_value = headers
        .get(axum::http::header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .ok_or_else(|| ApiError::Unauthorized("missing Authorization header".into()))?;

    let token = header_value
        .strip_prefix("Bearer ")
        .ok_or_else(|| ApiError::Unauthorized("invalid Authorization scheme".into()))?;

    let token_data = decode::<Claims>(token, &config.decoding_key(), &config.validation())
        .map_err(|e| ApiError::Unauthorized(format!("invalid token: {e}")))?;

    Ok(token_data.claims)
}

/// Verify that the caller has the required role (P200-CHECK-AUTH).
pub fn require_role(claims: &Claims, required: Role) -> Result<(), ApiError> {
    let allowed = match required {
        Role::Readonly => true,
        Role::User => matches!(claims.role, Role::Admin | Role::User),
        Role::Admin => claims.role == Role::Admin,
    };
    if allowed {
        Ok(())
    } else {
        Err(ApiError::Forbidden(format!(
            "role '{}' required",
            serde_json::to_string(&required)
                .unwrap_or_default()
                .trim_matches('"')
        )))
    }
}

// ---------------------------------------------------------------------------
// Axum extractor — lets handlers declare `AuthUser` as a parameter
// ---------------------------------------------------------------------------

/// Authenticated user extracted from the JWT.
///
/// Usage: `async fn handler(user: AuthUser) { ... }`
#[derive(Debug, Clone)]
pub struct AuthUser {
    pub claims: Claims,
}

impl<S> FromRequestParts<S> for AuthUser
where
    S: AsRef<JwtConfig> + Send + Sync,
{
    type Rejection = ApiError;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let config = state.as_ref();
        let claims = validate_token(&parts.headers, config)?;
        Ok(AuthUser { claims })
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use axum::http::HeaderValue;
    use jsonwebtoken::{encode, EncodingKey, Header};

    const TEST_SECRET: &str = "test-secret-key-for-unit-tests-only";

    fn test_config() -> JwtConfig {
        JwtConfig::new(TEST_SECRET)
    }

    fn make_token(claims: &Claims) -> String {
        encode(
            &Header::default(),
            claims,
            &EncodingKey::from_secret(TEST_SECRET.as_bytes()),
        )
        .unwrap()
    }

    fn valid_claims() -> Claims {
        Claims {
            sub: "testuser".into(),
            role: Role::User,
            exp: (chrono::Utc::now().timestamp() + 3600) as usize,
        }
    }

    #[test]
    fn validate_token_success() {
        let claims = valid_claims();
        let token = make_token(&claims);
        let mut headers = HeaderMap::new();
        headers.insert(
            axum::http::header::AUTHORIZATION,
            HeaderValue::from_str(&format!("Bearer {token}")).unwrap(),
        );
        let result = validate_token(&headers, &test_config());
        assert!(result.is_ok());
        let decoded = result.unwrap();
        assert_eq!(decoded.sub, "testuser");
    }

    #[test]
    fn validate_token_missing_header() {
        let headers = HeaderMap::new();
        let result = validate_token(&headers, &test_config());
        assert!(matches!(result, Err(ApiError::Unauthorized(_))));
    }

    #[test]
    fn validate_token_bad_scheme() {
        let mut headers = HeaderMap::new();
        headers.insert(
            axum::http::header::AUTHORIZATION,
            HeaderValue::from_static("Basic abc"),
        );
        let result = validate_token(&headers, &test_config());
        assert!(matches!(result, Err(ApiError::Unauthorized(_))));
    }

    #[test]
    fn validate_token_expired() {
        let claims = Claims {
            sub: "expired".into(),
            role: Role::User,
            exp: 0,
        };
        let token = make_token(&claims);
        let mut headers = HeaderMap::new();
        headers.insert(
            axum::http::header::AUTHORIZATION,
            HeaderValue::from_str(&format!("Bearer {token}")).unwrap(),
        );
        let result = validate_token(&headers, &test_config());
        assert!(matches!(result, Err(ApiError::Unauthorized(_))));
    }

    #[test]
    fn validate_token_wrong_secret() {
        let claims = valid_claims();
        let token = encode(
            &Header::default(),
            &claims,
            &EncodingKey::from_secret(b"wrong-secret"),
        )
        .unwrap();
        let mut headers = HeaderMap::new();
        headers.insert(
            axum::http::header::AUTHORIZATION,
            HeaderValue::from_str(&format!("Bearer {token}")).unwrap(),
        );
        let result = validate_token(&headers, &test_config());
        assert!(matches!(result, Err(ApiError::Unauthorized(_))));
    }

    #[test]
    fn require_role_admin_allows_admin() {
        let claims = Claims {
            sub: "admin".into(),
            role: Role::Admin,
            exp: 9999999999,
        };
        assert!(require_role(&claims, Role::Admin).is_ok());
    }

    #[test]
    fn require_role_admin_denies_user() {
        let claims = Claims {
            sub: "user1".into(),
            role: Role::User,
            exp: 9999999999,
        };
        assert!(matches!(
            require_role(&claims, Role::Admin),
            Err(ApiError::Forbidden(_))
        ));
    }

    #[test]
    fn require_role_user_allows_admin() {
        let claims = Claims {
            sub: "admin".into(),
            role: Role::Admin,
            exp: 9999999999,
        };
        assert!(require_role(&claims, Role::User).is_ok());
    }

    #[test]
    fn require_role_readonly_allows_all() {
        for role in [Role::Admin, Role::User, Role::Readonly] {
            let claims = Claims {
                sub: "x".into(),
                role,
                exp: 9999999999,
            };
            assert!(require_role(&claims, Role::Readonly).is_ok());
        }
    }
}
