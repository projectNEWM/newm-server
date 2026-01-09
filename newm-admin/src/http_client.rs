//! Shared HTTP client configuration for NEWM Admin.
//!
//! Studio traffic is fronted by an AWS load balancer that rejects requests without a non-empty
//! `User-Agent` header (returns an HTML 403). To keep behavior consistent across environments,
//! we configure a default `User-Agent` for all outbound reqwest requests.

use reqwest::{Client, header};

const USER_AGENT: &str = concat!("newm-admin/", env!("CARGO_PKG_VERSION"));

pub fn new_client() -> Client {
    Client::builder()
        .default_headers({
            let mut headers = header::HeaderMap::new();
            headers.insert(
                header::USER_AGENT,
                header::HeaderValue::from_static(USER_AGENT),
            );
            headers
        })
        .build()
        .expect("Failed to build reqwest client")
}
