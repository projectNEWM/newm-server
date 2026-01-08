//! CSV Import Module for NEWM Admin
//!
//! Handles parsing CSV files for batch earnings creation and writing result files.
//! Supports CSV files with or without headers.

use std::path::{Path, PathBuf};

/// A row from the input CSV
#[derive(Debug, Clone)]
pub struct CsvRow {
    pub song_id_or_isrc: String,
    pub amount_usd: String,
}

/// Result for a processed row
#[derive(Debug, Clone)]
pub struct CsvResult {
    pub row: CsvRow,
    pub result: String, // "Success" or error message
}

/// Summary of CSV import operation
#[derive(Debug, Clone)]
#[allow(dead_code)]
pub struct CsvImportSummary {
    pub total: usize,
    pub succeeded: usize,
    pub failed: usize,
    pub output_path: PathBuf,
}

/// Errors that can occur during CSV operations
#[derive(Debug)]
pub enum CsvError {
    IoError(String),
    ParseError(String),
    InvalidFormat(String),
}

impl std::fmt::Display for CsvError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            CsvError::IoError(msg) => write!(f, "IO error: {}", msg),
            CsvError::ParseError(msg) => write!(f, "Parse error: {}", msg),
            CsvError::InvalidFormat(msg) => write!(f, "Invalid format: {}", msg),
        }
    }
}

impl std::error::Error for CsvError {}

/// Check if a row looks like a header row (contains common header names)
fn is_header_row(col1: &str, col2: &str) -> bool {
    let col1_lower = col1.to_lowercase();
    let col2_lower = col2.to_lowercase();

    // Common header patterns
    let col1_is_header = col1_lower.contains("song")
        || col1_lower.contains("isrc")
        || col1_lower.contains("id")
        || col1_lower == "songid_or_isrc"
        || col1_lower == "song_id";

    let col2_is_header = col2_lower.contains("amount")
        || col2_lower.contains("usd")
        || col2_lower.contains("price")
        || col2_lower == "amount_usd";

    col1_is_header && col2_is_header
}

/// Parse a CSV file into rows
///
/// Supports CSV files with or without headers. If a header row is detected,
/// it will be skipped automatically.
///
/// Expected format (2 columns):
/// - Column 1: Song ID or ISRC
/// - Column 2: Amount in USD (e.g., "10.50")
pub fn parse_csv(path: &Path) -> Result<Vec<CsvRow>, CsvError> {
    let mut reader = csv::ReaderBuilder::new()
        .has_headers(false) // We handle headers manually
        .flexible(true) // Allow varying number of fields
        .trim(csv::Trim::All)
        .from_path(path)
        .map_err(|e| CsvError::IoError(e.to_string()))?;

    let mut rows = Vec::new();
    let mut is_first_row = true;

    for (line_num, result) in reader.records().enumerate() {
        let record =
            result.map_err(|e| CsvError::ParseError(format!("Line {}: {}", line_num + 1, e)))?;

        // Ensure we have at least 2 columns
        if record.len() < 2 {
            return Err(CsvError::InvalidFormat(format!(
                "Line {}: Expected at least 2 columns, found {}",
                line_num + 1,
                record.len()
            )));
        }

        let col1 = record.get(0).unwrap_or("").trim();
        let col2 = record.get(1).unwrap_or("").trim();

        // Skip empty rows
        if col1.is_empty() && col2.is_empty() {
            continue;
        }

        // Check if first row is a header
        if is_first_row {
            is_first_row = false;
            if is_header_row(col1, col2) {
                tracing::info!("Detected header row, skipping");
                continue;
            }
        }

        rows.push(CsvRow {
            song_id_or_isrc: col1.to_string(),
            amount_usd: col2.to_string(),
        });
    }

    if rows.is_empty() {
        return Err(CsvError::InvalidFormat(
            "CSV file contains no data rows".to_string(),
        ));
    }

    tracing::info!("Parsed {} rows from CSV", rows.len());
    Ok(rows)
}

/// Write results to a new CSV file
///
/// Creates a new file with "_results" appended to the original filename.
/// Output format: songId_or_isrc,amount_usd,result
pub fn write_results(input_path: &Path, results: &[CsvResult]) -> Result<PathBuf, CsvError> {
    // Generate output filename
    let stem = input_path
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("output");
    let extension = input_path
        .extension()
        .and_then(|s| s.to_str())
        .unwrap_or("csv");

    let output_filename = format!("{}_results.{}", stem, extension);
    let output_path = input_path.with_file_name(output_filename);

    let mut writer =
        csv::Writer::from_path(&output_path).map_err(|e| CsvError::IoError(e.to_string()))?;

    // Write header
    writer
        .write_record(["songId_or_isrc", "amount_usd", "result"])
        .map_err(|e| CsvError::IoError(e.to_string()))?;

    // Write data rows
    for result in results {
        writer
            .write_record([
                &result.row.song_id_or_isrc,
                &result.row.amount_usd,
                &result.result,
            ])
            .map_err(|e| CsvError::IoError(e.to_string()))?;
    }

    writer
        .flush()
        .map_err(|e| CsvError::IoError(e.to_string()))?;

    tracing::info!("Wrote results to {:?}", output_path);
    Ok(output_path)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::NamedTempFile;

    fn create_temp_csv(content: &str) -> NamedTempFile {
        let mut file = NamedTempFile::new().unwrap();
        file.write_all(content.as_bytes()).unwrap();
        file.flush().unwrap();
        file
    }

    #[test]
    fn test_parse_csv_with_header() {
        let csv_content = "songId_or_isrc,amount_usd\n\
                           550e8400-e29b-41d4-a716-446655440000,10.50\n\
                           USRC12345678,25.00";
        let file = create_temp_csv(csv_content);

        let rows = parse_csv(file.path()).unwrap();
        assert_eq!(rows.len(), 2);
        assert_eq!(
            rows[0].song_id_or_isrc,
            "550e8400-e29b-41d4-a716-446655440000"
        );
        assert_eq!(rows[0].amount_usd, "10.50");
        assert_eq!(rows[1].song_id_or_isrc, "USRC12345678");
        assert_eq!(rows[1].amount_usd, "25.00");
    }

    #[test]
    fn test_parse_csv_without_header() {
        let csv_content = "550e8400-e29b-41d4-a716-446655440000,10.50\n\
                           USRC12345678,25.00";
        let file = create_temp_csv(csv_content);

        let rows = parse_csv(file.path()).unwrap();
        assert_eq!(rows.len(), 2);
        assert_eq!(
            rows[0].song_id_or_isrc,
            "550e8400-e29b-41d4-a716-446655440000"
        );
        assert_eq!(rows[0].amount_usd, "10.50");
    }

    #[test]
    fn test_is_header_row() {
        // Should detect as headers
        assert!(is_header_row("songId_or_isrc", "amount_usd"));
        assert!(is_header_row("song_id", "amount"));
        assert!(is_header_row("ISRC", "USD Amount"));
        assert!(is_header_row("Song ID", "Price USD"));

        // Should NOT detect as headers (actual data)
        assert!(!is_header_row(
            "550e8400-e29b-41d4-a716-446655440000",
            "10.50"
        ));
        assert!(!is_header_row("USRC12345678", "25.00"));
    }
}
