//! JWT parsing module for NEWM Admin
//!
//! Parses JWT claims without signature verification since we trust the server.

use base64::Engine;
use base64::engine::general_purpose;
use serde::Deserialize;

/// JWT claims structure matching the backend's token format
#[derive(Debug, Deserialize)]
pub struct JwtClaims {
    /// Whether the user has admin privileges
    pub admin: Option<bool>,
    /// Token type (access/refresh)
    #[serde(rename = "type")]
    #[allow(dead_code)]
    pub token_type: Option<String>,
    /// User ID (subject)
    #[allow(dead_code)]
    pub sub: Option<String>,
    /// Expiration timestamp (seconds since epoch)
    pub exp: Option<i64>,
}

/// Check if a token is expired
#[allow(dead_code)]
pub fn is_expired(token: &str) -> bool {
    expires_in_secs(token).is_none_or(|secs| secs <= 0)
}

/// Check if a token expires within the given buffer (in seconds)
pub fn expires_soon(token: &str, buffer_secs: i64) -> bool {
    expires_in_secs(token).is_none_or(|secs| secs <= buffer_secs)
}

/// Get seconds until token expires (negative if already expired)
fn expires_in_secs(token: &str) -> Option<i64> {
    let claims = parse_claims(token).ok()?;
    let exp = claims.exp?;
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .ok()?
        .as_secs() as i64;
    Some(exp - now)
}

/// Parse claims from a JWT token without signature verification.
///
/// This extracts the payload section and deserializes the claims.
/// We don't verify the signature because we trust the NEWM server response.
pub fn parse_claims(token: &str) -> Result<JwtClaims, String> {
    // JWT format: header.payload.signature
    let parts: Vec<&str> = token.split('.').collect();
    if parts.len() != 3 {
        return Err("Invalid JWT format: expected 3 parts".to_string());
    }

    // Decode the payload (second part) using URL-safe base64
    let payload_bytes = general_purpose::URL_SAFE_NO_PAD
        .decode(parts[1])
        .map_err(|e| format!("Failed to decode JWT payload: {}", e))?;

    let claims: JwtClaims = serde_json::from_slice(&payload_bytes)
        .map_err(|e| format!("Failed to parse JWT claims: {}", e))?;

    Ok(claims)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_admin_token() {
        // Create a mock JWT with admin=true
        // Header: {"alg":"HS256","typ":"JWT"}
        // Payload: {"admin":true,"sub":"123"}
        let header = general_purpose::URL_SAFE_NO_PAD.encode(r#"{"alg":"HS256","typ":"JWT"}"#);
        let payload = general_purpose::URL_SAFE_NO_PAD.encode(r#"{"admin":true,"sub":"123"}"#);
        let token = format!("{}.{}.signature", header, payload);

        let claims = parse_claims(&token).unwrap();
        assert_eq!(claims.admin, Some(true));
    }

    #[test]
    fn test_parse_non_admin_token() {
        let header = general_purpose::URL_SAFE_NO_PAD.encode(r#"{"alg":"HS256","typ":"JWT"}"#);
        let payload = general_purpose::URL_SAFE_NO_PAD.encode(r#"{"admin":false,"sub":"456"}"#);
        let token = format!("{}.{}.signature", header, payload);

        let claims = parse_claims(&token).unwrap();
        assert_eq!(claims.admin, Some(false));
    }

    #[test]
    fn test_invalid_jwt_format() {
        let result = parse_claims("invalid.token");
        assert!(result.is_err());
    }
}
