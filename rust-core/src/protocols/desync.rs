use tokio::io::AsyncWriteExt;
use tokio::net::TcpStream;
use tokio::time::{sleep, Duration};

pub struct TlsDesyncFragmenter;

impl TlsDesyncFragmenter {
    /// Fragments a TLS ClientHello packet across TCP segments with micro-delays
    /// to disrupt Deep Packet Inspection (DPI) state machines and SNI pattern matching.
    pub async fn write_fragmented_client_hello(
        stream: &mut TcpStream,
        client_hello: &[u8],
        split_size: usize,
        delay_ms: u64,
    ) -> Result<(), std::io::Error> {
        let total_len = client_hello.len();

        if total_len <= split_size || split_size == 0 {
            stream.write_all(client_hello).await?;
            stream.flush().await?;
            return Ok(());
        }

        // Send first chunk (contains TLS record header + initial SNI boundary)
        let first_chunk = &client_hello[..split_size];
        stream.write_all(first_chunk).await?;
        stream.flush().await?;

        // Optional evasion jitter delay to disrupt DPI buffer reassembly
        if delay_ms > 0 {
            sleep(Duration::from_millis(delay_ms)).await;
        }

        // Send remaining packet payload
        let remaining = &client_hello[split_size..];
        stream.write_all(remaining).await?;
        stream.flush().await?;

        Ok(())
    }
}
