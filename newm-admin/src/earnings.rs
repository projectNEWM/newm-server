//! Earnings API client for NEWM Admin
//!
//! Handles earnings-related API calls with automatic session management.

use async_compat::Compat;
use reqwest::Client;
use serde::{Deserialize, Serialize};

use crate::session::{Session, SessionError};

/// Request to add royalties to a song
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AddSongRoyaltyRequest {
    /// Amount in USD with 6 decimal places (e.g., 10.50 USD = 10500000)
    pub usd_amount: i64,
}

/// Earning record from the API
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Earning {
    pub id: Option<String>,
    pub song_id: Option<String>,
    pub stake_address: String,
    pub amount: i64,
    pub memo: Option<String>,
    #[serde(default)]
    pub claimed: bool,
    pub claimed_at: Option<String>,
    pub created_at: String,
}

/// Error from earnings API operations
#[derive(Debug)]
pub enum EarningsError {
    /// Session expired, user must re-login
    SessionExpired(String),
    /// API returned an error
    Api { status: u16, message: String },
    /// Network or other error
    Network(String),
}

impl std::fmt::Display for EarningsError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            EarningsError::SessionExpired(msg) => write!(f, "Session expired: {}", msg),
            EarningsError::Api { status, message } => {
                write!(f, "API error {}: {}", status, message)
            }
            EarningsError::Network(msg) => write!(f, "Network error: {}", msg),
        }
    }
}

impl std::error::Error for EarningsError {}

impl From<SessionError> for EarningsError {
    fn from(err: SessionError) -> Self {
        match err {
            SessionError::Expired(msg) => EarningsError::SessionExpired(msg),
            SessionError::Network(msg) => EarningsError::Network(msg),
        }
    }
}

/// Client for earnings API operations
#[derive(Clone)]
pub struct EarningsClient {
    client: Client,
}

impl Default for EarningsClient {
    fn default() -> Self {
        Self::new()
    }
}

impl EarningsClient {
    /// Create a new earnings client
    pub fn new() -> Self {
        Self {
            client: Client::new(),
        }
    }

    /// Add earnings for a song.
    ///
    /// # Arguments
    /// * `session` - The authenticated session (will auto-refresh token if needed)
    /// * `song_id_or_isrc` - UUID or ISRC identifier for the song
    /// * `usd_amount` - Amount in USD with 6 decimal places
    ///
    /// # Returns
    /// * `Ok(())` on success
    /// * `Err(EarningsError::SessionExpired)` if token refresh fails
    /// * `Err(EarningsError::Api)` for API errors
    pub async fn add_earnings(
        &self,
        session: &Session,
        song_id_or_isrc: &str,
        usd_amount: i64,
    ) -> Result<(), EarningsError> {
        let access_token = session.get_valid_token().await?;

        let url = format!(
            "{}/v1/earnings/admin/{}",
            session.environment().base_url(),
            song_id_or_isrc
        );

        let request_body = AddSongRoyaltyRequest { usd_amount };

        tracing::info!(
            "Adding earnings for {} with amount {}",
            song_id_or_isrc,
            usd_amount
        );

        let response = Compat::new(async {
            self.client
                .post(&url)
                .header("Authorization", format!("Bearer {}", access_token))
                .json(&request_body)
                .send()
                .await
        })
        .await
        .map_err(|e| EarningsError::Network(e.to_string()))?;

        let status = response.status();

        if status.is_success() {
            tracing::info!("Earnings added successfully");
            Ok(())
        } else if status.as_u16() == 401 {
            // Token was invalid despite refresh - session expired
            Err(EarningsError::SessionExpired(
                "Unauthorized - please login again".to_string(),
            ))
        } else {
            let error_text = Compat::new(async { response.text().await })
                .await
                .unwrap_or_else(|_| "Unknown error".to_string());

            tracing::warn!("Add earnings failed: {} - {}", status, error_text);
            Err(EarningsError::Api {
                status: status.as_u16(),
                message: error_text,
            })
        }
    }

    /// Get all earnings
    ///
    /// # Arguments
    /// * `session` - The authenticated session
    ///
    /// # Returns
    /// * `Ok(Vec<Earning>)` on success
    /// * `Err(EarningsError)` on failure
    pub async fn get_earnings(&self, session: &Session) -> Result<Vec<Earning>, EarningsError> {
        let access_token = session.get_valid_token().await?;

        let url = format!("{}/v1/earnings/admin", session.environment().base_url());

        tracing::info!("Fetching all earnings");

        let response = Compat::new(async {
            self.client
                .get(&url)
                .header("Authorization", format!("Bearer {}", access_token))
                .send()
                .await
        })
        .await
        .map_err(|e| EarningsError::Network(e.to_string()))?;

        let status = response.status();

        if status.is_success() {
            let earnings = Compat::new(async { response.json::<Vec<Earning>>().await })
                .await
                .map_err(|e| EarningsError::Api {
                    status: 200,
                    message: format!("Failed to parse response: {}", e),
                })?;

            tracing::info!("Fetched {} earnings", earnings.len());
            Ok(earnings)
        } else if status.as_u16() == 401 {
            Err(EarningsError::SessionExpired(
                "Unauthorized - please login again".to_string(),
            ))
        } else {
            let error_text = Compat::new(async { response.text().await })
                .await
                .unwrap_or_else(|_| "Unknown error".to_string());

            tracing::warn!("Get earnings failed: {} - {}", status, error_text);
            Err(EarningsError::Api {
                status: status.as_u16(),
                message: error_text,
            })
        }
    }

    /// Delete earnings by IDs
    ///
    /// # Arguments
    /// * `session` - The authenticated session
    /// * `earning_ids` - List of earning UUIDs to delete
    ///
    /// # Returns
    /// * `Ok(())` on success
    /// * `Err(EarningsError)` on failure
    pub async fn delete_earnings(
        &self,
        session: &Session,
        earning_ids: Vec<String>,
    ) -> Result<(), EarningsError> {
        let access_token = session.get_valid_token().await?;

        let url = format!("{}/v1/earnings/admin", session.environment().base_url());

        tracing::info!("Deleting {} earnings", earning_ids.len());

        let response = Compat::new(async {
            self.client
                .delete(&url)
                .header("Authorization", format!("Bearer {}", access_token))
                .json(&earning_ids)
                .send()
                .await
        })
        .await
        .map_err(|e| EarningsError::Network(e.to_string()))?;

        let status = response.status();

        if status.is_success() {
            tracing::info!("Successfully deleted {} earnings", earning_ids.len());
            Ok(())
        } else if status.as_u16() == 401 {
            Err(EarningsError::SessionExpired(
                "Unauthorized - please login again".to_string(),
            ))
        } else {
            let error_text = Compat::new(async { response.text().await })
                .await
                .unwrap_or_else(|_| "Unknown error".to_string());

            tracing::warn!("Delete earnings failed: {} - {}", status, error_text);
            Err(EarningsError::Api {
                status: status.as_u16(),
                message: error_text,
            })
        }
    }
}

/// Convert a USD decimal string to 6-decimal integer
///
/// Validates that input has at most 6 decimal places.
///
/// Examples:
/// * "10.50" -> 10500000
/// * "100" -> 100000000
/// * "0.000001" -> 1
pub fn usd_to_amount(usd_str: &str) -> Result<i64, String> {
    let trimmed = usd_str.trim();

    // Check decimal places
    if let Some(dot_pos) = trimmed.find('.') {
        let decimals = trimmed.len() - dot_pos - 1;
        if decimals > 6 {
            return Err(format!(
                "Maximum 6 decimal places allowed (got {})",
                decimals
            ));
        }
    }

    let value: f64 = trimmed
        .parse()
        .map_err(|_| "Invalid number format".to_string())?;

    if value < 0.0 {
        return Err("Amount cannot be negative".to_string());
    }

    // Multiply by 1,000,000 for 6 decimal places
    let amount = (value * 1_000_000.0).round() as i64;
    Ok(amount)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_usd_to_amount() {
        assert_eq!(usd_to_amount("10.50").unwrap(), 10_500_000);
        assert_eq!(usd_to_amount("100").unwrap(), 100_000_000);
        assert_eq!(usd_to_amount("0.000001").unwrap(), 1);
        assert_eq!(usd_to_amount("1.234567").unwrap(), 1_234_567);
    }

    #[test]
    fn test_usd_to_amount_too_many_decimals() {
        assert!(usd_to_amount("10.1234567").is_err());
        assert!(usd_to_amount("1.0000001").is_err());
    }

    #[test]
    fn test_usd_to_amount_negative() {
        assert!(usd_to_amount("-10").is_err());
    }

    #[test]
    fn test_usd_to_amount_invalid() {
        assert!(usd_to_amount("abc").is_err());
    }
}
