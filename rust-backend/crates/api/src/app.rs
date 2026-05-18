//! Application setup: router, CORS, tracing middleware, state management.
//!
//! Assembles the Axum router tree and shared application state.

use std::collections::HashMap;
use std::sync::atomic::{AtomicU32, Ordering};

use axum::{Json, Router};
use parking_lot::RwLock;
use tower_http::cors::{Any, CorsLayer};
use tower_http::trace::TraceLayer;

use domain::{PortfolioRecord, PositionRecord, TransactionRecord};

use crate::middleware::auth::JwtConfig;
use crate::openapi;
use crate::routes;

// ---------------------------------------------------------------------------
// Application state
// ---------------------------------------------------------------------------

/// Shared application state available to all handlers via `State<AppState>`.
#[derive(Debug, Clone)]
pub struct AppState {
    pub jwt: JwtConfig,
    pub store: std::sync::Arc<RwLock<InMemoryStore>>,
    seq: std::sync::Arc<AtomicU32>,
}

impl AsRef<JwtConfig> for AppState {
    fn as_ref(&self) -> &JwtConfig {
        &self.jwt
    }
}

/// In-memory data store (will be replaced by `db::DatabasePool` in Wave 4).
#[derive(Debug, Default)]
pub struct InMemoryStore {
    pub portfolios: HashMap<String, PortfolioRecord>,
    pub positions: Vec<PositionRecord>,
    pub transactions: Vec<TransactionRecord>,
}

impl AppState {
    pub fn new(jwt: JwtConfig) -> Self {
        Self {
            jwt,
            store: std::sync::Arc::new(RwLock::new(InMemoryStore::default())),
            seq: std::sync::Arc::new(AtomicU32::new(1)),
        }
    }

    /// Generate the next COBOL-compatible portfolio ID (PORT0001..PORT9999).
    ///
    /// Returns `None` when the 9 999-entry ID space is exhausted.
    pub fn next_portfolio_id(&self) -> Option<String> {
        loop {
            let current = self.seq.load(Ordering::Relaxed);
            if current >= 10_000 {
                return None;
            }
            if self
                .seq
                .compare_exchange(current, current + 1, Ordering::Relaxed, Ordering::Relaxed)
                .is_ok()
            {
                return Some(format!("PORT{:04}", current));
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Router builder
// ---------------------------------------------------------------------------

/// Build the full application router.
pub fn build_router(state: AppState) -> Router {
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    Router::new()
        .nest("/api/portfolios", routes::portfolios::router())
        .route("/api/openapi.json", axum::routing::get(openapi_handler))
        .route("/health", axum::routing::get(health))
        .with_state(state)
        .layer(TraceLayer::new_for_http())
        .layer(cors)
}

async fn health() -> &'static str {
    "ok"
}

async fn openapi_handler() -> Json<serde_json::Value> {
    Json(openapi::spec())
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use axum::body::Body;
    use axum::http::{Request, StatusCode};
    use tower::ServiceExt;

    fn test_state() -> AppState {
        AppState::new(JwtConfig::new("test-secret"))
    }

    #[tokio::test]
    async fn health_returns_ok() {
        let app = build_router(test_state());
        let req = Request::builder()
            .uri("/health")
            .body(Body::empty())
            .unwrap();
        let resp = app.oneshot(req).await.unwrap();
        assert_eq!(resp.status(), StatusCode::OK);
    }

    #[tokio::test]
    async fn portfolios_requires_auth() {
        let app = build_router(test_state());
        let req = Request::builder()
            .uri("/api/portfolios")
            .body(Body::empty())
            .unwrap();
        let resp = app.oneshot(req).await.unwrap();
        assert_eq!(resp.status(), StatusCode::UNAUTHORIZED);
    }
}
