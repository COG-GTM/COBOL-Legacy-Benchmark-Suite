//! Server entry point.
//!
//! Reads configuration from environment variables, initialises tracing,
//! builds application state, and starts the Axum HTTP server.

use std::net::SocketAddr;
use std::sync::Arc;

use parking_lot::RwLock;
use tracing_subscriber::EnvFilter;

use api::app::{build_router, AppState, InMemoryStore};
use api::middleware::auth::JwtConfig;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()))
        .json()
        .init();

    let jwt_secret =
        std::env::var("JWT_SECRET").unwrap_or_else(|_| "change-me-in-production".into());
    let jwt_issuer = std::env::var("JWT_ISSUER").ok();
    let port: u16 = std::env::var("PORT")
        .ok()
        .and_then(|p| p.parse().ok())
        .unwrap_or(3000);

    let mut jwt = JwtConfig::new(jwt_secret);
    if let Some(iss) = jwt_issuer {
        jwt = jwt.with_issuer(iss);
    }

    let state = AppState {
        jwt,
        store: Arc::new(RwLock::new(InMemoryStore::default())),
    };

    let app = build_router(state);
    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    tracing::info!(%addr, "starting server");

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
