//! Session management for NEWM Admin
//!
//! Handles token storage, automatic refresh, and session expiry.
//! This module provides a reusable authentication layer for all admin API calls.

use async_compat::Compat;
use gpui::*;
use std::sync::{Arc, Mutex};

use crate::auth::{Environment, LoginResponse};
use crate::http_client;
use crate::jwt;

/// Session state containing authentication tokens
#[derive(Clone)]
pub struct Session {
    inner: Arc<Mutex<SessionInner>>,
    environment: Environment,
}

struct SessionInner {
    access_token: String,
    refresh_token: String,
}

/// Error returned when session operations fail
#[derive(Debug, Clone)]
pub enum SessionError {
    /// Token refresh failed - user must re-login
    Expired(String),
    /// Network or other error
    Network(String),
}

impl std::fmt::Display for SessionError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            SessionError::Expired(msg) => write!(f, "Session expired: {}", msg),
            SessionError::Network(msg) => write!(f, "Network error: {}", msg),
        }
    }
}

impl std::error::Error for SessionError {}

impl Session {
    /// Create a new session from login response
    pub fn new(login_response: LoginResponse, environment: Environment) -> Self {
        Self {
            inner: Arc::new(Mutex::new(SessionInner {
                access_token: login_response.access_token,
                refresh_token: login_response.refresh_token,
            })),
            environment,
        }
    }

    /// Get the environment for this session
    pub fn environment(&self) -> Environment {
        self.environment
    }

    /// Get a valid access token, refreshing if necessary.
    ///
    /// This method checks if the current token expires within 60 seconds
    /// and automatically refreshes it if needed.
    ///
    /// Returns `SessionError::Expired` if the token cannot be refreshed.
    pub async fn get_valid_token(&self) -> Result<String, SessionError> {
        const REFRESH_BUFFER_SECS: i64 = 60;

        let (access_token, refresh_token) = {
            let inner = self.inner.lock().unwrap();
            (inner.access_token.clone(), inner.refresh_token.clone())
        };

        // Check if token expires soon
        if !jwt::expires_soon(&access_token, REFRESH_BUFFER_SECS) {
            return Ok(access_token);
        }

        tracing::info!("Access token expires soon, refreshing...");

        // Need to refresh - use the refresh token
        let client = http_client::new_client();
        let url = self.environment.refresh_url();

        let response = Compat::new(async {
            client
                .get(&url)
                .header("Authorization", format!("Bearer {}", refresh_token))
                .send()
                .await
        })
        .await
        .map_err(|e| SessionError::Network(e.to_string()))?;

        if response.status().is_success() {
            let login_response: LoginResponse = Compat::new(async { response.json().await })
                .await
                .map_err(|e| SessionError::Network(e.to_string()))?;

            // Update stored tokens
            {
                let mut inner = self.inner.lock().unwrap();
                inner.access_token = login_response.access_token.clone();
                inner.refresh_token = login_response.refresh_token;
            }

            tracing::info!("Token refresh successful");
            Ok(login_response.access_token)
        } else {
            let status = response.status();
            let error_text = Compat::new(async { response.text().await })
                .await
                .unwrap_or_else(|_| "Unknown error".to_string());

            tracing::warn!("Token refresh failed: {} - {}", status, error_text);
            Err(SessionError::Expired(format!(
                "Refresh failed: {}",
                error_text
            )))
        }
    }

    /// Get current access token without refresh check (for internal use)
    #[allow(dead_code)]
    pub fn current_token(&self) -> String {
        self.inner.lock().unwrap().access_token.clone()
    }
}

/// Event emitted when the session expires and user must re-login
pub struct SessionExpiredEvent {
    pub message: String,
}
