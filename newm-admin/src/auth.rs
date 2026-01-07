//! Authentication module for NEWM API
//!
//! Handles login requests to Garage and Studio environments with
//! JWT token management.
//!
//! Uses async reqwest with async-compat for Tokio compatibility
//! within GPUI's async executor.

use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::fmt;

/// Environment selection for API requests
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Environment {
    #[default]
    Garage,
    Studio,
}

impl Environment {
    /// Get the base URL for this environment
    pub fn base_url(&self) -> &'static str {
        match self {
            Environment::Garage => "https://garage.newm.io",
            Environment::Studio => "https://studio.newm.io",
        }
    }

    /// Get the login endpoint URL
    pub fn login_url(&self) -> String {
        format!("{}/v1/auth/login", self.base_url())
    }

    /// Get the token refresh endpoint URL
    #[allow(dead_code)]
    pub fn refresh_url(&self) -> String {
        format!("{}/v1/auth/refresh", self.base_url())
    }

    /// Get environment from index (for RadioGroup)
    pub fn from_index(index: usize) -> Self {
        match index {
            0 => Environment::Garage,
            1 => Environment::Studio,
            _ => Environment::Garage,
        }
    }

    /// Get index for RadioGroup
    #[allow(dead_code)]
    pub fn to_index(self) -> usize {
        match self {
            Environment::Garage => 0,
            Environment::Studio => 1,
        }
    }

    /// Display name for UI
    #[allow(dead_code)]
    pub fn display_name(&self) -> &'static str {
        match self {
            Environment::Garage => "Garage",
            Environment::Studio => "Studio",
        }
    }
}

/// Login request payload
#[derive(Debug, Serialize)]
pub struct LoginRequest {
    pub email: String,
    pub password: String,
}

/// Successful login response with JWT tokens
#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct LoginResponse {
    pub access_token: String,
    pub refresh_token: String,
}

/// API error response
#[derive(Debug, Deserialize)]
pub struct ApiErrorResponse {
    pub code: u16,
    pub description: String,
    pub cause: String,
}

/// Authentication errors
#[derive(Debug)]
pub enum AuthError {
    /// Network or connection error
    Network(String),
    /// HTTP error with status code and message
    Http { status: u16, message: String },
    /// Failed to parse response
    Parse(String),
    /// User is not an admin
    NotAdmin,
}

impl fmt::Display for AuthError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            AuthError::Network(msg) => write!(f, "Network error: {}", msg),
            AuthError::Http { status, message } => write!(f, "Error {}: {}", status, message),
            AuthError::Parse(msg) => write!(f, "Parse error: {}", msg),
            AuthError::NotAdmin => write!(f, "Access denied: Admin privileges required"),
        }
    }
}

impl std::error::Error for AuthError {}

/// HTTP client for authentication API calls.
/// Methods are async and should be called within an `async_compat::Compat` wrapper
/// when used from GPUI's async context.
#[derive(Clone)]
pub struct AuthClient {
    client: Client,
}

impl Default for AuthClient {
    fn default() -> Self {
        Self::new()
    }
}

impl AuthClient {
    /// Create a new authentication client
    pub fn new() -> Self {
        Self {
            client: Client::new(),
        }
    }

    /// Attempt to login with email and password.
    ///
    /// This is an async method that uses reqwest. When called from GPUI,
    /// wrap the call in `async_compat::Compat::new()` to enable Tokio compatibility.
    pub async fn login(
        &self,
        email: &str,
        password: &str,
        environment: Environment,
    ) -> Result<LoginResponse, AuthError> {
        let url = environment.login_url();

        tracing::info!("Attempting login to {} for email: {}", url, email);

        let request = LoginRequest {
            email: email.to_string(),
            password: password.to_string(),
        };

        let response = self
            .client
            .post(&url)
            .json(&request)
            .send()
            .await
            .map_err(|e| AuthError::Network(e.to_string()))?;

        let status = response.status();

        if status.is_success() {
            let login_response: LoginResponse = response
                .json()
                .await
                .map_err(|e| AuthError::Parse(e.to_string()))?;

            // Validate that the user is an admin
            let claims =
                crate::jwt::parse_claims(&login_response.access_token).map_err(AuthError::Parse)?;

            if claims.admin != Some(true) {
                tracing::warn!("Login rejected for {}: user is not an admin", email);
                return Err(AuthError::NotAdmin);
            }

            tracing::info!("Admin login successful for {}", email);
            Ok(login_response)
        } else {
            // Try to parse error response
            let error_text = response
                .text()
                .await
                .unwrap_or_else(|_| "Unknown error".to_string());

            // Try to parse as API error
            if let Ok(api_error) = serde_json::from_str::<ApiErrorResponse>(&error_text) {
                tracing::warn!(
                    "Login failed: [{}] {} - {}",
                    api_error.code,
                    api_error.description,
                    api_error.cause
                );
                Err(AuthError::Http {
                    status: status.as_u16(),
                    message: api_error.cause,
                })
            } else {
                tracing::warn!("Login failed with status {}: {}", status, error_text);
                Err(AuthError::Http {
                    status: status.as_u16(),
                    message: error_text,
                })
            }
        }
    }

    /// Refresh access token using refresh token.
    ///
    /// This is an async method that uses reqwest. When called from GPUI,
    /// wrap the call in `async_compat::Compat::new()` to enable Tokio compatibility.
    #[allow(dead_code)]
    pub async fn refresh(
        &self,
        refresh_token: &str,
        environment: Environment,
    ) -> Result<LoginResponse, AuthError> {
        let url = environment.refresh_url();

        tracing::info!("Refreshing token for {}", environment.display_name());

        let response = self
            .client
            .get(&url)
            .header("Authorization", format!("Bearer {}", refresh_token))
            .send()
            .await
            .map_err(|e| AuthError::Network(e.to_string()))?;

        let status = response.status();

        if status.is_success() {
            let login_response: LoginResponse = response
                .json()
                .await
                .map_err(|e| AuthError::Parse(e.to_string()))?;

            tracing::info!("Token refresh successful");
            Ok(login_response)
        } else {
            let error_text = response
                .text()
                .await
                .unwrap_or_else(|_| "Unknown error".to_string());

            tracing::warn!("Token refresh failed: {}", error_text);
            Err(AuthError::Http {
                status: status.as_u16(),
                message: error_text,
            })
        }
    }
}
